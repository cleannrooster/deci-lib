package com.cleannrooster.decilib.ai.cooldown;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * HashMap-backed implementation of {@link AbilityCooldownRegistry}.
 * Entries at zero ticks are eagerly removed to keep iteration lean.
 *
 * <p>When the JVM property {@code decilib.strict=true} is set, any
 * {@link #isReady} or {@link #trigger} call on an ID that was not previously
 * passed to {@link #register} throws {@link IllegalArgumentException}. This
 * surfaces ability-ID typos during testing rather than letting them silently
 * behave as always-ready.
 */
public final class DefaultCooldownRegistry implements AbilityCooldownRegistry {

    private static final boolean STRICT =
            "true".equalsIgnoreCase(System.getProperty("decilib.strict"));
    private final Map<String, Integer> cooldowns    = new HashMap<>();
    private final Set<String>          registeredIds = new HashSet<>();
    private final ReadOnlyCooldownRegistry readOnlyView = new ReadOnlyView();
    @Override
    public void register(String abilityId, int cooldown) {
        registeredIds.add(abilityId);
        cooldowns.put(abilityId, cooldown);
    }
    @Override
    public Set<String> registeredIds() {
        return Collections.unmodifiableSet(registeredIds);
    }
    @Override
    public boolean isReady(String abilityId) {
        checkRegistered(abilityId);
        return cooldowns.getOrDefault(abilityId, 0) <= 0;
    }
    @Override
    public int remaining(String abilityId) {
        return Math.max(0, cooldowns.getOrDefault(abilityId, 0));
    }
    @Override
    public void trigger(String abilityId, int durationTicks) {
        checkRegistered(abilityId);
        if (isReady(abilityId)) {
            cooldowns.put(abilityId, durationTicks);
        }
    }
    private void checkRegistered(String abilityId) {
        if (STRICT && !registeredIds.contains(abilityId)) {
            throw new IllegalArgumentException(
                    "Unknown ability id: '" + abilityId + "'. Did you call register() in initialize()?");
        }
    }
    @Override
    public void forceSet(String abilityId, int durationTicks) {
        cooldowns.put(abilityId, durationTicks);
    }
    @Override
    public void reset(String abilityId) {
        cooldowns.remove(abilityId);
    }
    @Override
    public void resetAll() {
        cooldowns.clear();
    }
    @Override
    public void tick() {
        cooldowns.replaceAll((id, ticks) -> ticks - 1);
        cooldowns.entrySet().removeIf(e -> e.getValue() <= 0);
    }
    @Override
    public ReadOnlyCooldownRegistry asReadOnly() {
        return readOnlyView;
    }
    private final class ReadOnlyView implements ReadOnlyCooldownRegistry {
        @Override
        public boolean isReady(String abilityId) {
            return DefaultCooldownRegistry.this.isReady(abilityId);
        }

        @Override
        public int remaining(String abilityId) {
            return DefaultCooldownRegistry.this.remaining(abilityId);
        }
    }
}
