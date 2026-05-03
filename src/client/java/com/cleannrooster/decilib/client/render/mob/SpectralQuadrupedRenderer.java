package com.cleannrooster.decilib.client.render.mob;

import com.cleannrooster.decilib.builder.entity.DataDrivenMob;
import com.cleannrooster.decilib.client.animation.AnimationProfile;
import com.cleannrooster.decilib.client.animation.AnimationStyle;
import com.cleannrooster.decilib.client.model.QuadrupedMobModel;
import com.cleannrooster.decilib.client.render.QuadrupedRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;

public class SpectralQuadrupedRenderer extends QuadrupedRenderer<DataDrivenMob,QuadrupedMobModel<DataDrivenMob>> {

    public SpectralQuadrupedRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new QuadrupedMobModel<>(ctx.getPart(QuadrupedMobModel.LAYER), AnimationProfile.agile()),1.0F);
    }
}
