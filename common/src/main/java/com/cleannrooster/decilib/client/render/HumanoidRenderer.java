package com.cleannrooster.decilib.client.render;

import com.cleannrooster.decilib.builder.entity.DataDrivenMob;
import com.cleannrooster.decilib.client.animation.AnimationStyle;
import com.cleannrooster.decilib.client.animation.AnimationProfile;
import com.cleannrooster.decilib.client.model.HumanoidMobModel;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class HumanoidRenderer<T extends DataDrivenMob>
        extends BaseMobRenderer<T, HumanoidMobModel<T>> {

    public HumanoidRenderer(EntityRendererFactory.Context ctx,
                              EntityModelLayer layer,
                              AnimationStyle style,
                              float scale) {
        super(ctx,
              new HumanoidMobModel<>(ctx.getPart(layer), AnimationProfile.of(style)),
              0.5f, scale);
    }

    @Override
    public void render(T entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        this.model.setTickDelta(tickDelta);
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    protected void updateAnimationState(T entity, float tickDelta) {
        getModel().animationState.update(entity, tickDelta);
    }

    @Override
    public Identifier getTexture(T entity) {
        return TextureTable.get(entity.getDefinition().form(), entity.getDefinition().theme());
    }
}
