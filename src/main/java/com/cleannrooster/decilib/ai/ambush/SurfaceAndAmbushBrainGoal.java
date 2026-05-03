package com.cleannrooster.decilib.ai.ambush;

import com.cleannrooster.decilib.ai.cooldown.AbilityCooldownRegistry;
import com.cleannrooster.decilib.ai.cooldown.ReadOnlyCooldownRegistry;
import com.cleannrooster.decilib.ai.goal.GoalEffects;
import com.cleannrooster.decilib.ai.goal.MobBrainGoal;
import com.cleannrooster.decilib.ai.goal.StopReason;
import com.cleannrooster.decilib.ai.stimulus.AIStimulus;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class SurfaceAndAmbushBrainGoal<E extends MobEntity & CanAmbush> implements MobBrainGoal<E> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SurfaceAndAmbushBrainGoal.class);
    static final String ABILITY_ID = "ambush_surface";

    @Nullable private GoalEffects<E> effects;

    private int surfaceCooldownTicks = 40;
    private boolean done;

    SurfaceAndAmbushBrainGoal() {}

    public SurfaceAndAmbushBrainGoal<E> withEffects(GoalEffects<E> effects) {
        this.effects = effects;
        return this;
    }

    public SurfaceAndAmbushBrainGoal<E> withSurfaceCooldownTicks(int ticks) {
        this.surfaceCooldownTicks = ticks;
        return this;
    }

    @Override
    public Set<String> declaredAbilityIds() {
        return Set.of(ABILITY_ID);
    }

    @Override
    public boolean isCommitted() {
        return !done;
    }

    @Override
    public boolean canStart(AIStimulus stimulus, ReadOnlyCooldownRegistry cooldowns) {
        return stimulus.selfIsHidden()
            && stimulus.hasTarget()
            && cooldowns.isReady(ABILITY_ID);
    }

    @Override
    public void reset(E entity) {
        done = false;
    }

    @Override
    public void start(E entity, ServerWorld world) {
        if (entity.isHidden()) {
            var pos = entity.getSurfacePosition();
            if (pos != null) {
                entity.teleport(pos.x, pos.y, pos.z, false);
            }
            entity.exitHiddenState();
            LOGGER.debug("[deci-lib] {} surfaced at {}", entity, entity.getPos());
            if (effects != null && effects.onStart() != null) effects.onStart().accept(entity, world);
        } else {
            LOGGER.debug("[deci-lib] {} surface goal started but entity already surfaced — skipping", entity);
            done = true;
        }
    }

    @Override
    public void tick(E entity, ServerWorld world, AIStimulus stimulus, AbilityCooldownRegistry cooldowns) {
        if (!done) {
            if (effects != null && effects.onTick() != null) effects.onTick().accept(entity, world);
            if (effects != null && effects.onComplete() != null) effects.onComplete().accept(entity, world);
            cooldowns.trigger(ABILITY_ID, surfaceCooldownTicks);
            LOGGER.debug("[deci-lib] {} ambush surface complete, cooldown={}t", entity, surfaceCooldownTicks);
            done = true;
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
        if (!done && effects != null && effects.onInterrupt() != null) {
            effects.onInterrupt().accept(entity, world);
        }
    }
}
