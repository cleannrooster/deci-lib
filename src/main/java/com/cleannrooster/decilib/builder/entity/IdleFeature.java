package com.cleannrooster.decilib.builder.entity;

import com.cleannrooster.decilib.ai.goal.idle.IdleStillBrainGoal;
import com.cleannrooster.decilib.ai.goal.idle.IdleWanderBrainGoal;
import com.cleannrooster.decilib.builder.MobProfile;
import com.cleannrooster.decilib.builder.MobState;
import com.cleannrooster.decilib.builder.feature.BehaviorComposer;
import com.cleannrooster.decilib.builder.feature.BehaviorFeature;
import com.cleannrooster.decilib.builder.feature.FeatureConfig;
import com.cleannrooster.decilib.builder.tuning.TuningProfile;
import com.cleannrooster.decilib.builder.tuning.TuningResolver;
import com.cleannrooster.decilib.builder.validation.ValidationResult;
import net.minecraft.entity.mob.MobEntity;

import java.util.Set;


public final class IdleFeature implements BehaviorFeature {

    private static final String MODE_STILL  = "still";
    private static final String MODE_WANDER = "wander";
    private static final String MODE_MIXED  = "mixed";

    @Override
    public String typeId() { return "idle"; }

    @Override
    public void validate(FeatureConfig config, MobProfile profile, ValidationResult result) {
        var mode = config.getString("mode", MODE_STILL);
        if (!MODE_STILL.equals(mode) && !MODE_WANDER.equals(mode) && !MODE_MIXED.equals(mode)) {
            result.error("Feature 'idle' in profile '" + profile.id()
                    + "': mode must be \"still\", \"wander\", or \"mixed\" (got \"" + mode + "\")");
        }

        var scanInterval = config.getInt("scan_interval_ticks", 0);
        if (scanInterval < 0) {
            result.error("Feature 'idle' in profile '" + profile.id()
                    + "': scan_interval_ticks must be >= 0 (got " + scanInterval + ")");
        }

        var wanderRange = config.getDouble("wander_range", 8.0);
        if (!MODE_STILL.equals(mode) && wanderRange <= 0) {
            result.warn("Feature 'idle' in profile '" + profile.id()
                    + "': wander_range <= 0 with mode \"" + mode + "\" — will be treated as \"still\"");
        }

        var pauseTicks = config.getInt("pause_ticks", 40);
        if (pauseTicks < 10) {
            result.warn("Feature 'idle' in profile '" + profile.id()
                    + "': pause_ticks " + pauseTicks + " is very low — clamped to 10");
        }

        if ("ambusher".equals(profile.archetype()) && !MODE_STILL.equals(mode)) {
            var range = config.getDouble("wander_range", 8.0);
            if (range > 6.0) {
                result.warn("Feature 'idle' in profile '" + profile.id()
                        + "': ambusher with wander_range " + range
                        + " may drift far from its ambush point; consider still mode or a smaller range");
            }
        }
    }

    @Override
    public void apply(BehaviorComposer composer, FeatureConfig config, TuningProfile tuning) {
        var mode         = config.getString("mode", MODE_STILL);
        var scanInterval = Math.max(0, config.getInt("scan_interval_ticks", 0));
        var wanderRange  = config.getDouble("wander_range", 8.0);
        var pauseTicks   = Math.max(10, config.getInt("pause_ticks", 40));
        var priority     = config.getInt("priority", 2);

        var baseSpeed = TuningResolver.movementSpeed(tuning.speed());
        double idleSpeed = config.has("movement_speed")
                ? config.getDouble("movement_speed", 0.6F)
                : 0.6F;

        // Coerce to still if wander range is unusable
        if (!MODE_STILL.equals(mode) && wanderRange <= 0) {
            mode = MODE_STILL;
        }

        var installStill  = MODE_STILL.equals(mode)  || MODE_MIXED.equals(mode);
        var installWander = MODE_WANDER.equals(mode) || MODE_MIXED.equals(mode);

        if (installWander) {
            var wander = new IdleWanderBrainGoal<MobEntity>(wanderRange, idleSpeed, pauseTicks);
            if (scanInterval > 0) wander.withScan(scanInterval);
            // wander gets the configured priority; still (if mixed) gets one lower
            composer.addGoal(wander, Set.of(MobState.IDLE), priority);
        }

        if (installStill) {
            var still = new IdleStillBrainGoal<MobEntity>();
            if (scanInterval > 0) still.withScan(scanInterval);
            int stillPriority = installWander ? priority - 1 : priority;
            composer.addGoal(still, Set.of(MobState.IDLE), stillPriority);
        }
    }
}
