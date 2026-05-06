package com.cleannrooster.decilib.ai.goal.common;

import com.cleannrooster.decilib.ai.cooldown.AbilityCooldownRegistry;
import com.cleannrooster.decilib.ai.cooldown.ReadOnlyCooldownRegistry;
import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.MobBrainGoal;
import com.cleannrooster.decilib.ai.goal.StopReason;
import com.cleannrooster.decilib.ai.projectile.RangedProjectileType;
import com.cleannrooster.decilib.ai.stimulus.AIStimulus;
import com.cleannrooster.decilib.builder.animation.MobAnimationDispatcherRegistry;
import com.cleannrooster.decilib.builder.entity.DataDrivenMob;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.BiConsumer;

public class RangedAttackBrainGoal<E extends MobEntity> implements MobBrainGoal<E> {

    public enum Mode { BASIC, STRAFE, KITE }

    private enum Phase { IDLE, WINDING_UP, WINDING_DOWN, RELOADING }

    // ── Config ────────────────────────────────────────────────────────────────

    private final String              abilityId;
    private final int                 engageCooldownTicks;
    private final Mode                mode;
    private final RangedProjectileType projectileType;
    private final float               projectileSpeed;
    private final float               projectileDivergence;
    private final int                 windupTicks;
    private final int                 shotCooldownTicks;
    private final int                 magSize;
    private final int                 reloadDurationTicks;
    private final double              reloadMoveSpeed;
    private final int                 winddownTicks;
    private final double              winddownMoveSpeed;

    // BASIC
    private final double approachSpeed;

    // STRAFE
    private final double preferredDistance;
    private final double strafeSpeed;
    private final double retreatRange;
    private final double retreatSpeed;
    private final int    strafeFlipInterval;

    // KITE
    private final double minKiteDistance;
    private final double maxKiteDistance;
    private final double kiteSpeed;

    // ── Animations ───────────────────────────────────────────────────────────

    @Nullable private final String windupAnim;
    @Nullable private final String fireAnim;
    @Nullable private final String reloadStartAnim;
    @Nullable private final String reloadCompleteAnim;

    // ── Callbacks ────────────────────────────────────────────────────────────

    @Nullable private GoalEffects<E>             effects;
    @Nullable private BiConsumer<E, ServerWorld> onWindupStart;
    @Nullable private BiConsumer<E, ServerWorld> onReloadStart;
    @Nullable private BiConsumer<E, ServerWorld> onReloadComplete;

    // ── Per-activation state ─────────────────────────────────────────────────

    private Phase phase;
    private int   windupCounter;
    private int   shotCooldownCounter;
    private int   reloadCounter;
    private int   winddownCounter;
    private int   ammoRemaining;
    private int   strafeFlipCounter;
    private int   strafeDirection;
    private boolean done;

    // ── Constructor ──────────────────────────────────────────────────────────

    public RangedAttackBrainGoal(String abilityId, int engageCooldownTicks, Mode mode,
                                  RangedProjectileType projectileType,
                                  float projectileSpeed, float projectileDivergence,
                                  int windupTicks, int shotCooldownTicks,
                                  int magSize, int reloadDurationTicks, double reloadMoveSpeed,
                                  int winddownTicks, double winddownMoveSpeed,
                                  double approachSpeed,
                                  double preferredDistance, double strafeSpeed,
                                  double retreatRange, double retreatSpeed, int strafeFlipInterval,
                                  double minKiteDistance, double maxKiteDistance, double kiteSpeed,
                                  @Nullable String windupAnim, @Nullable String fireAnim,
                                  @Nullable String reloadStartAnim, @Nullable String reloadCompleteAnim) {
        this.abilityId            = abilityId;
        this.engageCooldownTicks  = engageCooldownTicks;
        this.mode                 = mode;
        this.projectileType       = projectileType;
        this.projectileSpeed      = projectileSpeed;
        this.projectileDivergence = projectileDivergence;
        this.windupTicks          = windupTicks;
        this.shotCooldownTicks    = shotCooldownTicks;
        this.magSize              = magSize;
        this.reloadDurationTicks  = reloadDurationTicks;
        this.reloadMoveSpeed      = reloadMoveSpeed;
        this.winddownTicks        = winddownTicks;
        this.winddownMoveSpeed    = winddownMoveSpeed;
        this.approachSpeed        = approachSpeed;
        this.preferredDistance    = preferredDistance;
        this.strafeSpeed          = strafeSpeed;
        this.retreatRange         = retreatRange;
        this.retreatSpeed         = retreatSpeed;
        this.strafeFlipInterval   = strafeFlipInterval;
        this.minKiteDistance      = minKiteDistance;
        this.maxKiteDistance      = maxKiteDistance;
        this.kiteSpeed            = kiteSpeed;
        this.windupAnim           = blank(windupAnim);
        this.fireAnim             = blank(fireAnim);
        this.reloadStartAnim      = blank(reloadStartAnim);
        this.reloadCompleteAnim   = blank(reloadCompleteAnim);
    }

