package com.cleannrooster.decilib.builder.entity;

import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.common.FrenzyBrainGoal;
import com.cleannrooster.decilib.builder.MobProfile;
import com.cleannrooster.decilib.builder.MobState;
import com.cleannrooster.decilib.builder.feature.BehaviorComposer;
import com.cleannrooster.decilib.builder.feature.BehaviorFeature;
import com.cleannrooster.decilib.builder.feature.FeatureConfig;
import com.cleannrooster.decilib.builder.tuning.TuningProfile;
import com.cleannrooster.decilib.builder.validation.ValidationResult;
import net.minecraft.entity.mob.MobEntity;

import java.util.Set;


public final class FrenzyFeature implements BehaviorFeature {

    @Override
    public String typeId() { return "frenzy"; }

    @Override
    public void validate(FeatureConfig config, MobProfile profile, ValidationResult result) {
        var id = config.getString("ability_id", "");
        if (id.isBlank()) {
            result.error("Feature 'frenzy' in profile '" + profile.id()
                    + "': 'ability_id' is required");
        }

        var cooldown = config.getInt("cooldown_ticks", 100);
        if (cooldown < 1) {
            result.error("Feature 'frenzy' in profile '" + profile.id()
                    + "': cooldown_ticks must be >= 1 (got " + cooldown + ")");
        }

        var duration = config.getInt("duration_ticks", 60);
        if (duration < 1) {
            result.error("Feature 'frenzy' in profile '" + profile.id()
                    + "': duration_ticks must be >= 1 (got " + duration + ")");
        }

        var speed = config.getDouble("speed", 1.4);
        if (speed <= 0) {
            result.error("Feature 'frenzy' in profile '" + profile.id()
                    + "': speed must be > 0 (got " + speed + ")");
        }

        var movement = config.getString("movement_mode", "track");
        if (!Set.of("commit", "turn_then_commit", "track").contains(movement)) {
            result.error("Feature 'frenzy' in profile '" + profile.id()
                    + "': movement_mode must be 'commit', 'turn_then_commit', or 'track'"
                    + " (got '" + movement + "')");
        }

        var attackType = config.getString("attack_type", "arc");
        if (!attackType.equals("arc") && !attackType.equals("radial")) {
            result.error("Feature 'frenzy' in profile '" + profile.id()
                    + "': attack_type must be 'arc' or 'radial' (got '" + attackType + "')");
        }

        var interval = config.getInt("attack_interval_ticks", 10);
        if (interval < 1) {
            result.error("Feature 'frenzy' in profile '" + profile.id()
                    + "': attack_interval_ticks must be >= 1 (got " + interval + ")");
        }

        var range = config.getDouble("range", 3.0);
        if (range <= 0) {
            result.error("Feature 'frenzy' in profile '" + profile.id()
                    + "': range must be > 0 (got " + range + ")");
        }

        var halfAngle = config.getDouble("half_angle_deg", 60.0);
        if (halfAngle <= 0 || halfAngle >= 180) {
            result.error("Feature 'frenzy' in profile '" + profile.id()
                    + "': half_angle_deg must be in (0, 180) (got " + halfAngle + ")");
        }

        var stateName = config.getString("state", "ATTACKING_MELEE");
        try {
            MobState.valueOf(stateName);
        } catch (IllegalArgumentException e) {
            result.error("Feature 'frenzy' in profile '" + profile.id()
                    + "': unknown state '" + stateName + "'");
        }
    }

    @Override
    public void apply(BehaviorComposer composer, FeatureConfig config, TuningProfile tuning) {
        var abilityId = config.getString("ability_id",         "frenzy");
        var cooldown  = config.getInt("cooldown_ticks",        100);
        var duration  = config.getInt("duration_ticks",        60);
        var speed     = config.getDouble("speed",              1.4);
        var interval  = config.getInt("attack_interval_ticks", 10);
        var range     = config.getDouble("range",              3.0);
        var halfAngle = (float) config.getDouble("half_angle_deg", 60.0);
        var damage    = (float) config.getDouble("damage",     0.0);
        var coeff     = (float) config.getDouble("coeff",      1.0);
        var turnTicks = config.getInt("turn_ticks",            5);
        var priority  = config.getInt("priority",              20);
        var loopAnim  = config.getString("frenzy_loop_anim",   "");
        var state     = MobState.valueOf(config.getString("state", "ATTACKING_MELEE"));
        var hooks     = config.hooks();

        FrenzyBrainGoal.MovementMode movement = switch (
                config.getString("movement_mode", "track").toLowerCase()) {
            case "commit"           -> FrenzyBrainGoal.MovementMode.COMMIT;
            case "turn_then_commit" -> FrenzyBrainGoal.MovementMode.TURN_THEN_COMMIT;
            default                 -> FrenzyBrainGoal.MovementMode.TRACK;
        };

        FrenzyBrainGoal.AttackType attackType = switch (
                config.getString("attack_type", "arc").toLowerCase()) {
            case "radial" -> FrenzyBrainGoal.AttackType.RADIAL;
            default       -> FrenzyBrainGoal.AttackType.ARC;
        };

        var onTick      = ParticleStyle.fromString(config.getString("on_tick_particles",      "none"));
        var onComplete  = ParticleStyle.fromString(config.getString("on_complete_particles",  "none"));
        var onInterrupt = ParticleStyle.fromString(config.getString("on_interrupt_particles", "none"));
        var finalRange  = (float) range;

        GoalEffects<MobEntity> effects = GoalEffects.<MobEntity>builder()
                .onTick(     (mob, world) -> onTick.spawn(mob, world, finalRange, halfAngle))
                .onComplete( (mob, world) -> onComplete.spawn(mob, world, finalRange, halfAngle))
                .onInterrupt((mob, world) -> onInterrupt.spawn(mob, world, 0f, 0f))
                .build();
        if (hooks != null) effects = GoalEffects.compose(effects, hooks.toGoalEffects());

        var goal = new FrenzyBrainGoal<MobEntity>(
                abilityId, cooldown, duration, speed,
                movement, attackType, interval, range, halfAngle, damage, coeff, turnTicks)
                .frenzyLoopAnim(loopAnim)
                .withEffects(effects);

        var gate = composer.aiProfile().combinedGate();
        composer.addTransition(
                null,
                ctx -> ctx.stimulus().hasTarget()
                        && ctx.cooldowns().isReady(abilityId)
                        && gate.test(ctx),
                state,
                null,
                priority
        );

        composer.addGoal(goal, Set.of(state), priority);
    }
}
