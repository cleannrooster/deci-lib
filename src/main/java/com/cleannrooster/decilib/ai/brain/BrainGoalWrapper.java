package com.cleannrooster.decilib.ai.brain;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.EnumSet;

public abstract class BrainGoalWrapper<E extends MobEntity> extends Goal {

    private final MobBrain<E> brain;
    private final E entity;

    protected BrainGoalWrapper(MobBrain<E> brain, E entity) {
        this.brain  = brain;
        this.entity = entity;
        setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.TARGET));
    }
    @Override
    public boolean canStart() {
        return entity.getWorld() instanceof ServerWorld;
    }
    @Override
    public boolean shouldContinue() {
        return entity.getWorld() instanceof ServerWorld;
    }
    @Override
    public void start() {
        ServerWorld world = serverWorld();
        if (world == null) return;
        brain.initialize(entity, world);
    }
    @Override
    public void tick() {
        ServerWorld world = serverWorld();
        if (world == null) return;
        brain.tick(entity, world);
    }
    @Override
    public void stop() {
        ServerWorld world = serverWorld();
        if (world == null) return;
        brain.teardown(entity, world);
    }
    private ServerWorld serverWorld() {
        World w = entity.getWorld();
        return w instanceof ServerWorld sw ? sw : null;
    }
}
