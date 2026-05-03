package com.cleannrooster.decilib.ai.cooldown;

/**
 * Centralized cooldown tracker for a single entity's abilities.
 *
 * <p>Owned and ticked by the brain. Goals call {@link #trigger} only inside
 * their {@code tick()} method (never inside {@code canStart()}, which receives
 * a {@link ReadOnlyCooldownRegistry} view instead).
 *
 * <p>Extends {@link ReadOnlyCooldownRegistry} so the full registry can be
 * passed wherever only read access is needed without allocating a wrapper.
 */
import java.util.Set;

public interface AbilityCooldownRegistry extends ReadOnlyCooldownRegistry {
    default void register(String abilityId, int durationTicks) {}
    void trigger(String abilityId, int durationTicks);
    void forceSet(String abilityId, int durationTicks);
    void reset(String abilityId);
    void resetAll();
    void tick();
    ReadOnlyCooldownRegistry asReadOnly();
    default Set<String> registeredIds() {
        return Set.of();
    }
}
