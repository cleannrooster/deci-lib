package com.cleannrooster.decilib.client.render;

import com.cleannrooster.decilib.builder.entity.DataDrivenMob;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;

public abstract class BaseMobRenderer<T extends DataDrivenMob, M extends SinglePartEntityModel<T>>
        extends MobEntityRenderer<T, M> {

    private final float scale;

    protected BaseMobRenderer(EntityRendererFactory.Context ctx, M model,
                               float shadowRadius, float scale) {
        super(ctx, model, shadowRadius);
        this.scale = scale;
    }

    protected abstract void updateAnimationState(T entity, float tickDelta);

    @Override
    public void render(T entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        updateAnimationState(entity, tickDelta);
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    protected void setupTransforms(T entity, MatrixStack matrices, float animationProgress,
                                   float bodyYaw, float tickDelta, float scale) {
        super.setupTransforms(entity, matrices, animationProgress, bodyYaw, tickDelta, scale);

    }
}
