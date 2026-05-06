package com.cleannrooster.decilib.builder.entity;

import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.common.ChargeReleaseBrainGoal;
import com.cleannrooster.decilib.ai.util.DamageUtil;
import com.cleannrooster.decilib.builder.MobProfile;
import com.cleannrooster.decilib.builder.MobState;
import com.cleannrooster.decilib.builder.feature.BehaviorComposer;
import com.cleannrooster.decilib.builder.feature.BehaviorFeature;
import com.cleannrooster.decilib.builder.feature.FeatureConfig;
import com.cleannrooster.decilib.builder.feature.FeatureLifecycleEvent;
import com.cleannrooster.decilib.builder.tuning.TuningProfile;
import com.cleannrooster.decilib.builder.validation.ValidationResult;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Set;


public final class GapcloseFeature implements BehaviorFeature {

    @Override
    public String typeId() { return "gapclose"; }

    @Override
    public void validate(FeatureConfig config, MobProfile profile, ValidationResult result) {
        var cooldown = config.getInt("cooldown_ticks", 80);
        if (cooldown < 1) {
            result.error("Feature 'gapclose' in profile '" + profile.id()
                    + "': cooldown_ticks must be >= 1 (got " + cooldown + ")");
        }

        var windup = config.getInt("windup_ticks", 10);
        if (windup < 1) {
            result.error("Feature 'gapclose' in profile '" + profile.id()
                    + "': windup_ticks must be >= 1 (got " + windup + ")");
        }

        var minDist = config.getDouble("min_distance", 6.0);
        if (minDist <= 0) {
            result.error("Feature 'gapclose' in profile '" + profile.id()
                    + "': min_distance must be > 0 (got " + minDist + ")");
        }

        var stateName = config.getString("state", "APPROACHING");
        try {
            MobState.valueOf(stateName);
        } catch (IllegalArgumentException e) {
            result.error("Feature 'gapclose' in profile '" + profile.id()
                    + "': unknown state '" + stateName + "'");
        }
    }

    @Override
    public void apply(BehaviorComposer composer, FeatureConfig config, TuningProfile tuning) {
        var abilityId     = config.getString("ability_id",      "gapclose");
        var cooldown      = config.getInt("cooldown_ticks",     80);
        var windup        = config.getInt("windup_ticks",       10);
        var releaseDelay  = config.getInt("release_delay",      15);
        var leapSpeed     = config.getDouble("leap_speed",      1.8);
        var leapHeight    = config.getDouble("leap_height",     0.6);
        var teleportMode  = config.getBoolean("teleport_mode",  false);
        var landingCoeff  = (float) config.getDouble("landing_damage_coeff", 0.0);
        var landingRange  = config.getDouble("landing_range",   3.0);
        var minDist       = config.getDouble("min_distance",    6.0);
        var state         = MobState.valueOf(config.getString("state", "APPROACHING"));
        var priority      = config.getInt("priority",           25);
        var hooks         = config.hooks();

        var onStart    = ParticleStyle.fromString(config.getString("on_start_particles",    "none"));
        var onComplete = ParticleStyle.fromString(config.getString("on_complete_particles", "smoke_ring"));
        var finalRange = (float) landingRange;

        GoalEffects<MobEntity> effects = GoalEffects.<MobEntity>builder()
                .onStart(   (mob, world) -> onStart.spawn(mob, world, 0f, 0f))
                .onComplete((mob, world) -> onComplete.spawn(mob, world, finalRange, 0f))
                .build();
        if (hooks != null) effects = GoalEffects.compose(effects, hooks.toGoalEffects());

        var goal = new ChargeReleaseBrainGoal.Builder<MobEntity>(abilityId, cooldown, windup, releaseDelay)
                .interruptCooldownTicks(windup)
                .abortOnTargetLoss(true)
                .onWindupStart(entity -> {
                    entity.getNavigation().stop();
                    var target = entity.getTarget();
                    if (target != null) entity.lookAtEntity(target, 180f, 180f);
                    if (hooks != null) hooks.fireIfPresent(FeatureLifecycleEvent.WINDUP, entity, null);
                })
                .onWindupEnd(entity -> {
                    var target = entity.getTarget();
                    if (target == null) return;
                    if (teleportMode) {
                        var dest = target.getPos().subtract(
                                target.getRotationVector().normalize().multiply(1.5));
                        entity.teleport(dest.x, dest.y, dest.z, false);
                    } else {
                        var dir = target.getPos().subtract(entity.getPos()).normalize();
                        entity.setVelocity(dir.x * leapSpeed, leapHeight, dir.z * leapSpeed);
                    }
                    if (hooks != null) hooks.fireIfPresent(FeatureLifecycleEvent.RELEASE, entity, null);
                })
                .onRelease((entity, world) -> {
                    if (landingCoeff <= 0) return;
                    float totalDmg = landingCoeff
                            * (float) entity.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
                    DamageUtil.performRadialDamage(entity, landingRange, totalDmg,
                            world.getDamageSources().mobAttack(entity), (ServerWorld) entity.getWorld());
                })
                .effects(effects)
                .build();

        var gate = composer.aiProfile().combinedGate();
        composer.addTransition(
                MobState.APPROACHING,
                ctx -> ctx.stimulus().hasTarget()
                        && ctx.stimulus().targetDistance() > minDist
                        && ctx.cooldowns().isReady(abilityId)
                        && gate.test(ctx),
                state,
                null,
                priority
        );

        composer.addGoal(goal, Set.of(state), priority);
        composer.addTransition(state, ctx -> true, MobState.APPROACHING, null, 5);
    }
}
