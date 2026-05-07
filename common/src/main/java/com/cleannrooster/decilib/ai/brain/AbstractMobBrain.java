package com.cleannrooster.decilib.ai.brain;

import com.cleannrooster.decilib.ai.DebugConfig;
import com.cleannrooster.decilib.ai.cooldown.AbilityCooldownRegistry;
import com.cleannrooster.decilib.ai.cooldown.DefaultCooldownRegistry;
import com.cleannrooster.decilib.ai.goal.GoalBinding;
import com.cleannrooster.decilib.ai.goal.MobBrainGoal;
import com.cleannrooster.decilib.ai.goal.StopReason;
import com.cleannrooster.decilib.ai.statemachine.MobStateMachine;
import com.cleannrooster.decilib.ai.statemachine.Phase;
import com.cleannrooster.decilib.ai.statemachine.PhaseTransition;
import com.cleannrooster.decilib.ai.statemachine.TransitionContext;
import com.cleannrooster.decilib.ai.stimulus.AIStimulus;
import com.cleannrooster.decilib.ai.stimulus.BaseAIStimulusBuilder;
import com.cleannrooster.decilib.ai.stimulus.StimulusContributor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractMobBrain<T extends LivingEntity,
                                        S extends Enum<S>,
                                        C extends Enum<C>>
        implements MobBrain<T> {

    private static final Logger  LOGGER = LoggerFactory.getLogger(AbstractMobBrain.class);
    private static final boolean STRICT = "true".equalsIgnoreCase(System.getProperty("decilib.strict"));

    // -------------------------------------------------------------------------
    // Owned subsystems (accessible from subclass constructors via lambdas)
    // -------------------------------------------------------------------------

    protected final DefaultCooldownRegistry cooldowns    = new DefaultCooldownRegistry();
    protected final MobStateMachine<S, C>   stateMachine;

    // -------------------------------------------------------------------------
    // Registrations — populated in the subclass constructor
    // -------------------------------------------------------------------------

    private final List<GoalBinding<T, S, C>>   goalBindings     = new ArrayList<>();
    private final List<PhaseTransition>         phaseTransitions = new ArrayList<>();
    private final List<StimulusContributor<T>>  contributors     = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Runtime state
    // -------------------------------------------------------------------------

    private Phase               currentPhase;
    private AIStimulus          currentStimulus;
    private boolean             lastStimulusWasFresh = false;
    private MobBrainGoal<T>     activeGoal;
    private GoalBinding<T,S,C>  activeBinding;
    private int                 tickCount;
    private int                 entityIdOffset;

    // -------------------------------------------------------------------------
    // Sub-systems
    // -------------------------------------------------------------------------

    private final GoalCircuitBreaker<T, S, C>  circuitBreaker = new GoalCircuitBreaker<>();
    @Nullable
    private GoalSelectionListener<T, S, C>     debugListener;

    protected AbstractMobBrain(MobStateMachine<S, C> stateMachine, Phase initialPhase) {
        this.stateMachine = stateMachine;
        this.currentPhase = initialPhase;
    }

    // -------------------------------------------------------------------------
    // Subclass must implement
    // -------------------------------------------------------------------------

    /**
     * Constructs the full stimulus snapshot for this tick.
     *
     * <p>The brain calls all registered {@link StimulusContributor}s on a fresh
     * {@link BaseAIStimulusBuilder} before this method, so base fields (target,
     * health, range, etc.) are already populated. Subclasses should read from
     * the builder, extend it with mob-specific fields, and return the completed
     * snapshot.
     *
     * <p>All world queries for this tick must occur inside this method.
     */
    protected abstract AIStimulus buildStimulus(T entity, ServerWorld world, BaseAIStimulusBuilder builder);

    // -------------------------------------------------------------------------
    // MobBrain implementation
    // -------------------------------------------------------------------------

    @Override
    public void initialize(T entity, ServerWorld world) {
        stateMachine.bind(entity);
        entityIdOffset = entity.getId();
        circuitBreaker.clear();
        lastStimulusWasFresh = false;

        // Validation is a dev-time check — only runs when debug mode is on
        if (DebugConfig.isEnabled()) {
            var validation = validate();
            if (validation.hasWarnings()) {
                var brainName = getClass().getSimpleName();
                for (String warning : validation.warnings()) {
                    if (STRICT) {
                        LOGGER.error("[deci-lib][DEBUG] [{}] (strict) {}", brainName, warning);
                    } else {
                        LOGGER.warn("[deci-lib][DEBUG] [{}] {}", brainName, warning);
                    }
                }
            }
        }
    }

    @Override
    public void tick(T entity, ServerWorld world) {
        tickCount++;

        // 1. Tick cooldowns first so canStart() sees the updated state
        cooldowns.tick();

        // 2. Build frozen stimulus — contributors applied first
        lastStimulusWasFresh = false;
        try {
            var builder = new BaseAIStimulusBuilder();
            for (StimulusContributor<T> c : contributors) {
                c.contribute(builder, entity, world);
            }
            currentStimulus      = buildStimulus(entity, world, builder);
            lastStimulusWasFresh = true;
        } catch (Exception e) {
            if (DebugConfig.isEnabled()) {
                LOGGER.error("[deci-lib][DEBUG] buildStimulus threw for entity {} [{}]: {}",
                        entity.getId(), getClass().getSimpleName(), e.getMessage(), e);
            }
            if (currentStimulus == null) return; // no safe state on first tick
            // retain previous stimulus; lastStimulusWasFresh remains false
        }

        // 3. Phase evaluation — every tick, health-critical
        evaluatePhases(entity, world);

        // 4. State machine evaluation — stance first, then combat state.
        // Combat state evaluation is suppressed while the active goal is committed
        // (e.g. mid-burrow) so the state machine cannot interrupt a multi-tick
        // sequence that has no valid intermediate states.
        var ctx = new TransitionContext(currentStimulus, cooldowns.asReadOnly());
        boolean goalCommitted = activeGoal != null && activeGoal.isCommitted();
        if (Math.floorMod(tickCount + entityIdOffset, 10) == 0) stateMachine.evaluateStance(ctx);
        if (!goalCommitted && Math.floorMod(tickCount + entityIdOffset, 2) == 0) stateMachine.evaluateCombatState(ctx);

        // 5. Tick the active goal; stop on completion or propagated exception
        try {
            tickActiveGoal(entity, world);
        } catch (Exception e) {
            if (DebugConfig.isEnabled()) {
                LOGGER.error("[deci-lib][DEBUG] Goal {} threw during tick — entity {} stance={} state={}: {}",
                        activeGoal != null ? activeGoal.getClass().getSimpleName() : "null",
                        entity.getId(),
                        stateMachine.getCurrentStance(),
                        stateMachine.getCurrentCombatState(),
                        e.getMessage(), e);
            }
            if (activeBinding != null && lastStimulusWasFresh) {
                circuitBreaker.recordFailure(activeBinding, tickCount);
            }
            safeStop(entity, world, StopReason.PREEMPTED);
        }

        // 6. Select a new goal when idle, or preempt with a strictly higher-priority one.
        // Committed goals are never preempted (they own the frame until done).
        if (lastStimulusWasFresh) {
            try {
                selectGoal(entity, world);
            } catch (Exception e) {
                if (DebugConfig.isEnabled()) {
                    LOGGER.error("[deci-lib][DEBUG] selectGoal threw — entity {} stance={} state={}: {}",
                            entity.getId(),
                            stateMachine.getCurrentStance(),
                            stateMachine.getCurrentCombatState(),
                            e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public AIStimulus getCurrentStimulus() { return currentStimulus; }

    @Override
    public AbilityCooldownRegistry getCooldowns() { return cooldowns; }

    @Override
    public MobStateMachine<?, ?> getStateMachine() { return stateMachine; }

    @Override
    public Phase getCurrentPhase() { return currentPhase; }

    @Override
    public @Nullable MobBrainGoal<?> getActiveGoal() { return activeGoal; }

    @Override
    public void teardown(T entity, ServerWorld world) {
        if (activeGoal != null) {
            try {
                activeGoal.stop(entity, world, StopReason.SHUTDOWN);
            } catch (Exception e) {
                if (DebugConfig.isEnabled()) {
                    LOGGER.error("[deci-lib][DEBUG] Goal {} threw in stop(SHUTDOWN) — entity {} stance={} state={}: {}",
                            activeGoal.getClass().getSimpleName(),
                            entity.getId(),
                            stateMachine.getCurrentStance(),
                            stateMachine.getCurrentCombatState(),
                            e.getMessage(), e);
                }
            } finally {
                activeGoal    = null;
                activeBinding = null;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Registration helpers — call from the subclass constructor
    // -------------------------------------------------------------------------

    /**
     * Registers a goal binding. Bindings are maintained in descending priority
     * order so {@link #selectGoal} finds the highest-priority candidate first
     * without allocating on every tick.
     */
    protected final void addGoalBinding(GoalBinding<T, S, C> binding) {
        int insertAt = 0;
        while (insertAt < goalBindings.size()
                && goalBindings.get(insertAt).priority() >= binding.priority()) {
            insertAt++;
        }
        goalBindings.add(insertAt, binding);
    }

    /**
     * Registers a phase transition. Transitions are maintained sorted by
     * {@code healthThresholdPct} descending so the highest threshold fires first
     * when a large damage spike crosses multiple thresholds in one tick. Only
     * one transition fires per tick; lower thresholds re-evaluate on subsequent
     * ticks.
     *
     * @throws IllegalArgumentException if a transition to the same Phase (by
     *         ordinal) has already been registered
     */
    protected final void addPhaseTransition(PhaseTransition pt) {
        for (PhaseTransition existing : phaseTransitions) {
            if (existing.toPhase().ordinal() == pt.toPhase().ordinal()) {
                throw new IllegalArgumentException(
                        "Duplicate phase transition to ordinal " + pt.toPhase().ordinal()
                        + " ('" + pt.toPhase().id() + "'). Each Phase may have at most one transition.");
            }
        }
        int insertAt = 0;
        while (insertAt < phaseTransitions.size()
                && phaseTransitions.get(insertAt).healthThresholdPct() >= pt.healthThresholdPct()) {
            insertAt++;
        }
        phaseTransitions.add(insertAt, pt);
    }

    protected final void addContributor(StimulusContributor<T> contributor) {
        contributors.add(contributor);
    }

    // -------------------------------------------------------------------------
    // Debug introspection — zero overhead when listener is null or debug is off
    // -------------------------------------------------------------------------

    /**
     * Registers a listener that is called once per binding evaluated during goal
     * selection. Pass {@code null} to remove the listener.
     *
     * <p>The listener is silently ignored (set to {@code null}) when debug mode
     * is disabled, preserving the zero-overhead guarantee. All call sites in
     * {@link #selectGoal} are already guarded by a {@code debugListener != null}
     * check; this method adds the additional {@link DebugConfig#isEnabled()}
     * guard at registration time so the field is never set in production.
     */
    public final void setDebugListener(@Nullable GoalSelectionListener<T, S, C> listener) {
        // Only store the listener when debug mode is on — null in production
        this.debugListener = DebugConfig.isEnabled() ? listener : null;
    }

    // -------------------------------------------------------------------------
    // Phase lifecycle — override for side effects
    // -------------------------------------------------------------------------

    /** Called after the entity enters a new phase. Override for stat changes, resets, particles. */
    protected void onPhaseEnter(Phase phase, T entity, ServerWorld world) {}

    /** Called before the entity leaves a phase. Override for cleanup. */
    protected void onPhaseExit(Phase phase, T entity, ServerWorld world) {}

    // -------------------------------------------------------------------------
    // Phase evaluation (every tick)
    // -------------------------------------------------------------------------

    private void evaluatePhases(T entity, ServerWorld world) {
        for (PhaseTransition pt : phaseTransitions) {
            if (pt.isTriggered()) continue;
            if (currentStimulus.selfHealthPct() < pt.healthThresholdPct()) {
                Phase previous = currentPhase;
                currentPhase = pt.toPhase();
                pt.trigger();

                stateMachine.applyStanceLockout(pt.lockedStances());
                stateMachine.applyCombatStateLockout(pt.lockedStates());

                if (activeGoal != null) {
                    try {
                        activeGoal.stop(entity, world, StopReason.PREEMPTED, cooldowns);
                    } catch (Exception e) {
                        if (DebugConfig.isEnabled()) {
                            LOGGER.error("[deci-lib][DEBUG] Goal {} threw in stop(PREEMPTED) during phase"
                                    + " transition — entity {} stance={} state={}: {}",
                                    activeGoal.getClass().getSimpleName(),
                                    entity.getId(),
                                    stateMachine.getCurrentStance(),
                                    stateMachine.getCurrentCombatState(),
                                    e.getMessage(), e);
                        }
                    } finally {
                        activeGoal    = null;
                        activeBinding = null;
                    }
                }

                onPhaseExit(previous, entity, world);
                onPhaseEnter(currentPhase, entity, world);

                for (String id : pt.cooldownsToReset()) {
                    cooldowns.reset(id);
                }
                for (Map.Entry<String, Integer> entry : pt.cooldownsToSet().entrySet()) {
                    cooldowns.forceSet(entry.getKey(), entry.getValue());
                }

                break;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Active goal lifecycle
    // -------------------------------------------------------------------------

    private void tickActiveGoal(T entity, ServerWorld world) {
        if (activeGoal == null) return;

        // Preempt goals that are no longer valid for the current stance/state
        if (activeBinding != null
                && !activeBinding.isValidIn(stateMachine.getCurrentStance(),
                                             stateMachine.getCurrentCombatState())) {
            if (DebugConfig.isEnabled()) {
                LOGGER.info("[deci-lib][DEBUG] Preempting goal {} — entity {} moved to"
                        + " stance={} state={} where goal is invalid",
                        activeGoal.getClass().getSimpleName(),
                        entity.getId(),
                        stateMachine.getCurrentStance(),
                        stateMachine.getCurrentCombatState());
            }
            try {
                activeGoal.stop(entity, world, StopReason.PREEMPTED, cooldowns);
            } catch (Exception e) {
                if (DebugConfig.isEnabled()) {
                    LOGGER.error("[deci-lib][DEBUG] Goal {} threw in stop(PREEMPTED) during"
                            + " state-validity preemption — entity {} stance={} state={}: {}",
                            activeGoal.getClass().getSimpleName(),
                            entity.getId(),
                            stateMachine.getCurrentStance(),
                            stateMachine.getCurrentCombatState(),
                            e.getMessage(), e);
                }
            } finally {
                activeGoal    = null;
                activeBinding = null;
            }
            return;
        }

        // shouldContinue() throwing propagates to tick()'s catch — counted as failure
        if (!activeGoal.shouldContinue(entity, currentStimulus)) {
            if (DebugConfig.isEnabled()) {
                LOGGER.info("[deci-lib][DEBUG] Goal {} completed — entity {} stance={} state={}",
                        activeGoal.getClass().getSimpleName(),
                        entity.getId(),
                        stateMachine.getCurrentStance(),
                        stateMachine.getCurrentCombatState());
            }
            try {
                activeGoal.stop(entity, world, StopReason.COMPLETED);
            } catch (Exception e) {
                if (DebugConfig.isEnabled()) {
                    LOGGER.error("[deci-lib][DEBUG] Goal {} threw in stop(COMPLETED)"
                            + " — entity {} stance={} state={}: {}",
                            activeGoal.getClass().getSimpleName(),
                            entity.getId(),
                            stateMachine.getCurrentStance(),
                            stateMachine.getCurrentCombatState(),
                            e.getMessage(), e);
                }
            } finally {
                activeGoal    = null;
                activeBinding = null;
            }
            return;
        }

        // tick() throwing propagates to tick()'s catch — counted as failure
        activeGoal.tick(entity, world, currentStimulus, cooldowns);
    }

    private void selectGoal(T entity, ServerWorld world) {
        // Committed goals cannot be preempted — they own the frame (e.g. mid-burrow sequence).
        if (activeGoal != null && activeGoal.isCommitted()) return;

        S       stance      = stateMachine.getCurrentStance();
        C       combatState = stateMachine.getCurrentCombatState();
        boolean debug       = debugListener != null; // null in production; non-null only when debug on

        for (GoalBinding<T, S, C> binding : goalBindings) {

            // When a goal is running, only a strictly higher-priority binding can replace it.
            // Bindings are sorted descending so once we reach the active priority level we stop.
            if (activeGoal != null && activeBinding != null
                    && binding.priority() <= activeBinding.priority()) break;

            if (circuitBreaker.isBlocked(binding, tickCount)) {
                if (debug) debugListener.onEvent(new GoalSelectionEvent<>(binding, GoalSkipReason.CIRCUIT_BROKEN));
                continue;
            }

            var stanceOk = binding.validStances().isEmpty()
                    || binding.validStances().contains(stance);
            if (!stanceOk) {
                if (debug) debugListener.onEvent(new GoalSelectionEvent<>(binding, GoalSkipReason.WRONG_STANCE));
                continue;
            }

            var stateOk = binding.validCombatStates().isEmpty()
                    || binding.validCombatStates().contains(combatState);
            if (!stateOk) {
                if (debug) debugListener.onEvent(new GoalSelectionEvent<>(binding, GoalSkipReason.WRONG_COMBAT_STATE));
                continue;
            }

            boolean canStart;
            try {
                canStart = binding.goal().canStart(currentStimulus, cooldowns.asReadOnly());
            } catch (Exception e) {
                if (DebugConfig.isEnabled()) {
                    LOGGER.error("[deci-lib][DEBUG] Goal {} threw in canStart()"
                            + " — entity {} stance={} state={}: {}",
                            binding.goal().getClass().getSimpleName(),
                            entity.getId(), stance, combatState,
                            e.getMessage(), e);
                }
                if (lastStimulusWasFresh) {
                    circuitBreaker.recordFailure(binding, tickCount);
                }
                if (debug) debugListener.onEvent(new GoalSelectionEvent<>(binding, GoalSkipReason.CANNOT_START));
                continue;
            }
            if (!canStart) {
                if (debug) debugListener.onEvent(new GoalSelectionEvent<>(binding, GoalSkipReason.CANNOT_START));
                continue;
            }

            // Stop the current goal before starting the preempting one
            if (activeGoal != null) {
                if (debug) {
                    LOGGER.info("[deci-lib][DEBUG] Preempting goal {} (pri {}) with {} (pri {})"
                            + " — entity {} stance={} state={}",
                            activeGoal.getClass().getSimpleName(), activeBinding.priority(),
                            binding.goal().getClass().getSimpleName(), binding.priority(),
                            entity.getId(), stance, combatState);
                }
                safeStop(entity, world, StopReason.PREEMPTED);
            }

            try {
                activeBinding = binding;
                activeGoal    = binding.goal();
                activeGoal.reset(entity);
                activeGoal.start(entity, world);
            } catch (Exception e) {
                if (DebugConfig.isEnabled()) {
                    LOGGER.error("[deci-lib][DEBUG] Goal {} threw in start()"
                            + " — entity {} stance={} state={}: {}",
                            binding.goal().getClass().getSimpleName(),
                            entity.getId(), stance, combatState,
                            e.getMessage(), e);
                }
                if (lastStimulusWasFresh) {
                    circuitBreaker.recordFailure(binding, tickCount);
                }
                activeGoal    = null;
                activeBinding = null;
                continue;
            }

            if (debug) {
                LOGGER.info("[deci-lib][DEBUG] Selected goal {} — entity {} stance={} state={}",
                        binding.goal().getClass().getSimpleName(),
                        entity.getId(), stance, combatState);
                debugListener.onEvent(new GoalSelectionEvent<>(binding, null));
            }
            return;
        }
    }

    private void safeStop(T entity, ServerWorld world, StopReason reason) {
        if (activeGoal != null) {
            try {
                activeGoal.stop(entity, world, reason, cooldowns);
            } catch (Exception ignored) {
                // Exception already logged at the call site that triggered safeStop
            }
        }
        activeGoal    = null;
        activeBinding = null;
    }

    // -------------------------------------------------------------------------
    // Initialization validation — only called when debug mode is on
    // -------------------------------------------------------------------------

    private BrainValidationResult validate() {
        BrainValidationResult.Builder b = BrainValidationResult.builder();

        var allLockedStates  = new HashSet<Object>();
        var allLockedStances = new HashSet<Object>();
        for (PhaseTransition pt : phaseTransitions) {
            allLockedStates.addAll(pt.lockedStates());
            allLockedStances.addAll(pt.lockedStances());
        }

        stateMachine.validateTransitions(allLockedStates, allLockedStances, b::warn);

        for (C state : stateMachine.referencedCombatStates()) {
            boolean covered = false;
            for (GoalBinding<T, S, C> gb : goalBindings) {
                if (gb.validCombatStates().isEmpty() || gb.validCombatStates().contains(state)) {
                    covered = true;
                    break;
                }
            }
            if (!covered) {
                b.warn("CombatState " + state
                        + " has transitions targeting it but no covering GoalBinding"
                        + " (entity will idle whenever this state is active)");
            }
        }

        Set<String> registered = cooldowns.registeredIds();
        for (GoalBinding<T, S, C> gb : goalBindings) {
            for (String id : gb.goal().declaredAbilityIds()) {
                if (!registered.contains(id)) {
                    b.warn("Goal " + gb.goal().getClass().getSimpleName()
                            + " declares ability '" + id
                            + "' but it was not registered via cooldowns.register()"
                            + " — the ability will behave as always-ready in non-strict mode");
                }
            }
        }

        return b.build();
    }

    // -------------------------------------------------------------------------
    // Accessors for subclasses
    // -------------------------------------------------------------------------

    protected final int tickCount() { return tickCount; }
}
