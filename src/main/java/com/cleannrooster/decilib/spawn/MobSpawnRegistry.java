package com.cleannrooster.decilib.spawn;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;


public final class MobSpawnRegistry {

    private static final Map<String, MobSpawnConfig> CONFIGS = new LinkedHashMap<>();

    private MobSpawnRegistry() {}

    public static void register(MobSpawnConfig config) {
        CONFIGS.put(config.mobId(), config);
    }

    public static Optional<MobSpawnConfig> get(String mobId) {
        return Optional.ofNullable(CONFIGS.get(mobId));
    }

    public static Collection<MobSpawnConfig> all() {
        return Collections.unmodifiableCollection(CONFIGS.values());
    }

    public static void clear() {
        CONFIGS.clear();
    }
}
