package com.cleannrooster.decilib.client.render;

import com.cleannrooster.decilib.builder.entity.DataDrivenMob;
import mod.azure.azurelib.common.render.entity.AzEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;

public class AzurelibMobRenderer extends AzEntityRenderer<DataDrivenMob> {

    public AzurelibMobRenderer(EntityRendererFactory.Context ctx) {
        super(AzurelibRendererConfig.create(), ctx);
    }
}
