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

    boolean selfIsHidden();
}
