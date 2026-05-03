package com.cleannrooster.decilib.builder.registry;

import com.cleannrooster.decilib.builder.archetype.Archetype;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


public final class ArchetypeRegistry {

    private static final Map<String, Archetype> REGISTRY = new LinkedHashMap<>();

    private ArchetypeRegistry() {}

    public static void register(Archetype archetype) {
        var id = archetype.id();
        if (REGISTRY.containsKey(id)) {
            throw new IllegalStateException("Archetype id '" + id + "' is already registered");
        }
        REGISTRY.put(id, archetype);
    }

    @Nullable
    public static Archetype get(String id) {
        return REGISTRY.get(id);
    }

    public static boolean isRegistered(String id) {
        return REGISTRY.containsKey(id);
    }

    /** Returns an unmodifiable view of all registered archetype IDs. */
    public static Set<String> knownIds() {
        return Collections.unmodifiableSet(REGISTRY.keySet());
    }
}
