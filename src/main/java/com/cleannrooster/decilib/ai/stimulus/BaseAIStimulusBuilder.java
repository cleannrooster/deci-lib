package com.cleannrooster.decilib.ai.stimulus;

public final class BaseAIStimulusBuilder
        extends AIStimulusBuilder<BaseAIStimulusBuilder, BaseAIStimulus> {

    @Override
    public BaseAIStimulus build() {
        return new BaseAIStimulus(
                targetDistance,
                targetInMeleeRange,
                targetInEngagementRange,
                hasLineOfSight,
                selfHealthPct,
                targetHealthPct,
                targetIsBlocking,
                ticksSinceLastHit,
                ticksSinceLastAttack,
                hasTarget,
                selfIsOnGround,
                selfIsInFluid,
                selfIsHidden
        );
    }
}
