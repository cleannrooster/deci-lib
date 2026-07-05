package com.cleannrooster.decilib.builder.entity;

import com.cleannrooster.decilib.ai.ambush.AmbushConfig;
import com.cleannrooster.decilib.ai.ambush.AmbushModule;
import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.common.ApproachTargetBrainGoal;
import com.cleannrooster.decilib.ai.goal.common.MeleeAttackBrainGoal;
import com.cleannrooster.decilib.builder.MobProfile;
import com.cleannrooster.decilib.builder.MobState;
import com.cleannrooster.decilib.builder.feature.BehaviorComposer;
import com.cleannrooster.decilib.builder.feature.BehaviorFeature;
import com.cleannrooster.decilib.builder.feature.FeatureConfig;
import com.cleannrooster.decilib.builder.feature.FeatureHooks;
import com.cleannrooster.decilib.builder.tuning.TuningProfile;
import com.cleannrooster.decilib.builder.tuning.TuningResolver;
import com.cleannrooster.decilib.builder.validation.ValidationResult;

import java.util.Set;


public final class AmbushFeature implements BehaviorFeature {

    private static final String ABILITY_MELEE = "ambush_melee";

    @Override
    public String typeId() { return "ambush"; }

    @Override
    public void validate(FeatureConfig config, MobProfile profile, ValidationResult result) {
        var detectionRange = config.getDouble("detection_range", 16.0);
        var ambushRadius   = config.getDouble("ambush_radius",    6.0);

        if (detectionRange <= 0) {
            result.error("Feature 'ambush' in profile '" + profile.id()
                    + "': detection_range must be > 0 (got " + detectionRange + ")");
        }
        if (ambushRadius <= 0) {
            result.error("Feature 'ambush' in profile '" + profile.id()
                    + "': ambush_radius must be > 0 (got " + ambushRadius + ")");
        }
        var scanInterval = config.getInt("scan_interval_ticks", 10);
        if (scanInterval <= 0) {
            result.error("Feature 'ambush' in profile '" + profile.id()
                    + "': scan_interval_ticks must be > 0 (got " + scanInterval + ")");
        }
        if (ambushRadius > detectionRange) {
            result.warn("Feature 'ambush' in profile '" + profile.id()
                    + "': ambush_radius (" + ambushRadius
                    + ") > detection_range (" + detectionRange
                    + ") — the mob will surface before it detects the target;"
                    + " consider reducing ambush_radius");
        }
        if (!"ambusher".equals(profile.archetype())) {
            result.warn("Feature 'ambush' works best with the 'ambusher' archetype;"
                    + " profile '" + profile.id() + "' uses '" + profile.archetype() + "'");
        }
    }

    @Override
    public void apply(BehaviorComposer composer, FeatureConfig config, TuningProfile tuning) {
        var detectionRange  = config.getDouble("detection_range",       TuningResolver.followRange(tuning.detection()));
        var ambushRadius    = config.getDouble("ambush_radius",          6.0);
        var scanInterval    = config.getInt("scan_interval_ticks",       10);
        var rehideDelay     = config.getInt("rehide_delay_ticks",        60);
        var surfaceCooldown = config.getInt("surface_cooldown_ticks",    40);
        var approachSpeed   = TuningResolver.movementSpeed(tuning.speed()) / 0.28;

        var ambushConfig = AmbushConfig.<DataDrivenMob>builder()
                .detectionRange(detectionRange)
                .ambushRadius(ambushRadius)
                .scanInterval(scanInterval)
                .rehideDelay(rehideDelay)
                .scanner((entity, world, range) -> world.getClosestPlayer(entity, range))
                .build();

        var hiddenTick     = ParticleStyle.fromString(config.getString("on_hidden_tick_particles",     "none"));
        var rehideComplete = ParticleStyle.fromString(config.getString("on_rehide_complete_particles", "none"));
        var hooks = config.hooks();

        var module = new AmbushModule<DataDrivenMob>(ambushConfig);
        module.surfaceGoal().withSurfaceCooldownTicks(surfaceCooldown);

        // watchGoal: hidden-tick particles + animation hooks (START/COMPLETE/CANCEL)
        GoalEffects.Builder<DataDrivenMob> watchBuilder = GoalEffects.<DataDrivenMob>builder();
        if (hiddenTick != ParticleStyle.NONE)
            watchBuilder.onTick((mob, world) -> hiddenTick.spawn(mob, world, 0f, 0f));
        GoalEffects<DataDrivenMob> watchEffects = watchBuilder.build();
        if (hooks != null) watchEffects = GoalEffects.compose(watchEffects, hooks.toGoalEffects());
        module.watchGoal().withEffects(watchEffects);

        // enterHidingGoal: rehide-complete particles only
        if (rehideComplete != ParticleStyle.NONE) {
            module.enterHidingGoal().withEffects(GoalEffects.<DataDrivenMob>builder()
                    .onComplete((mob, world) -> rehideComplete.spawn(mob, world, 0f, 0f))
                    .build());
        }

        composer.addTransition(MobState.HIDDEN,
                module.hiddenToActiveCondition(),
                MobState.ACTIVE, null, 20);

        composer.addTransition(MobState.ACTIVE,
                module.activeToRehidingCondition(),
                MobState.REHIDING, null, 5);

        composer.addTransition(MobState.REHIDING,
                module.rehidingToHiddenCondition(),
                MobState.HIDDEN, null, 30);

        // Ambush lifecycle goals (surface at highest priority — see AmbushModule Javadoc)
        composer.addGoal(module.watchGoal(),       Set.of(MobState.HIDDEN),   10);
        composer.addGoal(module.surfaceGoal(),     Set.of(MobState.ACTIVE),  100);
        composer.addGoal(module.enterHidingGoal(), Set.of(MobState.REHIDING),  5);

        composer.addOffensiveGoal(new ApproachTargetBrainGoal<>(approachSpeed),
                Set.of(MobState.ACTIVE), 8);

        composer.addOffensiveGoal(new MeleeAttackBrainGoal<>(ABILITY_MELEE, 20),
                Set.of(MobState.ACTIVE), 10);
    }
}
