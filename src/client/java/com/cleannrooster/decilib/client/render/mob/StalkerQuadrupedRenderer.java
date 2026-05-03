package com.cleannrooster.decilib.client.render.mob;

import com.cleannrooster.decilib.builder.entity.DataDrivenMob;
import com.cleannrooster.decilib.client.animation.AnimationProfile;
import com.cleannrooster.decilib.client.animation.AnimationStyle;
import com.cleannrooster.decilib.client.model.QuadrupedMobModel;
import com.cleannrooster.decilib.client.render.BurrowerRenderer;
import com.cleannrooster.decilib.client.render.QuadrupedRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;

public class StalkerQuadrupedRenderer extends QuadrupedRenderer<DataDrivenMob, QuadrupedMobModel<DataDrivenMob>> {

    public StalkerQuadrupedRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new QuadrupedMobModel<>(ctx.getPart(QuadrupedMobModel.LAYER), AnimationProfile.erratic()),1.0F);
    }
}
