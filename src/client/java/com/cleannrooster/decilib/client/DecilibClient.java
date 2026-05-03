package com.cleannrooster.decilib.client;

import com.cleannrooster.decilib.builder.MobDefinition;
import com.cleannrooster.decilib.builder.entity.DataDrivenMob;
import com.cleannrooster.decilib.builder.visual.Form;
import com.cleannrooster.decilib.client.model.*;
import com.cleannrooster.decilib.client.render.AzurelibMobRenderer;
import com.cleannrooster.decilib.client.render.RendererResolver;
import com.cleannrooster.decilib.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;

public class DecilibClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(
                HumanoidMobModel.LAYER,    HumanoidMobModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(
                QuadrupedMobModel.LAYER,   QuadrupedMobModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(
                ApexPredatorModel.LAYER,   ApexPredatorModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(
                BurrowerMobModel.LAYER,    BurrowerMobModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(
                SmallEntityMobModel.LAYER, SmallEntityMobModel::getTexturedModelData);
        ModEntities.getDataDrivenMobs().forEach((id, type) -> {
            var def = ModEntities.getDefinition(id);
            EntityRendererRegistry.register(type, ctx -> resolveRenderer(ctx, def));
        });
    }

    private static EntityRenderer<DataDrivenMob> resolveRenderer(EntityRendererFactory.Context ctx, MobDefinition def) {
        if (def.form() == Form.AZURELIB) {
            return createAzurelibRenderer(ctx);
        }
        return RendererResolver.resolve(def.form()).create(ctx);
    }

    private static EntityRenderer<DataDrivenMob> createAzurelibRenderer(EntityRendererFactory.Context ctx) {
        return new AzurelibMobRenderer(ctx);
    }
}
