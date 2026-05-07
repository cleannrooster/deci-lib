package com.cleannrooster.decilib.ai;

import org.jetbrains.annotations.Nullable;

public final class DebugConfig {

    private static final boolean JVM_FLAG =
            "true".equalsIgnoreCase(System.getProperty("decilib.debug"));

    private static @Nullable Boolean programmaticOverride = null;
    private static boolean           configValue          = false;

    private static boolean enabled = JVM_FLAG;

    private DebugConfig() {}

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setConfigValue(boolean value) {
        configValue = value;
        recompute();
    }

    public static void setOverride(@Nullable Boolean value) {
        programmaticOverride = value;
        recompute();
    }

    private static void recompute() {
        if (programmaticOverride != null) {
            enabled = programmaticOverride;
        } else if (JVM_FLAG) {
            enabled = true;
        } else {
            enabled = configValue;
        }
    }
}
