package com.cleannrooster.decilib.builder.entity;

import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.common.ChargeReleaseBrainGoal;
import com.cleannrooster.decilib.ai.util.DamageUtil;
import com.cleannrooster.decilib.builder.MobProfile;
import com.cleannrooster.decilib.builder.MobState;
import com.cleannrooster.decilib.builder.feature.BehaviorComposer;
import com.cleannrooster.decilib.builder.feature.BehaviorFeature;
import com.cleannrooster.decilib.builder.feature.FeatureConfig;
import com.cleannrooster.decilib.builder.feature.FeatureHooks;
import com.cleannrooster.decilib.builder.feature.FeatureLifecycleEvent;
import com.cleannrooster.decilib.builder.tuning.TuningProfile;
import com.cleannrooster.decilib.builder.validation.ValidationResult;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Set;


public final class ChargeFeature implements BehaviorFeature {

    private static final String DEFAULT_STATE        = "APPROACHING";
    private static final String RELEASE_ARC          = "arc";
    private static final String RELEASE_RADIAL       = "radial";
    private static final String DEFAULT_CHARGE = "default_charge";

    @Override
    public String typeId() { return "charge"; }

    @Override
    public void validate(FeatureConfig config, MobProfile profile, ValidationResult result) {
        var windup = config.getInt("windup_ticks", 8);
        if (windup < 1) {
            result.error("Feature 'charge' in profile '" + profile.id()
                    + "': windup_ticks must be >= 1 (got " + windup + ")");
        }
        var range = config.getDouble("range", 4.5);
        if (range <= 0) {
            result.error("Feature 'charge' in profile '" + profile.id()
                    + "': range must be > 0 (got " + range + ")");
        }
        var releaseType = config.getString("release_type", RELEASE_ARC);
        if (!RELEASE_ARC.equals(releaseType) && !RELEASE_RADIAL.equals(releaseType)) {
            result.error("Feature 'charge' in profile '" + profile.id()
                    + "': release_type must be \"arc\" or \"radial\" (got \"" + releaseType + "\")");
        }
        var stateName = config.getString("state", DEFAULT_STATE);
        try {
            MobState.valueOf(stateName);
        } catch (IllegalArgumentException e) {
            result.error("Feature 'charge' in profile '" + profile.id()
                    + "': unknown state '" + stateName + "'");
        }
        if ("ambusher".equals(profile.archetype()) && DEFAULT_STATE.equals(stateName)) {
            result.warn("Feature 'charge' in profile '" + profile.id()
                    + "': ambusher archetype never enters APPROACHING — "
                    + "add \"state\": \"ACTIVE\" or the charge will never fire");
        }
    }

    @Override
    public void apply(BehaviorComposer composer, FeatureConfig config, TuningProfile tuning) {
        var abilityId   = config.getString("ability_id",    "charge");
        var cooldown    = config.getInt("cooldown_ticks",   60);
        var windup      = config.getInt("windup_ticks",     8);
        var release     = config.getInt("release_delay",     20);

        var releaseType  = config.getString("release_type",  RELEASE_ARC);
        var halfAngle    = (float) config.getDouble("half_angle_deg", 50.0);
        var range        = config.getDouble("range",         4.5);
        var coeff        = (float) config.getDouble("coeff", 1.2);
        var upwardsSpeed = (float) config.getDouble("upwards_speed", 0.05);
        var chargeSpeed  = config.getDouble("charge_speed",  2.0);
        var state        = MobState.valueOf(config.getString("state", "CHARGING"));
        var priority     = config.getInt("priority",         100);

        var radialRelease = RELEASE_RADIAL.equals(releaseType);
        var finalRange = (float) range;
        // Arc releases pass the actual half-angle so sweep_arc can size its geometry correctly;
        // radial releases have no arc so the angle is zero.
        float effectiveHalfAngle = radialRelease ? 0f : halfAngle;
        var onStart    = ParticleStyle.fromString(config.getString("on_start_particles",    "none"));
        var onComplete = ParticleStyle.fromString(config.getString("on_complete_particles", "smoke_ring"));
        var hooks = config.hooks();

        var goal =
                new ChargeReleaseBrainGoal.Builder<MobEntity>(abilityId, cooldown, windup, release)
                        .interruptCooldownTicks(windup)
                        .onWindupStart(e -> {
                            var target = e.getTarget();
                            if (target != null) {
                                e.getNavigation().stop();
                                var dir = target.getPos().subtract(e.getPos()).normalize();
                                e.lookAtEntity(target, 180, 180);
                                e.setVelocity(dir.x * chargeSpeed * 0.8, upwardsSpeed, dir.z * chargeSpeed * 0.8);
                            }
                            if (hooks != null) hooks.fireIfPresent(FeatureLifecycleEvent.WINDUP, e, null);
                        })
                        .onWindupEnd(e -> {
                            e.getNavigation().stop();
                            var target = e.getTarget();
                            if (target == null || !target.isAlive()) return;
                            float totalDmg = coeff * (float) e.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
                            if (radialRelease) {
                                DamageUtil.performRadialDamage(e, range, totalDmg,
                                        e.getDamageSources().mobAttack(e), (ServerWorld) e.getWorld());
                            } else {
                                DamageUtil.performArcDamage(e, null, halfAngle, range, totalDmg,
                                        e.getDamageSources().mobAttack(e), (ServerWorld) e.getWorld());
                            }
                            if (hooks != null) hooks.fireIfPresent(FeatureLifecycleEvent.RELEASE, e, (ServerWorld) e.getWorld());

                        })
                        .onRelease((e, w) -> {
                        })
                        .effects(buildChargeEffects(onStart, onComplete, finalRange, effectiveHalfAngle, hooks))
                        .build();

        var gate = composer.aiProfile().combinedGate();
        composer.addTransition(
                MobState.APPROACHING,
                ctx -> ctx.stimulus().hasTarget()
                        && !ctx.stimulus().targetInMeleeRange()
                        && ctx.cooldowns().isReady(abilityId)
                        && gate.test(ctx),
                state,
                null,
                15
        );
        composer.addGoal(goal, Set.of(state), priority);
        composer.addTransition(state, ctx -> true, MobState.APPROACHING, null, 5);
    }

    private static GoalEffects<MobEntity> buildChargeEffects(
            ParticleStyle onStart, ParticleStyle onComplete,
            float finalRange, float effectiveHalfAngle,
            FeatureHooks hooks) {
        GoalEffects<MobEntity> effects = GoalEffects.<MobEntity>builder()
                .onStart(   (mob, world) -> onStart.spawn(mob, world, finalRange, 0f))
                .onComplete((mob, world) -> onComplete.spawn(mob, world, finalRange, effectiveHalfAngle))
                .build();
        return hooks != null ? GoalEffects.compose(effects, hooks.toGoalEffects()) : effects;
    }
}

