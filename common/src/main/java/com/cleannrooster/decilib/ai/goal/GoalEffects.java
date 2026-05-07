package com.cleannrooster.decilib.ai.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

public record GoalEffects<E extends LivingEntity>(
        @Nullable BiConsumer<E, ServerWorld> onStart,
        @Nullable BiConsumer<E, ServerWorld> onTick,
        @Nullable BiConsumer<E, ServerWorld> onAction,
        @Nullable BiConsumer<E, ServerWorld> onComplete,
        @Nullable BiConsumer<E, ServerWorld> onInterrupt
) {
    public static <E extends LivingEntity> Builder<E> builder() {
        return new Builder<>();
    }


    public static <E extends LivingEntity> GoalEffects<E> compose(
            GoalEffects<E> base, GoalEffects<E> overlay) {
        return new GoalEffects<>(
                chain(base.onStart(),     overlay.onStart()),
                chain(base.onTick(),      overlay.onTick()),
                chain(base.onAction(),    overlay.onAction()),
                chain(base.onComplete(),  overlay.onComplete()),
                chain(base.onInterrupt(), overlay.onInterrupt())
        );
    }

    private static <E extends LivingEntity> BiConsumer<E, ServerWorld> chain(
            BiConsumer<E, ServerWorld> a, BiConsumer<E, ServerWorld> b) {
        if (a == null) return b;
        if (b == null) return a;
        return (entity, world) -> { a.accept(entity, world); b.accept(entity, world); };
    }

    public static final class Builder<E extends LivingEntity> {
        private BiConsumer<E, ServerWorld> onStart;
        private BiConsumer<E, ServerWorld> onTick;
        private BiConsumer<E, ServerWorld> onAction;
        private BiConsumer<E, ServerWorld> onComplete;
        private BiConsumer<E, ServerWorld> onInterrupt;

        public Builder<E> onStart(BiConsumer<E, ServerWorld> fn)     { this.onStart     = fn; return this; }
        public Builder<E> onTick(BiConsumer<E, ServerWorld> fn)      { this.onTick      = fn; return this; }
        public Builder<E> onAction(BiConsumer<E, ServerWorld> fn)    { this.onAction    = fn; return this; }
        public Builder<E> onComplete(BiConsumer<E, ServerWorld> fn)  { this.onComplete  = fn; return this; }
        public Builder<E> onInterrupt(BiConsumer<E, ServerWorld> fn) { this.onInterrupt = fn; return this; }

        public GoalEffects<E> build() {
            return new GoalEffects<>(onStart, onTick, onAction, onComplete, onInterrupt);
        }
    }
}
