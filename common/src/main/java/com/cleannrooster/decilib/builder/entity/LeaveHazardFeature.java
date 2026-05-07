package com.cleannrooster.decilib.builder.entity;

import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.common.LeaveHazardBrainGoal;
import com.cleannrooster.decilib.builder.MobProfile;
import com.cleannrooster.decilib.builder.MobState;
import com.cleannrooster.decilib.builder.feature.BehaviorComposer;
import com.cleannrooster.decilib.builder.feature.BehaviorFeature;
import com.cleannrooster.decilib.builder.feature.FeatureConfig;
import com.cleannrooster.decilib.builder.tuning.TuningProfile;
import com.cleannrooster.decilib.builder.validation.ValidationResult;
import net.minecraft.entity.mob.MobEntity;

import java.util.Set;


public final class LeaveHazardFeature implements BehaviorFeature {

    @Override
    public String typeId() { return "leave_hazard"; }

    @Override
    public void validate(FeatureConfig config, MobProfile profile, ValidationResult result) {
        var id = config.getString("ability_id", "");
        if (id.isBlank()) {
            result.error("Feature 'leave_hazard' in profile '" + profile.id()
                    + "': 'ability_id' is required");
        }

        var cooldown = config.getInt("cooldown_ticks", 100);
        if (cooldown < 1) {
            result.error("Feature 'leave_hazard' in profile '" + profile.id()
                    + "': cooldown_ticks must be >= 1 (got " + cooldown + ")");
        }

        var checkWater = config.getBoolean("check_in_water", true);
        var checkLava  = config.getBoolean("check_in_lava",  true);
        var checkDmg   = config.getBoolean("check_sustained_damage", true);
        if (!checkWater && !checkLava && !checkDmg) {
            result.warn("Feature 'leave_hazard' in profile '" + profile.id()
                    + "': no hazard checks are enabled — goal will never trigger");
        }

        var conditionMode = config.getString("condition_mode", "any");
        if (!conditionMode.equalsIgnoreCase("any") && !conditionMode.equalsIgnoreCase("all")) {
            result.error("Feature 'leave_hazard' in profile '" + profile.id()
                    + "': condition_mode must be 'any' or 'all' (got '" + conditionMode + "')");
        }

        if (checkDmg) {
            var dmgThreshold = config.getDouble("sustained_damage_threshold", 5.0);
            if (dmgThreshold <= 0) {
                result.error("Feature 'leave_hazard' in profile '" + profile.id()
                        + "': sustained_damage_threshold must be > 0 (got " + dmgThreshold + ")");
            }
            var interactionTicks = config.getInt("sustained_damage_interaction_ticks", 60);
            if (interactionTicks < 1) {
                result.error("Feature 'leave_hazard' in profile '" + profile.id()
                        + "': sustained_damage_interaction_ticks must be >= 1 (got " + interactionTicks + ")");
            }
        }

        var escapeMode = config.getString("escape_mode", "teleport");
        if (!escapeMode.equalsIgnoreCase("teleport") && !escapeMode.equalsIgnoreCase("launch")) {
            result.error("Feature 'leave_hazard' in profile '" + profile.id()
                    + "': escape_mode must be 'teleport' or 'launch' (got '" + escapeMode + "')");
        }

        if (escapeMode.equalsIgnoreCase("teleport")) {
            var radius = config.getDouble("teleport_radius", 4.0);
            if (radius <= 0) {
                result.error("Feature 'leave_hazard' in profile '" + profile.id()
                        + "': teleport_radius must be > 0 (got " + radius + ")");
            }
            var minRadius = config.getDouble("teleport_min_radius", 1.5);
            if (minRadius < 0) {
                result.error("Feature 'leave_hazard' in profile '" + profile.id()
                        + "': teleport_min_radius must be >= 0 (got " + minRadius + ")");
            }
            var attempts = config.getInt("teleport_attempts", 16);
            if (attempts < 1) {
                result.error("Feature 'leave_hazard' in profile '" + profile.id()
                        + "': teleport_attempts must be >= 1 (got " + attempts + ")");
            }
        }

        if (escapeMode.equalsIgnoreCase("launch")) {
            var vSpeed = config.getDouble("launch_vertical_speed", 0.6);
            if (vSpeed < 0) {
                result.error("Feature 'leave_hazard' in profile '" + profile.id()
                        + "': launch_vertical_speed must be >= 0 (got " + vSpeed + ")");
            }
            var hSpeed = config.getDouble("launch_horizontal_speed", 0.5);
            if (hSpeed < 0) {
                result.error("Feature 'leave_hazard' in profile '" + profile.id()
                        + "': launch_horizontal_speed must be >= 0 (got " + hSpeed + ")");
            }
        }

        var stateName = config.getString("state", "APPROACHING");
        try {
            MobState.valueOf(stateName);
        } catch (IllegalArgumentException e) {
            result.error("Feature 'leave_hazard' in profile '" + profile.id()
                    + "': unknown state '" + stateName + "'");
        }
    }

    @Override
    public void apply(BehaviorComposer composer, FeatureConfig config, TuningProfile tuning) {
        var abilityId    = config.getString("ability_id",   "leave_hazard");
        var cooldown     = config.getInt("cooldown_ticks",  100);
        var checkWater   = config.getBoolean("check_in_water",          true);
        var checkLava    = config.getBoolean("check_in_lava",           true);
        var checkDmg     = config.getBoolean("check_sustained_damage",  true);
        var dmgThreshold = (float) config.getDouble("sustained_damage_threshold",        5.0);
        var dmgInteractionTicks = config.getInt("sustained_damage_interaction_ticks",    60);
        var priority     = config.getInt("priority",        90);
        var state        = MobState.valueOf(config.getString("state", "APPROACHING"));

        var conditionMode = config.getString("condition_mode", "any").equalsIgnoreCase("all")
                ? LeaveHazardBrainGoal.ConditionMode.ALL
                : LeaveHazardBrainGoal.ConditionMode.ANY;

        var escapeMode = config.getString("escape_mode", "teleport").equalsIgnoreCase("launch")
                ? LeaveHazardBrainGoal.EscapeMode.LAUNCH
                : LeaveHazardBrainGoal.EscapeMode.TELEPORT;

        var teleportRadius    = config.getDouble("teleport_radius",     4.0);
        var teleportMinRadius = config.getDouble("teleport_min_radius", 1.5);
        var teleportAttempts  = config.getInt("teleport_attempts",      16);
        var launchVSpeed      = config.getDouble("launch_vertical_speed",   0.6);
        var launchHSpeed      = config.getDouble("launch_horizontal_speed", 0.5);

        var onEscape = ParticleStyle.fromString(config.getString("on_escape_particles", "none"));
        var hooks    = config.hooks();

        GoalEffects<MobEntity> effects = GoalEffects.<MobEntity>builder()
                .onAction((mob, world) -> onEscape.spawn(mob, world, 0f, 0f))
                .build();
        if (hooks != null) effects = GoalEffects.compose(effects, hooks.toGoalEffects());

        var goal = new LeaveHazardBrainGoal<MobEntity>(
                abilityId, cooldown,
                checkWater, checkLava, checkDmg, dmgThreshold, dmgInteractionTicks, conditionMode,
                escapeMode,
                teleportRadius, teleportMinRadius, teleportAttempts,
                launchVSpeed, launchHSpeed)
                .withEffects(effects);

        composer.addGoal(goal, Set.of(state), priority);
    }
}
