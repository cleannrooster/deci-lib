package com.cleannrooster.decilib.builder.registry;

import com.cleannrooster.decilib.builder.preset.Preset;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


public final class PresetRegistry {

    private static final Map<String, Preset> REGISTRY = new LinkedHashMap<>();

    private PresetRegistry() {}

    public static void register(Preset preset) {
        var id = preset.id();
        if (REGISTRY.containsKey(id)) {
            throw new IllegalStateException("Preset id '" + id + "' is already registered");
        }
        REGISTRY.put(id, preset);
    }

    @Nullable
    public static Preset get(String id) {
        return REGISTRY.get(id);
    }

    public static boolean isRegistered(String id) {
        return REGISTRY.containsKey(id);
    }

    /** Returns an unmodifiable view of all registered preset IDs. */
    public static Set<String> knownIds() {
        return Collections.unmodifiableSet(REGISTRY.keySet());
    }
}
