package com.cleannrooster.decilib.client.render.mob;

import com.cleannrooster.decilib.builder.entity.DataDrivenMob;
import com.cleannrooster.decilib.client.animation.AnimationStyle;
import com.cleannrooster.decilib.client.model.HumanoidMobModel;
import com.cleannrooster.decilib.client.render.HumanoidRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;

public class AgileBipedRenderer extends HumanoidRenderer<DataDrivenMob> {

    public AgileBipedRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, HumanoidMobModel.LAYER, AnimationStyle.AGILE, 1.0f);
    }
}
