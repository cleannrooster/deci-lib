package com.cleannrooster.decilib.client.model;

import com.cleannrooster.decilib.client.animation.AnimationProfile;
import com.cleannrooster.decilib.builder.entity.DataDrivenMob;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class BurrowerMobModel<T extends DataDrivenMob> extends HumanoidMobModel<T> {

    public static final EntityModelLayer LAYER =
            new EntityModelLayer(Identifier.of(com.cleannrooster.decilib.Decilib.MOD_ID, "burrower_mob"), "main");

    public BurrowerMobModel(ModelPart root, AnimationProfile profile) {
        super(root, profile);
    }

    public static TexturedModelData getTexturedModelData() {
        return HumanoidMobModel.getTexturedModelData();
    }
}
