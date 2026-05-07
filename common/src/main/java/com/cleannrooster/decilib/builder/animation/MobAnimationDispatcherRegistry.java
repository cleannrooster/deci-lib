package com.cleannrooster.decilib.builder.animation;

public final class MobAnimationDispatcherRegistry {

    private static MobAnimationDispatcher active = MobAnimationDispatcher.NOOP;

    private MobAnimationDispatcherRegistry() {}

    public static void register(MobAnimationDispatcher dispatcher) {
        active = dispatcher;
    }

    public static MobAnimationDispatcher get() {
        return active;
    }
}
