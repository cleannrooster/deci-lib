package com.cleannrooster.decilib.builder.entity;

import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.common.AmbushAttackBrainGoal;
import com.cleannrooster.decilib.builder.MobProfile;
import com.cleannrooster.decilib.builder.MobState;
import com.cleannrooster.decilib.builder.feature.BehaviorComposer;
import com.cleannrooster.decilib.builder.feature.BehaviorFeature;
import com.cleannrooster.decilib.builder.feature.FeatureConfig;
import com.cleannrooster.decilib.builder.feature.FeatureHooks;
import com.cleannrooster.decilib.builder.tuning.TuningProfile;
import com.cleannrooster.decilib.builder.validation.ValidationResult;

import java.util.Set;


public final class AmbushAttackFeature implements BehaviorFeature {

    @Override
    public String typeId() { return "ambush_attack"; }

    @Override
    public void validate(FeatureConfig config, MobProfile profile, ValidationResult result) {
        var id = config.getString("ability_id", "");
        if (id.isBlank()) {
            result.error("Feature 'ambush_attack' in profile '" + profile.id()
                    + "': 'ability_id' is required");
        }

        var cooldown = config.getInt("cooldown_ticks", 120);
        if (cooldown <= 0) {
            result.error("Feature 'ambush_attack' in profile '" + profile.id()
                    + "': cooldown_ticks must be > 0 (got " + cooldown + ")");
        }

        var minCooldown = config.getInt("min_cooldown_ticks", 40);
        if (minCooldown < 0) {
            result.error("Feature 'ambush_attack' in profile '" + profile.id()
                    + "': min_cooldown_ticks must be >= 0 (got " + minCooldown + ")");
        }

        var duration = config.getInt("hidden_duration_ticks", 60);
        if (duration <= 0) {
            result.error("Feature 'ambush_attack' in profile '" + profile.id()
                    + "': hidden_duration_ticks must be > 0 (got " + duration + ")");
        }

        var radius = config.getDouble("strafe_radius", 4.0);
        if (radius <= 0) {
            result.error("Feature 'ambush_attack' in profile '" + profile.id()
                    + "': strafe_radius must be > 0 (got " + radius + ")");
        }

        var movement = config.getString("movement_mode", "approach");
        if (!movement.equals("approach") && !movement.equals("strafe")) {
            result.error("Feature 'ambush_attack' in profile '" + profile.id()
                    + "': movement_mode must be 'approach' or 'strafe' (got '" + movement + "')");
        }

        var surface = config.getString("surface_mode", "behind");
        if (!Set.of("behind", "front", "underneath", "random").contains(surface)) {
            result.error("Feature 'ambush_attack' in profile '" + profile.id()
                    + "': surface_mode must be 'behind', 'front', 'underneath', or 'random'"
                    + " (got '" + surface + "')");
        }
    }

    @Override
    public void apply(BehaviorComposer composer, FeatureConfig config, TuningProfile tuning) {
        var abilityId = config.getString("ability_id",           "ambush_attack");
        var cooldown  = config.getInt("cooldown_ticks",          120);
        var minCd     = config.getInt("min_cooldown_ticks",       40);
        var duration  = config.getInt("hidden_duration_ticks",    60);
        var radius    = config.getDouble("strafe_radius",          4.0);
        var priority  = config.getInt("priority",                  15);
        var burrow    = config.getBoolean("burrow_mode",          false);

        AmbushAttackBrainGoal.MovementMode movement = switch (
                config.getString("movement_mode", "approach").toLowerCase()) {
            case "strafe" -> AmbushAttackBrainGoal.MovementMode.STRAFE;
            default       -> AmbushAttackBrainGoal.MovementMode.APPROACH;
        };

        AmbushAttackBrainGoal.SurfaceMode surface = switch (
                config.getString("surface_mode", "behind").toLowerCase()) {
            case "front"      -> AmbushAttackBrainGoal.SurfaceMode.FRONT;
            case "underneath" -> AmbushAttackBrainGoal.SurfaceMode.UNDERNEATH;
            case "random"     -> AmbushAttackBrainGoal.SurfaceMode.RANDOM;
            default           -> AmbushAttackBrainGoal.SurfaceMode.BEHIND;
        };

        var hideVisuals  = config.getBoolean("hide_visuals",   true);
        var loopAnim     = config.getString("ambush_loop_anim", "");
        var onTick      = ParticleStyle.fromString(config.getString("on_tick_particles",      "none"));
        var onComplete  = ParticleStyle.fromString(config.getString("on_complete_particles",  "none"));
        var onInterrupt = ParticleStyle.fromString(config.getString("on_interrupt_particles", "none"));
        var hooks = config.hooks();

        GoalEffects<DataDrivenMob> effects = GoalEffects.<DataDrivenMob>builder()
                .onTick(     (mob, world) -> onTick.spawn(mob, world, 0f, 0f))
                .onComplete( (mob, world) -> onComplete.spawn(mob, world, 0f, 0f))
                .onInterrupt((mob, world) -> onInterrupt.spawn(mob, world, 0f, 0f))
                .build();
        if (hooks != null) effects = GoalEffects.compose(effects, hooks.toGoalEffects());

        var goal = new AmbushAttackBrainGoal<DataDrivenMob>(
                abilityId, cooldown, minCd, duration, movement, surface, radius, burrow)
                .hideVisuals(hideVisuals)
                .ambushLoopAnim(loopAnim)
                .withEffects(effects);

        // Enter ACTIVE from APPROACHING when the ability is off cooldown and target is at range.
        // Priority should be above the archetype's base approach (5) and charge (15) so the
        // ambush fires opportunistically when available, before falling back to a normal dash.
        composer.addOffensiveTransition(
                MobState.APPROACHING,
                ctx -> ctx.stimulus().hasTarget()
                        && !ctx.stimulus().targetInMeleeRange()
                        && ctx.cooldowns().isReady(abilityId),
                MobState.ACTIVE,
                null,
                priority
        );

        // Return to APPROACHING once the entity surfaces (selfIsHidden flips to false).
        // Low priority — the goal's isCommitted() already blocks preemption while hidden.
        composer.addTransition(
                MobState.ACTIVE,
                ctx -> !ctx.stimulus().selfIsHidden(),
                MobState.APPROACHING,
                null,
                5
        );

        composer.addGoal(goal, Set.of(MobState.ACTIVE), priority);
    }
}
