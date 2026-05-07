package com.cleannrooster.decilib.client.render.mob;

import com.cleannrooster.decilib.builder.entity.DataDrivenMob;
import com.cleannrooster.decilib.client.animation.AnimationProfile;
import com.cleannrooster.decilib.client.animation.AnimationStyle;
import com.cleannrooster.decilib.client.model.ApexPredatorModel;
import com.cleannrooster.decilib.client.model.QuadrupedMobModel;
import com.cleannrooster.decilib.client.render.QuadrupedRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class HeavyQuadrupedRenderer extends QuadrupedRenderer<DataDrivenMob,ApexPredatorModel<DataDrivenMob>> {
    public HeavyQuadrupedRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new ApexPredatorModel<>(ctx.getPart(ApexPredatorModel.LAYER), AnimationProfile.heavy()), 1.2f);
    }

    @Override
    public Identifier getTexture(DataDrivenMob entity) {
        return super.getTexture(entity);
    }
}
