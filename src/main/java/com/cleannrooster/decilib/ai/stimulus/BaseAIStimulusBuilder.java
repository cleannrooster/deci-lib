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
                selfIsInWater,
                selfIsInLava,
                selfIsHidden,
                recentDamageTaken,
                lastHitWasProjectile,
                isNearAnchor,
                nearbyAllyCount,
                fightProgressPct,
                nearbyThreatCount,
                false,   // targetNearObstacle — Phase 1 stub
                false    // allyUnderAttack    — Phase 1 stub
        );
    }
}
