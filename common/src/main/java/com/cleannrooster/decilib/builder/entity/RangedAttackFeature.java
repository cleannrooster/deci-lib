package com.cleannrooster.decilib.builder.entity;

import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.common.RangedAttackBrainGoal;
import com.cleannrooster.decilib.ai.projectile.RangedProjectileType;
import com.cleannrooster.decilib.builder.MobProfile;
import com.cleannrooster.decilib.builder.MobState;
import com.cleannrooster.decilib.builder.feature.BehaviorComposer;
import com.cleannrooster.decilib.builder.feature.BehaviorFeature;
import com.cleannrooster.decilib.builder.feature.FeatureConfig;
import com.cleannrooster.decilib.builder.tuning.TuningProfile;
import com.cleannrooster.decilib.builder.validation.ValidationResult;
import net.minecraft.entity.mob.MobEntity;

import java.util.Set;


public final class RangedAttackFeature implements BehaviorFeature {

    @Override
    public String typeId() { return "ranged_attack"; }

    @Override
    public void validate(FeatureConfig config, MobProfile profile, ValidationResult result) {
        var id = config.getString("ability_id", "");
        if (id.isBlank()) {
            result.error("Feature 'ranged_attack' in profile '" + profile.id()
                    + "': 'ability_id' is required");
        }

        var engageCd = config.getInt("engage_cooldown_ticks", 20);
        if (engageCd < 1) {
            result.error("Feature 'ranged_attack' in profile '" + profile.id()
                    + "': engage_cooldown_ticks must be >= 1 (got " + engageCd + ")");
        }

        var mode = config.getString("movement_mode", "basic");
        if (!Set.of("basic", "strafe", "kite").contains(mode.toLowerCase())) {
            result.error("Feature 'ranged_attack' in profile '" + profile.id()
                    + "': movement_mode must be 'basic', 'strafe', or 'kite' (got '" + mode + "')");
        }

        var windupTicks = config.getInt("windup_ticks", 15);
        if (windupTicks < 1) {
            result.error("Feature 'ranged_attack' in profile '" + profile.id()
                    + "': windup_ticks must be >= 1 (got " + windupTicks + ")");
        }

        var shotCd = config.getInt("shot_cooldown_ticks", 20);
        if (shotCd < 0) {
            result.error("Feature 'ranged_attack' in profile '" + profile.id()
                    + "': shot_cooldown_ticks must be >= 0 (got " + shotCd + ")");
        }

        var winddownTicks = config.getInt("winddown_ticks", 0);
        if (winddownTicks < 0) {
            result.error("Feature 'ranged_attack' in profile '" + profile.id()
                    + "': winddown_ticks must be >= 0 (got " + winddownTicks + ")");
        }

        var magSize = config.getInt("mag_size", -1);
        if (magSize == 0) {
            result.error("Feature 'ranged_attack' in profile '" + profile.id()
                    + "': mag_size must be > 0 for finite magazines, or <= 0 for infinite (got 0)");
        }

        if (magSize > 0) {
            var reloadDuration = config.getInt("reload_duration_ticks", 60);
            if (reloadDuration < 1) {
                result.error("Feature 'ranged_attack' in profile '" + profile.id()
                        + "': reload_duration_ticks must be >= 1 (got " + reloadDuration + ")");
            }
        }

        var projectileSpeed = config.getDouble("projectile_speed", 1.6);
        if (projectileSpeed <= 0) {
            result.error("Feature 'ranged_attack' in profile '" + profile.id()
                    + "': projectile_speed must be > 0 (got " + projectileSpeed + ")");
        }

        if ("strafe".equalsIgnoreCase(mode)) {
            var preferredDist = config.getDouble("preferred_distance", 8.0);
            if (preferredDist <= 0) {
                result.error("Feature 'ranged_attack' in profile '" + profile.id()
                        + "': preferred_distance must be > 0 (got " + preferredDist + ")");
            }
            var retreatRange = config.getDouble("retreat_range", 4.0);
            if (retreatRange < 0) {
                result.error("Feature 'ranged_attack' in profile '" + profile.id()
                        + "': retreat_range must be >= 0 (got " + retreatRange + ")");
            }
        }

        if ("kite".equalsIgnoreCase(mode)) {
            var minKiteDist = config.getDouble("min_kite_distance", 8.0);
            if (minKiteDist <= 0) {
                result.error("Feature 'ranged_attack' in profile '" + profile.id()
                        + "': min_kite_distance must be > 0 (got " + minKiteDist + ")");
            }
            var maxKiteDist = config.getDouble("max_kite_distance", minKiteDist * 2.0);
            if (maxKiteDist <= minKiteDist) {
                result.error("Feature 'ranged_attack' in profile '" + profile.id()
                        + "': max_kite_distance must be > min_kite_distance (got " + maxKiteDist + ")");
            }
        }

        var stateName = config.getString("state", "APPROACHING");
        try {
            MobState.valueOf(stateName);
        } catch (IllegalArgumentException e) {
            result.error("Feature 'ranged_attack' in profile '" + profile.id()
                    + "': unknown state '" + stateName + "'");
        }
    }

