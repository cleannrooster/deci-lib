package com.cleannrooster.decilib.ai.stimulus;

public interface AIStimulus {

    // -------------------------------------------------------------------------
    // Target geometry
    // -------------------------------------------------------------------------

    double targetDistance();

    boolean targetInMeleeRange();

    boolean targetInEngagementRange();

    boolean hasLineOfSight();

    // -------------------------------------------------------------------------
    // Health
    // -------------------------------------------------------------------------

    float selfHealthPct();

    float targetHealthPct();

    // -------------------------------------------------------------------------
    // Combat state
    // -------------------------------------------------------------------------

    boolean targetIsBlocking();

    int ticksSinceLastHit();

    int ticksSinceLastAttack();

    boolean hasTarget();

    // -------------------------------------------------------------------------
    // Environment
    // -------------------------------------------------------------------------

    boolean selfIsOnGround();

    boolean selfIsInFluid();

    boolean selfIsInWater();

    boolean selfIsInLava();

    boolean selfIsHidden();

    // -------------------------------------------------------------------------
    // Recent combat history
    // -------------------------------------------------------------------------

    float recentDamageTaken();

    boolean lastHitWasProjectile();

    // -------------------------------------------------------------------------
    // AiProfile axes
    // -------------------------------------------------------------------------

    /** True when the mob is within its follow range of its spawn anchor. Used by TERRITORIAL. */
    boolean isNearAnchor();

    /** Number of allied DataDrivenMob entities within ~12 blocks. Used by SWARMER. */
    int nearbyAllyCount();

    /**
     * Fraction of max health taken as damage since combat began (capped at 1.0).
     * Used by ESCALATING.
     */
    float fightProgressPct();

    /**
     * Number of hostile entities near the mob's current target that are also
     * targeting this mob. Used by COWARD.
     */
    int nearbyThreatCount();

    /**
     * True when the target is cornered near a wall or obstacle.
     * Phase 1 stub — always returns {@code false}. Real implementation requires raycasting.
     * Used by PREDATORY.
     */
    boolean targetNearObstacle();

    /**
     * True when an allied mob is currently taking damage.
     * Phase 1 stub — always returns {@code false}. Real implementation requires damage event tracking.
     * Used by PACK_HUNTER.
     */
    boolean allyUnderAttack();
}
