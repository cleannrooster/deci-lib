package com.cleannrooster.decilib.client.render;

import com.cleannrooster.decilib.builder.entity.DataDrivenMob;
import com.cleannrooster.decilib.client.animation.AnimationStyle;
import com.cleannrooster.decilib.client.animation.AnimationProfile;
import com.cleannrooster.decilib.client.model.BurrowerMobModel;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class BurrowerRenderer<T extends DataDrivenMob>
        extends BaseMobRenderer<T, BurrowerMobModel<T>> {

    public BurrowerRenderer(EntityRendererFactory.Context ctx,
                             AnimationStyle style,
                             float scale) {
        super(ctx,
              new BurrowerMobModel<>(ctx.getPart(BurrowerMobModel.LAYER), AnimationProfile.of(style)),
              0.5f, scale);
    }

    @Override
    protected void updateAnimationState(T entity, float tickDelta) {
        getModel().animationState.update(entity, tickDelta);
    }

    @Override
    public Identifier getTexture(T entity) {
        return TextureTable.get(entity.getDefinition().form(), entity.getDefinition().theme());
    }

    @Override
    protected void setupTransforms(T entity, MatrixStack matrices, float animationProgress,
                                    float bodyYaw, float tickDelta, float scale) {
        super.setupTransforms(entity, matrices, animationProgress, bodyYaw, tickDelta, scale);
        if (entity.isEmerging()) {
            matrices.translate(0.0, -(double) (entity.getEmergeProgress() * 1.5f), 0.0);
        }
    }
}