    @Override
    public void apply(BehaviorComposer composer, FeatureConfig config, TuningProfile tuning) {
        var abilityId           = config.getString("ability_id",          "ranged_attack");
        var engageCooldownTicks = config.getInt("engage_cooldown_ticks",  20);
        var windupTicks         = config.getInt("windup_ticks",           15);
        var shotCooldownTicks   = config.getInt("shot_cooldown_ticks",    20);
        var magSize             = config.getInt("mag_size",               -1);
        var reloadDurationTicks = config.getInt("reload_duration_ticks",  60);
        var reloadMoveSpeed     = config.getDouble("reload_move_speed",   -1.0);
        var winddownTicks       = config.getInt("winddown_ticks",         0);
        var winddownMoveSpeed   = config.getDouble("winddown_movespeed",  0.0);
        var projectileSpeed     = (float) config.getDouble("projectile_speed",      1.6);
        var projectileDivergence = (float) config.getDouble("projectile_divergence", 1.0);
        var state               = MobState.valueOf(config.getString("state",         "APPROACHING"));
        var priority            = config.getInt("priority",               10);

        var projectileType = RangedProjectileType.fromString(
                config.getString("projectile_type", "arrow"));

        var mode = switch (config.getString("movement_mode", "basic").toLowerCase()) {
            case "strafe" -> RangedAttackBrainGoal.Mode.STRAFE;
            case "kite"   -> RangedAttackBrainGoal.Mode.KITE;
            default       -> RangedAttackBrainGoal.Mode.BASIC;
        };

        // Mode-specific movement
        var approachSpeed      = config.getDouble("approach_speed",       0.0);
        var preferredDistance  = config.getDouble("preferred_distance",   8.0);
        var strafeSpeed        = config.getDouble("strafe_speed",         1.2);
        var retreatRange       = config.getDouble("retreat_range",        4.0);
        var retreatSpeed       = config.getDouble("retreat_speed",        1.5);
        var strafeFlipInterval = config.getInt("strafe_flip_interval",    60);
        var minKiteDistance    = config.getDouble("min_kite_distance",    8.0);
        var maxKiteDistance    = config.getDouble("max_kite_distance",    minKiteDistance * 2.0);
        var kiteSpeed          = config.getDouble("kite_speed",           1.4);

        // Animations
        var windupAnim         = config.getString("windup_anim",          "");
        var fireAnim           = config.getString("fire_anim",            "");
        var reloadStartAnim    = config.getString("reload_start_anim",    "");
        var reloadCompleteAnim = config.getString("reload_complete_anim", "");

        // Particle effects
        var onFireParticles           = ParticleStyle.fromString(config.getString("on_fire_particles",           "none"));
        var onWindupParticles         = ParticleStyle.fromString(config.getString("on_windup_particles",         "none"));
        var onReloadCompleteParticles = ParticleStyle.fromString(config.getString("on_reload_complete_particles","none"));

        var hooks = config.hooks();

        GoalEffects<MobEntity> effects = GoalEffects.<MobEntity>builder()
                .onAction((mob, world) -> onFireParticles.spawn(mob, world, 0f, 0f))
                .build();
        if (hooks != null) effects = GoalEffects.compose(effects, hooks.toGoalEffects());

        var goal = new RangedAttackBrainGoal<MobEntity>(
                abilityId, engageCooldownTicks, mode,
                projectileType, projectileSpeed, projectileDivergence,
                windupTicks, shotCooldownTicks, magSize, reloadDurationTicks, reloadMoveSpeed,
                winddownTicks, winddownMoveSpeed,
                approachSpeed,
                preferredDistance, strafeSpeed, retreatRange, retreatSpeed, strafeFlipInterval,
                minKiteDistance, maxKiteDistance, kiteSpeed,
                windupAnim, fireAnim, reloadStartAnim, reloadCompleteAnim)
                .withEffects(effects)
                .onWindupStart((mob, world) -> onWindupParticles.spawn(mob, world, 0f, 0f))
                .onReloadComplete((mob, world) -> onReloadCompleteParticles.spawn(mob, world, 0f, 0f));

        // Phase 2 TODO: RangedAttackFeature registers only a goal (no entry transition of its own).
        // The AiProfile combinedGate cannot be applied here in Phase 1.
        // Add a dedicated entry transition or gate inside RangedAttackBrainGoal.canStart().
        composer.addGoal(goal, Set.of(state), priority);
    }
}
