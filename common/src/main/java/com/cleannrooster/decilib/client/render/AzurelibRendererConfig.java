package com.cleannrooster.decilib.client.render;

import com.cleannrooster.decilib.builder.entity.DataDrivenMob;
import com.cleannrooster.decilib.builder.visual.AzurelibRenderConfig;
import com.cleannrooster.decilib.client.animation.AzurelibMobAnimator;
import mod.azure.azurelib.common.render.entity.AzEntityRendererConfig;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.util.Identifier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class AzurelibRendererConfig {

    private AzurelibRendererConfig() {}

    public static AzEntityRendererConfig<DataDrivenMob> create() {
        return AzEntityRendererConfig.<DataDrivenMob>builder(
                AzurelibRendererConfig::resolveGeo,
                AzurelibRendererConfig::resolveTexture
        )
        .setAnimatorProvider(AzurelibMobAnimator::new)
                .setRenderType((mob, layer) ->
                RenderLayer.getEntityTranslucent(
                        Identifier.of(layer.getDefinition().renderConfig().texture())))
        .build();
    }

    private static Identifier resolveGeo(DataDrivenMob entity) {
        var rc = entity.getDefinition().renderConfig();
        if (rc == null) throw new IllegalStateException(
                "[deci-lib] Entity '" + entity.getDefinition().id()
                        + "' is using AzurelibMobRenderer but has no renderConfig.");
        return Identifier.of(rc.geo());
    }

    private static Identifier resolveTexture(DataDrivenMob entity) {
        var rc = entity.getDefinition().renderConfig();
        if (rc == null) throw new IllegalStateException(
                "[deci-lib] Entity '" + entity.getDefinition().id()
                        + "' is using AzurelibMobRenderer but has no renderConfig.");
        return Identifier.of(rc.texture());
    }
}
