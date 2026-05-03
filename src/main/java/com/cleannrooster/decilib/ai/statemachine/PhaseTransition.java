package com.cleannrooster.decilib.ai.statemachine;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class PhaseTransition {

    private final float healthThresholdPct;
    private final Phase toPhase;
    private final Set<Object> lockedStances;
    private final Set<Object> lockedStates;
    private final Set<String> cooldownsToReset;
    private final Map<String, Integer> cooldownsToSet;
    private boolean triggered    = false;
    private boolean triggerCalled = false;

    private PhaseTransition(Builder builder) {
        this.healthThresholdPct = builder.healthThresholdPct;
        this.toPhase = builder.toPhase;
        this.lockedStances    = Collections.unmodifiableSet(new HashSet<>(builder.lockedStances));
        this.lockedStates     = Collections.unmodifiableSet(new HashSet<>(builder.lockedStates));
        this.cooldownsToReset = Collections.unmodifiableSet(new HashSet<>(builder.cooldownsToReset));
        this.cooldownsToSet   = Collections.unmodifiableMap(new HashMap<>(builder.cooldownsToSet));
    }

    /** Fires when {@code selfHealthPct} drops strictly below this value. */
    public float healthThresholdPct() { return healthThresholdPct; }

    /** The phase the entity transitions into. */
    public Phase toPhase() { return toPhase; }

    /** Stances permanently removed from evaluation after this transition fires. */
    public Set<Object> lockedStances() { return lockedStances; }

    /** CombatStates permanently removed from evaluation after this transition fires. */
    public Set<Object> lockedStates() { return lockedStates; }

    /** Ability IDs whose cooldowns are cleared immediately after this transition fires. */
    public Set<String> cooldownsToReset() { return cooldownsToReset; }

    /** Ability IDs mapped to specific cooldown durations applied immediately after this transition fires. */
    public Map<String, Integer> cooldownsToSet() { return cooldownsToSet; }

    /** Whether this transition has already fired. */
    public boolean isTriggered() { return triggered; }

    /**
     * Marks this transition as triggered. Must be called exactly once by
     * {@code AbstractMobBrain}. Calling it a second time throws to catch
     * accidental double-trigger bugs at the earliest possible point.
     */
    public void trigger() {
        if (triggerCalled) {
            throw new IllegalStateException(
                    "PhaseTransition.trigger() called more than once for phase '"
                    + toPhase.id() + "'. Only AbstractMobBrain may call this.");
        }
        triggerCalled = true;
        triggered = true;
    }

    // -------------------------------------------------------------------------

    public static final class Builder {
        private final float healthThresholdPct;
        private final Phase toPhase;
        private final Set<Object>        lockedStances    = new HashSet<>();
        private final Set<Object>        lockedStates     = new HashSet<>();
        private final Set<String>        cooldownsToReset = new HashSet<>();
        private final Map<String, Integer> cooldownsToSet = new HashMap<>();

        public Builder(float healthThresholdPct, Phase toPhase) {
            this.healthThresholdPct = healthThresholdPct;
            this.toPhase = toPhase;
        }

        public Builder lockStances(Collection<?> stances) {
            this.lockedStances.addAll(stances);
            return this;
        }

        public Builder lockStates(Collection<?> states) {
            this.lockedStates.addAll(states);
            return this;
        }

        /** Clears the cooldown for each listed ability ID when this transition fires. */
        public Builder resetCooldowns(Collection<String> abilityIds) {
            this.cooldownsToReset.addAll(abilityIds);
            return this;
        }

        /** Forces specific cooldown durations for the given ability IDs when this transition fires. */
        public Builder setCooldowns(Map<String, Integer> abilityIdToTicks) {
            this.cooldownsToSet.putAll(abilityIdToTicks);
            return this;
        }

        public PhaseTransition build() {
            return new PhaseTransition(this);
        }
    }
}
