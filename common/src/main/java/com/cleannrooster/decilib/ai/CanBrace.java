package com.cleannrooster.decilib.ai;

public interface CanBrace {

    void enterBraceState(float damageReduction);

    void exitBraceState();

    boolean isBracing();

    float getBraceReduction();

    boolean wasLastHitProjectile();

    float getRecentDamageTaken();
}
