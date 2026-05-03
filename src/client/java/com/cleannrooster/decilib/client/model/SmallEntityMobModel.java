package com.cleannrooster.decilib.client.model;

import com.cleannrooster.decilib.client.animation.AnimationProfile;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;

public class SmallEntityMobModel<T extends LivingEntity> extends HumanoidMobModel<T> {

    public static final EntityModelLayer LAYER =
            new EntityModelLayer(Identifier.of("deci-lib", "small_mob"), "main");

    public SmallEntityMobModel(ModelPart root, AnimationProfile profile) {
        super(root, profile);
    }
    public float tickDelta;


    public void setTickDelta(float tickDelta) {
        this.tickDelta = tickDelta;
    }
    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();

        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create().uv(0, 12).cuboid(-3, 0, -2, 6, 8, 4),
                ModelTransform.pivot(0, 0, 0));

        body.addChild("head",
                ModelPartBuilder.create().uv(0, 0).cuboid(-3, -6, -3, 6, 6, 6),
                ModelTransform.pivot(0, 0, 0));

        body.addChild("right_arm",
                ModelPartBuilder.create().uv(24, 0).cuboid(-2, -1, -1, 3, 8, 3),
                ModelTransform.pivot(-4, 1, 0));

        body.addChild("left_arm",
                ModelPartBuilder.create().uv(24, 11).cuboid(-1, -1, -1, 3, 8, 3),
                ModelTransform.pivot(4, 1, 0));

        body.addChild("right_leg",
                ModelPartBuilder.create().uv(0, 24).cuboid(-1, 0, -1, 3, 8, 3),
                ModelTransform.pivot(-2, 8, 0));

        body.addChild("left_leg",
                ModelPartBuilder.create().uv(8, 24).cuboid(-1, 0, -1, 3, 8, 3),
                ModelTransform.pivot(2, 8, 0));

        return TexturedModelData.of(data, 32, 32);
    }
}
