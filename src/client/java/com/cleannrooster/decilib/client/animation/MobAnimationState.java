package com.cleannrooster.decilib.client.animation;

import com.cleannrooster.decilib.builder.entity.DataDrivenMob;

public final class MobAnimationState {

    public boolean isHidden;
    public boolean isAttacking;
    public boolean isMoving;
    public boolean isEmerging;
    public float   emergeProgress;

    public void update(DataDrivenMob entity, float tickDelta) {
        isHidden      = entity.isHidden();
        isEmerging    = entity.isEmerging();
        emergeProgress = entity.getEmergeProgress();
        isMoving      = entity.getVelocity().horizontalLength() > 0.01;
        isAttacking   = entity.handSwinging;
    }
}