    // ── Fluent setters ───────────────────────────────────────────────────────

    public RangedAttackBrainGoal<E> withEffects(GoalEffects<E> effects) {
        this.effects = effects;
        return this;
    }

    public RangedAttackBrainGoal<E> onWindupStart(BiConsumer<E, ServerWorld> cb) {
        this.onWindupStart = cb;
        return this;
    }

    public RangedAttackBrainGoal<E> onReloadStart(BiConsumer<E, ServerWorld> cb) {
        this.onReloadStart = cb;
        return this;
    }

    public RangedAttackBrainGoal<E> onReloadComplete(BiConsumer<E, ServerWorld> cb) {
        this.onReloadComplete = cb;
        return this;
    }

    // ── MobBrainGoal ─────────────────────────────────────────────────────────

    @Override
    public boolean canStart(AIStimulus stimulus, ReadOnlyCooldownRegistry cooldowns) {
        return stimulus.hasTarget() && cooldowns.isReady(abilityId);
    }

    @Override
    public void reset(E entity) {
        phase               = Phase.IDLE;
        windupCounter       = 0;
        shotCooldownCounter = 0;
        reloadCounter       = 0;
        winddownCounter     = 0;
        ammoRemaining       = magSize > 0 ? magSize : -1;
        strafeFlipCounter   = 0;
        strafeDirection     = 1;
        done                = false;
    }

    @Override
    public void start(E entity, ServerWorld world) {
        reset(entity);
        if (effects != null && effects.onStart() != null) effects.onStart().accept(entity, world);
    }

    @Override
    public void tick(E entity, ServerWorld world, AIStimulus stimulus, AbilityCooldownRegistry cooldowns) {
        if (effects != null && effects.onTick() != null) effects.onTick().accept(entity, world);

        LivingEntity target = entity.getTarget();
        if (target == null || !target.isAlive()) {
            cooldowns.trigger(abilityId, engageCooldownTicks);
            if (effects != null && effects.onComplete() != null) effects.onComplete().accept(entity, world);
            done = true;
            return;
        }

        entity.getLookControl().lookAt(target, 30f, 30f);

        switch (phase) {
            case IDLE         -> tickIdle(entity, world, stimulus, target, cooldowns);
            case WINDING_UP   -> tickWindup(entity, world, stimulus, target, cooldowns);
            case WINDING_DOWN -> tickWinddown(entity, world, stimulus, target, cooldowns);
            case RELOADING    -> tickReload(entity, world, stimulus, target, cooldowns);
        }
    }

    @Override
    public boolean shouldContinue(E entity, AIStimulus stimulus) {
        return !done;
    }

    @Override
    public void stop(E entity, ServerWorld world) {
        if (!done && effects != null && effects.onInterrupt() != null) {
            effects.onInterrupt().accept(entity, world);
        }
    }

    @Override
    public void stop(E entity, ServerWorld world, StopReason reason) {
        if (reason != StopReason.SHUTDOWN && !done
                && effects != null && effects.onInterrupt() != null) {
            effects.onInterrupt().accept(entity, world);
        }
    }

    @Override
    public Set<String> declaredAbilityIds() { return Set.of(abilityId); }

    // ── Phase logic ──────────────────────────────────────────────────────────

