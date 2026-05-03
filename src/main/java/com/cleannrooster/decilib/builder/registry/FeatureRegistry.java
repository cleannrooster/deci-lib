package com.cleannrooster.decilib.builder.registry;

import com.cleannrooster.decilib.builder.feature.BehaviorFeature;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


public final class FeatureRegistry {

    private static final Map<String, BehaviorFeature> REGISTRY = new LinkedHashMap<>();

    private FeatureRegistry() {}

    public static void register(BehaviorFeature feature) {
        var id = feature.typeId();
        if (REGISTRY.containsKey(id)) {
            throw new IllegalStateException("Feature type id '" + id + "' is already registered");
        }
        REGISTRY.put(id, feature);
    }

    @Nullable
    public static BehaviorFeature get(String id) {
        return REGISTRY.get(id);
    }

    public static boolean isRegistered(String id) {
        return REGISTRY.containsKey(id);
    }

    /** Returns an unmodifiable view of all registered feature type IDs. */
    public static Set<String> knownIds() {
        return Collections.unmodifiableSet(REGISTRY.keySet());
    }
}
