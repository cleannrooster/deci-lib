package com.cleannrooster.decilib.client.render.mob;

import com.cleannrooster.decilib.builder.entity.DataDrivenMob;
import com.cleannrooster.decilib.client.animation.AnimationStyle;
import com.cleannrooster.decilib.client.model.SmallEntityMobModel;
import com.cleannrooster.decilib.client.render.HumanoidRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;

public class SmallQuadrupedRenderer extends HumanoidRenderer<DataDrivenMob> {

    public SmallQuadrupedRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, SmallEntityMobModel.LAYER, AnimationStyle.ERRATIC, 0.6f);
    }
}
