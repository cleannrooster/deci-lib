package com.cleannrooster.decilib.builder.entity;

import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.common.RepositionBrainGoal;
import com.cleannrooster.decilib.builder.MobProfile;
import com.cleannrooster.decilib.builder.MobState;
import com.cleannrooster.decilib.builder.feature.BehaviorComposer;
import com.cleannrooster.decilib.builder.feature.BehaviorFeature;
import com.cleannrooster.decilib.builder.feature.FeatureConfig;
import com.cleannrooster.decilib.builder.tuning.TuningProfile;
import com.cleannrooster.decilib.builder.validation.ValidationResult;
import net.minecraft.entity.mob.MobEntity;

import java.util.Set;


public final class RepositionFeature implements BehaviorFeature {

    @Override
    public String typeId() { return "reposition"; }

    @Override
    public void validate(FeatureConfig config, MobProfile profile, ValidationResult result) {
        var id = config.getString("ability_id", "");
        if (id.isBlank()) {
            result.error("Feature 'reposition' in profile '" + profile.id()
                    + "': 'ability_id' is required");
        }

        var cooldown = config.getInt("cooldown_ticks", 60);
        if (cooldown < 1) {
            result.error("Feature 'reposition' in profile '" + profile.id()
                    + "': cooldown_ticks must be >= 1 (got " + cooldown + ")");
        }

        var duration = config.getInt("duration_ticks", 20);
        if (duration < 1) {
            result.error("Feature 'reposition' in profile '" + profile.id()
                    + "': duration_ticks must be >= 1 (got " + duration + ")");
        }

        var radius = config.getDouble("radius", 6.0);
        if (radius <= 0) {
            result.error("Feature 'reposition' in profile '" + profile.id()
                    + "': radius must be > 0 (got " + radius + ")");
        }

        var minDist = config.getDouble("min_distance", 2.0);
        if (minDist < 0) {
            result.error("Feature 'reposition' in profile '" + profile.id()
                    + "': min_distance must be >= 0 (got " + minDist + ")");
        }
        if (minDist >= radius) {
            result.warn("Feature 'reposition' in profile '" + profile.id()
                    + "': min_distance >= radius — the reposition target will always be exactly"
                    + " at min_distance (which is clamped to radius)");
        }

        var arrivalRadius = config.getDouble("arrival_radius", 1.5);
        if (arrivalRadius <= 0) {
            result.error("Feature 'reposition' in profile '" + profile.id()
                    + "': arrival_radius must be > 0 (got " + arrivalRadius + ")");
        }

        var movement = config.getString("movement_mode", "pathfind");
        if (!movement.equalsIgnoreCase("pathfind") && !movement.equalsIgnoreCase("dash")) {
            result.error("Feature 'reposition' in profile '" + profile.id()
                    + "': movement_mode must be 'pathfind' or 'dash' (got '" + movement + "')");
        }

        var speedMode = config.getString("speed_mode", "absolute");
        if (!speedMode.equalsIgnoreCase("absolute") && !speedMode.equalsIgnoreCase("dynamic")) {
            result.error("Feature 'reposition' in profile '" + profile.id()
                    + "': speed_mode must be 'absolute' or 'dynamic' (got '" + speedMode + "')");
        }

        if (speedMode.equalsIgnoreCase("absolute")) {
            var speed = config.getDouble("speed", 1.8);
            if (speed <= 0) {
                result.error("Feature 'reposition' in profile '" + profile.id()
                        + "': speed must be > 0 (got " + speed + ")");
            }
        }

        var anchor = config.getString("anchor", "self");
        if (!anchor.equalsIgnoreCase("self") && !anchor.equalsIgnoreCase("target")) {
            result.error("Feature 'reposition' in profile '" + profile.id()
                    + "': anchor must be 'self' or 'target' (got '" + anchor + "')");
        }

        var stateName = config.getString("state", "APPROACHING");
        try {
            MobState.valueOf(stateName);
        } catch (IllegalArgumentException e) {
            result.error("Feature 'reposition' in profile '" + profile.id()
                    + "': unknown state '" + stateName + "'");
        }
    }

    @Override
    public void apply(BehaviorComposer composer, FeatureConfig config, TuningProfile tuning) {
        var abilityId     = config.getString("ability_id",    "reposition");
        var cooldown      = config.getInt("cooldown_ticks",   60);
        var duration      = config.getInt("duration_ticks",   20);
        var radius        = config.getDouble("radius",        6.0);
        var minDist       = config.getDouble("min_distance",  2.0);
        var arrivalRadius = config.getDouble("arrival_radius",1.5);
        var speed         = config.getDouble("speed",         1.8);
        var priority      = config.getInt("priority",         15);
        var state         = MobState.valueOf(config.getString("state", "APPROACHING"));

        var movement = switch (config.getString("movement_mode", "pathfind").toLowerCase()) {
            case "dash" -> RepositionBrainGoal.Movement.DASH;
            default     -> RepositionBrainGoal.Movement.PATHFIND;
        };

        var speedMode = switch (config.getString("speed_mode", "absolute").toLowerCase()) {
            case "dynamic" -> RepositionBrainGoal.SpeedMode.DYNAMIC;
            default        -> RepositionBrainGoal.SpeedMode.ABSOLUTE;
        };

        var anchor = switch (config.getString("anchor", "self").toLowerCase()) {
            case "target" -> RepositionBrainGoal.Anchor.TARGET;
            default       -> RepositionBrainGoal.Anchor.SELF;
        };

        var onStart     = ParticleStyle.fromString(config.getString("on_start_particles",     "none"));
        var onComplete  = ParticleStyle.fromString(config.getString("on_complete_particles",  "none"));
        var onInterrupt = ParticleStyle.fromString(config.getString("on_interrupt_particles", "none"));

        var hooks = config.hooks();

        GoalEffects<MobEntity> effects = GoalEffects.<MobEntity>builder()
                .onStart(    (mob, world) -> onStart.spawn(mob, world, 0f, 0f))
                .onComplete( (mob, world) -> onComplete.spawn(mob, world, 0f, 0f))
                .onInterrupt((mob, world) -> onInterrupt.spawn(mob, world, 0f, 0f))
                .build();
        if (hooks != null) effects = GoalEffects.compose(effects, hooks.toGoalEffects());

        var goal = new RepositionBrainGoal<MobEntity>(
                abilityId, cooldown, duration,
                radius, minDist, arrivalRadius,
                movement, speedMode, speed, anchor)
                .withEffects(effects);

        composer.addGoal(goal, Set.of(state), priority);
    }
}
