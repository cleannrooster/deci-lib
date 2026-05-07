package com.cleannrooster.decilib.ai.goal.common;

import com.cleannrooster.decilib.ai.cooldown.AbilityCooldownRegistry;
import com.cleannrooster.decilib.ai.cooldown.ReadOnlyCooldownRegistry;
import com.cleannrooster.decilib.ai.goal.MobBrainGoal;
import com.cleannrooster.decilib.ai.goal.StopReason;
import com.cleannrooster.decilib.ai.stimulus.AIStimulus;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;

public class SeekCoverBrainGoal<E extends MobEntity> implements MobBrainGoal<E> {

    private static final int  RESAMPLE_INTERVAL = 20;
    private static final int  SAMPLES           = 12;
    private static final double ARRIVAL_SQ      = 4.0; // 2-block arrival radius

    private final double searchRadius;
    private final double moveSpeed;

    @Nullable private Vec3d destination;
    private int ticksSinceLastSample;

    public SeekCoverBrainGoal(double searchRadius, double moveSpeed) {
        this.searchRadius = searchRadius;
        this.moveSpeed    = moveSpeed;
    }

    @Override
    public boolean canStart(AIStimulus stimulus, ReadOnlyCooldownRegistry cooldowns) {
        return stimulus.hasTarget();
    }

    @Override
    public void reset(E entity) {
        destination          = null;
        ticksSinceLastSample = RESAMPLE_INTERVAL; // force resample on next start
    }

    @Override
    public void start(E entity, ServerWorld world) {
        ticksSinceLastSample = RESAMPLE_INTERVAL;
        resampleAndNavigate(entity, world);
    }

    @Override
    public void tick(E entity, ServerWorld world, AIStimulus stimulus, AbilityCooldownRegistry cooldowns) {
        ticksSinceLastSample++;
        boolean arrived = destination != null
                && entity.squaredDistanceTo(destination) < ARRIVAL_SQ;
        if (arrived || ticksSinceLastSample >= RESAMPLE_INTERVAL) {
            resampleAndNavigate(entity, world);
        }
    }

    @Override
    public boolean shouldContinue(E entity, AIStimulus stimulus) {
        return stimulus.hasTarget() && stimulus.hasLineOfSight();
    }

    @Override
    public void stop(E entity, ServerWorld world) {
        entity.getNavigation().stop();
    }

    @Override
    public void stop(E entity, ServerWorld world, StopReason reason) {
        entity.getNavigation().stop();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void resampleAndNavigate(E entity, ServerWorld world) {
        destination          = findCoverPosition(entity, world);
        ticksSinceLastSample = 0;
        if (destination != null) {
            entity.getNavigation().startMovingTo(
                    destination.x, destination.y, destination.z, moveSpeed);
        }
    }

    private @Nullable Vec3d findCoverPosition(E entity, ServerWorld world) {
        LivingEntity threat = entity.getTarget();
        if (threat == null) return null;

        Vec3d delta = entity.getPos().subtract(threat.getPos());
        double len  = delta.length();
        Vec3d awayDir = len > 0.01 ? delta.normalize() : new Vec3d(1, 0, 0);
        double awayAngle = Math.atan2(awayDir.z, awayDir.x);

        Vec3d bestPos   = null;
        double bestSqDist = Double.MAX_VALUE;

        for (int i = 0; i < SAMPLES; i++) {
            // Bias within ±120° of the directly-away direction
            double angleOffset = (world.random.nextDouble() - 0.5) * (4.0 * Math.PI / 3.0);
            double angle       = awayAngle + angleOffset;
            double dist        = searchRadius * (0.4 + 0.6 * world.random.nextDouble());

            Vec3d candidate = new Vec3d(
                    entity.getX() + Math.cos(angle) * dist,
                    entity.getY(),
                    entity.getZ() + Math.sin(angle) * dist
            );

            if (losBlocked(world, candidate.add(0, 1.62, 0), threat)) {
                double sqd = entity.getPos().squaredDistanceTo(candidate);
                if (sqd < bestSqDist) {
                    bestSqDist = sqd;
                    bestPos    = candidate;
                }
            }
        }

        // Fallback: no cover found — move directly away
        return bestPos != null
                ? bestPos
                : entity.getPos().add(awayDir.multiply(searchRadius * 0.5));
    }

    private boolean losBlocked(ServerWorld world, Vec3d from, LivingEntity target) {
        var ctx = new RaycastContext(
                from, target.getEyePos(),
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                ShapeContext.absent());
        return world.raycast(ctx).getType() != HitResult.Type.MISS;
    }
}
