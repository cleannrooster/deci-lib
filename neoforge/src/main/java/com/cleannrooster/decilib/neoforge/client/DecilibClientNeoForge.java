package com.cleannrooster.decilib.neoforge.client;

import com.cleannrooster.decilib.Decilib;
import com.cleannrooster.decilib.builder.entity.DataDrivenMob;
import com.cleannrooster.decilib.builder.visual.Form;
import com.cleannrooster.decilib.client.model.*;
import com.cleannrooster.decilib.client.render.AzurelibMobRenderer;
import com.cleannrooster.decilib.client.render.RendererResolver;
import com.cleannrooster.decilib.entity.ModEntities;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = Decilib.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class DecilibClientNeoForge {

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(HumanoidMobModel.LAYER,    HumanoidMobModel::getTexturedModelData);
        event.registerLayerDefinition(QuadrupedMobModel.LAYER,   QuadrupedMobModel::getTexturedModelData);
        event.registerLayerDefinition(ApexPredatorModel.LAYER,   ApexPredatorModel::getTexturedModelData);
        event.registerLayerDefinition(BurrowerMobModel.LAYER,    BurrowerMobModel::getTexturedModelData);
        event.registerLayerDefinition(SmallEntityMobModel.LAYER, SmallEntityMobModel::getTexturedModelData);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ModEntities.getDataDrivenMobs().forEach((id, type) -> {
            var def = ModEntities.getDefinition(id);
            event.registerEntityRenderer(type, ctx -> resolveRenderer(ctx, def));
        });
    }

    private static EntityRenderer<DataDrivenMob> resolveRenderer(
            EntityRendererFactory.Context ctx,
            com.cleannrooster.decilib.builder.MobDefinition def) {
        if (def.form() == Form.AZURELIB) {
            return new AzurelibMobRenderer(ctx);
        }
        return RendererResolver.resolve(def.form()).create(ctx);
    }
}
