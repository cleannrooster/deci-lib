package com.cleannrooster.decilib.ai.stimulus;

public record BaseAIStimulus(
        double targetDistance,
        boolean targetInMeleeRange,
        boolean targetInEngagementRange,
        boolean hasLineOfSight,
        float selfHealthPct,
        float targetHealthPct,
        boolean targetIsBlocking,
        int ticksSinceLastHit,
        int ticksSinceLastAttack,
        boolean hasTarget,
        boolean selfIsOnGround,
        boolean selfIsInFluid,
        boolean selfIsInWater,
        boolean selfIsInLava,
        boolean selfIsHidden,
        float recentDamageTaken,
        boolean lastHitWasProjectile,
        // AiProfile axes
        boolean isNearAnchor,
        int     nearbyAllyCount,
        float   fightProgressPct,
        int     nearbyThreatCount,
        boolean targetNearObstacle,   // Phase 1 stub — always false
        boolean allyUnderAttack       // Phase 1 stub — always false
) implements AIStimulus {}