    private void tickIdle(E entity, ServerWorld world, AIStimulus stimulus,
                           LivingEntity target, AbilityCooldownRegistry cooldowns) {
        applyMovement(entity, stimulus, target);

        if (shotCooldownCounter > 0) {
            shotCooldownCounter--;
            return;
        }

        // KITE: only start windup when inside the engagement envelope
        if (mode == Mode.KITE) {
            double dist = stimulus.targetDistance();
            if (dist < minKiteDistance || dist > maxKiteDistance) return;
        }

        phase = Phase.WINDING_UP;
        windupCounter = 0;
        triggerAnim(entity, windupAnim);
        if (onWindupStart != null) onWindupStart.accept(entity, world);
    }

    private void tickWindup(E entity, ServerWorld world, AIStimulus stimulus,
                             LivingEntity target, AbilityCooldownRegistry cooldowns) {
        if (mode == Mode.KITE) {
            double dist = stimulus.targetDistance();
            if (dist < minKiteDistance) {
                // Too close — flee and reset windup
                applyKiteMovement(entity, target);
                windupCounter = 0;
                return;
            }
            if (dist > maxKiteDistance) {
                // Too far — approach and reset windup
                entity.getNavigation().startMovingTo(target, kiteSpeed);
                windupCounter = 0;
                return;
            }
        }

        applyMovement(entity, stimulus, target);
        windupCounter++;

        if (windupCounter >= windupTicks) {
            fireProjectile(entity, world, cooldowns, target);
        }
    }

    private void tickReload(E entity, ServerWorld world, AIStimulus stimulus,
                             LivingEntity target, AbilityCooldownRegistry cooldowns) {
        applyReloadMovement(entity, stimulus, target);
        reloadCounter++;

        if (reloadCounter >= reloadDurationTicks) {
            ammoRemaining = magSize;
            triggerAnim(entity, reloadCompleteAnim);
            if (onReloadComplete != null) onReloadComplete.accept(entity, world);
            cooldowns.trigger(abilityId, engageCooldownTicks);
            if (effects != null && effects.onComplete() != null) effects.onComplete().accept(entity, world);
            done = true;
        }
    }

    private void tickWinddown(E entity, ServerWorld world, AIStimulus stimulus,
                               LivingEntity target, AbilityCooldownRegistry cooldowns) {
        if (winddownMoveSpeed < 0) {
            applyMovement(entity, stimulus, target);
        } else if (winddownMoveSpeed == 0) {
            entity.getNavigation().stop();
        } else {
            entity.getNavigation().startMovingTo(target, winddownMoveSpeed);
        }

        winddownCounter++;
        if (winddownCounter >= winddownTicks) {
            if (ammoRemaining == -1 && shouldExitAfterShot()) {
                cooldowns.trigger(abilityId, shotCooldownTicks);
                if (effects != null && effects.onComplete() != null) effects.onComplete().accept(entity, world);
                done = true;
            } else {
                shotCooldownCounter = shotCooldownTicks;
                phase = Phase.IDLE;
            }
        }
    }

    private void fireProjectile(E entity, ServerWorld world,
                                 AbilityCooldownRegistry cooldowns, LivingEntity target) {
        RangedProjectileType.aimAt(entity, target);
        projectileType.launch(entity, world, projectileSpeed, projectileDivergence);

        triggerAnim(entity, fireAnim);
        if (effects != null && effects.onAction() != null) effects.onAction().accept(entity, world);

        if (ammoRemaining > 0) ammoRemaining--;

        if (ammoRemaining == 0) {
            // Finite magazine exhausted — begin reload
            reloadCounter = 0;
            phase = Phase.RELOADING;
            triggerAnim(entity, reloadStartAnim);
            if (onReloadStart != null) onReloadStart.accept(entity, world);
        } else if (ammoRemaining == -1) {
            // Infinite ammo. BASIC mode exits the goal after each shot so other goals
            // (e.g. reposition) can interleave during the cooldown window.
            // KITE and STRAFE stay in-goal so their IDLE movement logic keeps running
            // and no other goal can walk the mob into the wrong position between shots.
            if (winddownTicks > 0) {
                winddownCounter = 0;
                phase = Phase.WINDING_DOWN;
            } else if (shouldExitAfterShot()) {
                cooldowns.trigger(abilityId, shotCooldownTicks);
                if (effects != null && effects.onComplete() != null) effects.onComplete().accept(entity, world);
                done = true;
            } else {
                shotCooldownCounter = shotCooldownTicks;
                phase = Phase.IDLE;
            }
        } else {
            // Finite ammo, shots remaining — stay in goal and wait for next shot
            if (winddownTicks > 0) {
                winddownCounter = 0;
                phase = Phase.WINDING_DOWN;
            } else {
                shotCooldownCounter = shotCooldownTicks;
                phase = Phase.IDLE;
            }
        }
    }

