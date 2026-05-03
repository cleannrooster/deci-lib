package com.cleannrooster.decilib.ai.cooldown;

public interface ReadOnlyCooldownRegistry {

    boolean isReady(String abilityId);
    int remaining(String abilityId);
}
