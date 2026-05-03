package com.cleannrooster.decilib.builder.feature;

import org.jetbrains.annotations.Nullable;

import java.util.Map;


public final class FeatureConfig {

    private final String              type;
    private final Map<String, Object> params;
    @Nullable private final FeatureHooks hooks;

    public FeatureConfig(String type, Map<String, Object> params) {
        this(type, params, null);
    }

    public FeatureConfig(String type, Map<String, Object> params, @Nullable FeatureHooks hooks) {
        this.type   = type;
        this.params = Map.copyOf(params);
        this.hooks  = hooks;
    }

    public String type() { return type; }

    public @Nullable FeatureHooks hooks() { return hooks; }

    public double getDouble(String key, double defaultValue) {
        Object v = params.get(key);
        return v instanceof Number n ? n.doubleValue() : defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        Object v = params.get(key);
        return v instanceof Number n ? n.intValue() : defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        Object v = params.get(key);
        return v instanceof Boolean b ? b : defaultValue;
    }

    public String getString(String key, String defaultValue) {
        Object v = params.get(key);
        return v instanceof String s ? s : defaultValue;
    }

    public boolean has(String key) { return params.containsKey(key); }
}
