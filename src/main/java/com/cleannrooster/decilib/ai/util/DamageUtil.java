package com.cleannrooster.decilib.ai.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;


public final class DamageUtil {

    private DamageUtil() {}


    public static List<LivingEntity> performArcDamage(
            LivingEntity attacker,
            LivingEntity primaryTarget,
            float halfAngleDeg,
            double range,
            float damage,
            DamageSource source,
            ServerWorld world) {

        var hit = new ArrayList<LivingEntity>();


        var cosThreshold = Math.cos(Math.toRadians(halfAngleDeg));
        var facingDir = attacker.getRotationVec(1.0f);
        var searchBox = buildSearchBox(attacker, range);

        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, searchBox, e -> true)) {
            if (entity == attacker ) continue;
            if (!RangePredicates.isValidTarget(entity)) continue;
            if (attacker.isTeammate(entity)) continue;

            var distSq = attacker.squaredDistanceTo(entity);
            if (distSq > range * range) continue;

            var toEntity = entity.getPos().subtract(attacker.getPos()).normalize();
            if (facingDir.dotProduct(toEntity) >= cosThreshold) {
                entity.damage(source, damage);
                hit.add(entity);
            }
        }

        return hit;
    }


    public static List<LivingEntity> performRadialDamage(
            LivingEntity attacker,
            double range,
            float damage,
            DamageSource source,
            ServerWorld world) {

        var hit = new ArrayList<LivingEntity>();
        var searchBox = buildSearchBox(attacker, range);

        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, searchBox, e -> true)) {
            if (entity == attacker) continue;
            if (!RangePredicates.isValidTarget(entity)) continue;
            if (attacker.isTeammate(entity)) continue;

            if (attacker.squaredDistanceTo(entity) <= range * range) {
                entity.damage(source, damage);
                hit.add(entity);
            }
        }

        return hit;
    }

    public static List<LivingEntity> performRingDamage(
            LivingEntity attacker,
            double innerRadius,
            double outerRadius,
            float damage,
            DamageSource source,
            ServerWorld world) {

        var hit = new ArrayList<LivingEntity>();
        var searchBox = buildSearchBox(attacker, outerRadius);

        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, searchBox, e -> true)) {
            if (entity == attacker) continue;
            if (!RangePredicates.isValidTarget(entity)) continue;
            if (attacker.isTeammate(entity)) continue;

            var distSq = attacker.squaredDistanceTo(entity);
            if (distSq >= innerRadius * innerRadius && distSq <= outerRadius * outerRadius) {
                entity.damage(source, damage);
                hit.add(entity);
            }
        }

        return hit;
    }

    // -------------------------------------------------------------------------

    private static Box buildSearchBox(LivingEntity center, double range) {
        return new Box(
                center.getX() - range, center.getY() - range, center.getZ() - range,
                center.getX() + range, center.getY() + range, center.getZ() + range
        );
    }
}