    // Only BASIC mode exits the goal after each infinite-ammo shot so that low-priority
    // interleaving goals (e.g. reposition) can run during the cooldown window.
    // KITE and STRAFE must remain active to keep their IDLE positioning logic running;
    // yielding would let an approach goal walk the mob into the wrong range.
    private boolean shouldExitAfterShot() { return mode == Mode.BASIC; }

    // ── Movement helpers ─────────────────────────────────────────────────────

    private void applyMovement(E entity, AIStimulus stimulus, LivingEntity target) {
        switch (mode) {
            case BASIC  -> applyBasicMovement(entity, target);
            case STRAFE -> applyStrafeMovement(entity, stimulus, target);
            case KITE   -> {
                double dist = stimulus.targetDistance();
                if (dist < minKiteDistance)      applyKiteMovement(entity, target);
                else if (dist > maxKiteDistance) entity.getNavigation().startMovingTo(target, kiteSpeed);
                else                             entity.getNavigation().stop();
            }
        }
    }

    private void applyBasicMovement(E entity, LivingEntity target) {
        if (approachSpeed > 0) {
            entity.getNavigation().startMovingTo(target, approachSpeed);
        } else {
            entity.getNavigation().stop();
        }
    }

    private void applyStrafeMovement(E entity, AIStimulus stimulus, LivingEntity target) {
        double dist = stimulus.targetDistance();

        if (dist < retreatRange) {
            Vec3d away = entity.getPos().subtract(target.getPos()).normalize();
            Vec3d dest = entity.getPos().add(away.multiply(4.0));
            entity.getNavigation().startMovingTo(dest.x, dest.y, dest.z, retreatSpeed);
            return;
        }

        strafeFlipCounter++;
        if (strafeFlipCounter >= strafeFlipInterval) {
            strafeDirection   = -strafeDirection;
            strafeFlipCounter = 0;
        }

        Vec3d toTarget = target.getPos().subtract(entity.getPos()).normalize();
        Vec3d perp     = new Vec3d(-toTarget.z, 0, toTarget.x).multiply(strafeDirection);

        Vec3d blended;
        if (dist > preferredDistance + 2.0) {
            blended = perp.add(toTarget).normalize();
        } else if (dist < preferredDistance - 2.0) {
            blended = perp.add(toTarget.negate()).normalize();
        } else {
            blended = perp;
        }

        Vec3d dest = entity.getPos().add(blended.multiply(3.0));
        entity.getNavigation().startMovingTo(dest.x, dest.y, dest.z, strafeSpeed);
    }

    private void applyKiteMovement(E entity, LivingEntity target) {
        Vec3d away = entity.getPos().subtract(target.getPos()).normalize();
        Vec3d dest = entity.getPos().add(away.multiply(4.0));
        entity.getNavigation().startMovingTo(dest.x, dest.y, dest.z, kiteSpeed);
    }

    private void applyReloadMovement(E entity, AIStimulus stimulus, LivingEntity target) {
        if (reloadMoveSpeed < 0) {
            applyMovement(entity, stimulus, target);
        } else if (reloadMoveSpeed == 0) {
            entity.getNavigation().stop();
        } else {
            entity.getNavigation().startMovingTo(target, reloadMoveSpeed);
        }
    }

    // ── Utility ──────────────────────────────────────────────────────────────

    private void triggerAnim(E entity, @Nullable String anim) {
        if (anim != null && entity instanceof DataDrivenMob ddm) {
            MobAnimationDispatcherRegistry.get().trigger(ddm, anim);
        }
    }

    @Nullable
    private static String blank(@Nullable String s) {
        return (s != null && !s.isBlank()) ? s : null;
    }
}
