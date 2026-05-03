package com.cleannrooster.decilib.ai.ambush;

import com.cleannrooster.decilib.ai.statemachine.TransitionContext;
import net.minecraft.entity.mob.MobEntity;

import java.util.function.Predicate;

public class AmbushModule<E extends MobEntity & CanAmbush> {

    private final AmbushConfig<E>               config;
    private final WatchFromHidingBrainGoal<E>   watchGoal;
    private final SurfaceAndAmbushBrainGoal<E>  surfaceGoal;
    private final EnterHidingBrainGoal<E>       enterHidingGoal;
    public AmbushModule(AmbushConfig<E> config) {
        this.config          = config;
        this.watchGoal       = new WatchFromHidingBrainGoal<>(config);
        this.surfaceGoal     = new SurfaceAndAmbushBrainGoal<>();
        this.enterHidingGoal = new EnterHidingBrainGoal<>(config.rehideDelayTicks());
    }
    // -------------------------------------------------------------------------
    // Sub-goal accessors
    // -------------------------------------------------------------------------
    /**
     * The watch goal. Bind to the HIDDEN combat state.
     * Runs continuously while hidden; sets entity target when a threat is found.
     */
    public WatchFromHidingBrainGoal<E> watchGoal() {
        return watchGoal;
    }
    /**
     * The surface goal. Bind to the ACTIVE combat state at the highest priority
     * (e.g. 100). Fires once at the start of each ACTIVE window; no-op if the
     * entity is already surfaced.
     */
    public SurfaceAndAmbushBrainGoal<E> surfaceGoal() {
        return surfaceGoal;
    }
    /**
     * The enter-hiding goal. Bind to the REHIDING combat state. Waits for the
     * configured rehide delay, then calls {@link CanAmbush#enterHiddenState()}.
     */
    public EnterHidingBrainGoal<E> enterHidingGoal() {
        return enterHidingGoal;
    }
    // -------------------------------------------------------------------------
    // State machine transition predicates
    // -------------------------------------------------------------------------
    /**
     * Condition for the {@code HIDDEN → ACTIVE} transition:
     * fires when a target is present and within the configured ambush radius.
     */
    public Predicate<TransitionContext> hiddenToActiveCondition() {
        return ctx -> ctx.stimulus().hasTarget()
                   && ctx.stimulus().targetDistance() < config.ambushRadius();
    }
    /**
     * Condition for the {@code ACTIVE → REHIDING} transition:
     * fires when the combat target is lost.
     */
    public Predicate<TransitionContext> activeToRehidingCondition() {
        return ctx -> !ctx.stimulus().hasTarget();
    }
    /**
     * Condition for the {@code REHIDING → HIDDEN} transition: unconditional.
     *
     * <p>The EnterHidingBrainGoal holds isCommitted()=true
     * for the duration of its rehide countdown, which suppresses combat-state
     * evaluation. This transition therefore only fires after that goal completes
     * and releases the committed lock — effectively gating the transition without
     * requiring an explicit predicate on entity state.
     */
    public Predicate<TransitionContext> rehidingToHiddenCondition() {
        return ctx -> true;
    }
    // -------------------------------------------------------------------------
    /** Returns the shared configuration for inspection or sub-goal tuning. */
    public AmbushConfig<E> config() {
        return config;
    }
}
