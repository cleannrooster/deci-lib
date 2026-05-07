package com.cleannrooster.decilib.ai.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Shared spatial and target-validity predicates used in stimulus construction
 * and goal guards. Centralised here so goals and brains do not duplicate range
 * math.
 */
public final class RangePredicates {

    private RangePredicates() {}

    public static boolean withinRange(LivingEntity a, LivingEntity b, double range) {
        return a.squaredDistanceTo(b) <= range * range;
    }

    public static boolean hasLineOfSight(LivingEntity entity, LivingEntity target) {
        return entity.canSee(target);
    }

    public static boolean isValidTarget(LivingEntity target) {
        return target != null
                && target.isAlive()
                && !target.isSpectator();
    }

    public static double distanceTo(LivingEntity a, LivingEntity b) {
        return a.distanceTo(b);
    }
}
