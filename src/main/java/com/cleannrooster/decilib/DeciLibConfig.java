package com.cleannrooster.decilib;

import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.ConfigData;

@Config(name = "deci-lib")
public class DeciLibConfig implements ConfigData {

    /**
     * Enables verbose per-tick logging for brain decisions, goal selection,
     * state transitions, and the circuit breaker.
     *
     * Can also be forced on via the JVM flag {@code -Ddecilib.debug=true}
     * regardless of this setting.
     */
    public boolean debugMode = false;
}
