package com.cleannrooster.decilib.client.model;

import com.cleannrooster.decilib.builder.entity.DataDrivenMob;
import com.cleannrooster.decilib.client.animation.AnimationProfile;
import com.cleannrooster.decilib.client.animation.MobAnimationState;
import com.cleannrooster.decilib.client.animation.ProceduralAnimator;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.render.entity.model.WolfEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;


public class ApexPredatorModel<T extends DataDrivenMob> extends QuadrupedMobModel<T> {
	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart upperBody;
	private final ModelPart leg3;
	private final ModelPart tail;
	private final ModelPart leg2;
	private final ModelPart leg0;
	private final ModelPart leg1;
    public static final EntityModelLayer LAYER =
            new EntityModelLayer(Identifier.of("deci-lib", "apex-predator-mob"), "main");
    private final AnimationProfile animationProfile;
    private final ModelPart main;

    public ApexPredatorModel(ModelPart root, AnimationProfile animationProfile) {
        this.main = root.getChild("main");
        this.animationProfile = animationProfile;
        this.body = this.main.getChild("body");
        this.upperBody = this.main.getChild("upperBody");
        this.leg3 = this.main.getChild("leg3");
        this.tail = this.main.getChild("tail");
        this.leg0 = this.main.getChild("leg0");
        this.leg2 = this.main.getChild("leg2");
        this.leg1 = this.main.getChild("leg1");
        this.head = this.main.getChild("head");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData main = modelPartData.addChild("main", ModelPartBuilder.create(), ModelTransform.pivot(-1.125F, 23.7824F, 5.4607F));

        ModelPartData body = main.addChild("body", ModelPartBuilder.create(), ModelTransform.pivot(-0.5625F, -10.2824F, -3.4607F));

        ModelPartData body_r1 = body.addChild("body_r1", ModelPartBuilder.create().uv(8, 14).cuboid(-3.0F, -7.0F, -3.5F, 6.0F, 14.0F, 7.0F, new Dilation(0.0F)), ModelTransform.of(0.6875F, 0.2F, 0.0F, 1.3963F, 0.0F, 0.0F));

        ModelPartData upperBody = main.addChild("upperBody", ModelPartBuilder.create(), ModelTransform.of(-0.5625F, -9.2824F, -10.9607F, 1.5708F, 0.0F, 0.0F));

        ModelPartData upperBody_r1 = upperBody.addChild("upperBody_r1", ModelPartBuilder.create().uv(26, 4).cuboid(-4.0F, -3.5F, -3.5F, 8.0F, 7.0F, 3.0F, new Dilation(0.2F)), ModelTransform.of(0.6875F, 2.0F, 1.0F, 0.1745F, 0.0F, 0.0F));

        ModelPartData upperBody_r2 = upperBody.addChild("upperBody_r2", ModelPartBuilder.create().uv(22, 0).cuboid(-4.0F, -3.5F, -3.5F, 8.0F, 7.0F, 7.0F, new Dilation(0.4F)), ModelTransform.of(0.6875F, 3.6F, 2.0F, 0.1745F, 0.0F, 0.0F));

        ModelPartData tail = main.addChild("tail", ModelPartBuilder.create(), ModelTransform.pivot(-0.5625F, -8.2447F, 6.2298F));

        ModelPartData tail_r1 = tail.addChild("tail_r1", ModelPartBuilder.create().uv(0, 35).cuboid(-3.0F, -0.1747F, -5.375F, 6.0F, 11.0F, 6.0F, new Dilation(-0.1F)), ModelTransform.of(0.6875F, -3.5F, -2.5F, 0.9599F, 0.0F, 0.0F));

        ModelPartData head = main.addChild("head", ModelPartBuilder.create(), ModelTransform.pivot(-0.5625F, -8.0363F, -11.7107F));

        ModelPartData head_r1 = head.addChild("head_r1", ModelPartBuilder.create().uv(24, 41).cuboid(-1.5F, -1.5F, -2.0F, 3.0F, 3.0F, 4.0F, new Dilation(0.0F)), ModelTransform.of(0.6875F, 0.6383F, -3.45F, 0.096F, 0.0F, 0.0F));

        ModelPartData head_r2 = head.addChild("head_r2", ModelPartBuilder.create().uv(8, 0).cuboid(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F, new Dilation(0.25F)), ModelTransform.of(2.6875F, -5.6461F, -0.25F, 0.0F, 0.3927F, 0.0F));

        ModelPartData head_r3 = head.addChild("head_r3", ModelPartBuilder.create().uv(8, 0).cuboid(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F, new Dilation(0.25F)), ModelTransform.of(-1.3125F, -5.6461F, -0.25F, 0.0F, -0.4363F, 0.0F));

        ModelPartData head_r4 = head.addChild("head_r4", ModelPartBuilder.create().uv(34, 23).cuboid(-4.0F, -3.5F, -3.0F, 8.0F, 7.0F, 6.0F, new Dilation(-0.1F)), ModelTransform.of(0.6875F, -1.7461F, 0.25F, 0.0873F, 0.0F, 0.0F));

        ModelPartData leg1 = main.addChild("leg1", ModelPartBuilder.create(), ModelTransform.pivot(2.9375F, -8.7465F, 1.5332F));

        ModelPartData leg3_r1 = leg1.addChild("leg3_r1", ModelPartBuilder.create().uv(42, 36).mirrored().cuboid(-1.5F, -1.0F, -6.0F, 2.0F, 1.0F, 3.0F, new Dilation(0.1F)).mirrored(false), ModelTransform.of(2.5875F, 8.864F, 3.9061F, 0.0F, -0.0436F, 0.0F));

        ModelPartData leg3_r2 = leg1.addChild("leg3_r2", ModelPartBuilder.create().uv(38, 40).mirrored().cuboid(-1.0F, -2.2F, -3.0F, 2.0F, 6.0F, 4.0F, new Dilation(0.2F)).mirrored(false), ModelTransform.of(0.6875F, 1.3288F, 1.1589F, -0.5219F, -0.0436F, -0.0756F));

        ModelPartData leg3_r3 = leg1.addChild("leg3_r3", ModelPartBuilder.create().uv(14, 4).mirrored().cuboid(-1.0F, -2.2F, -1.0F, 2.0F, 4.4F, 2.0F, new Dilation(0.1F)).mirrored(false), ModelTransform.of(1.1875F, 4.2288F, 0.4589F, 1.2735F, -0.1112F, -0.146F));

        ModelPartData leg3_r4 = leg1.addChild("leg3_r4", ModelPartBuilder.create().uv(0, 15).mirrored().cuboid(-1.5F, -6.0F, -5.0F, 2.0F, 5.2F, 2.0F, new Dilation(0.0F)).mirrored(false), ModelTransform.of(2.8875F, 10.964F, 3.0061F, -0.5156F, -0.0955F, -0.1668F));

        ModelPartData leg2 = main.addChild("leg2", ModelPartBuilder.create().uv(42, 36).mirrored().cuboid(-1.0F, 6.1482F, -1.4786F, 2.0F, 1.0F, 3.0F, new Dilation(0.1F)).mirrored(false), ModelTransform.pivot(-3.375F, -7.3306F, -8.5821F));

        ModelPartData leg3_r5 = leg2.addChild("leg3_r5", ModelPartBuilder.create().uv(14, 4).mirrored().cuboid(-1.0F, -2.2F, -1.0F, 2.0F, 2.0F, 2.0F, new Dilation(0.5F)).mirrored(false)
                .uv(14, 4).mirrored().cuboid(-1.0F, -2.2F, -1.0F, 2.0F, 4.4F, 2.0F, new Dilation(0.1F)).mirrored(false), ModelTransform.of(0.0F, 0.704F, 1.3937F, 0.6109F, 0.0F, 0.0F));

        ModelPartData leg3_r6 = leg2.addChild("leg3_r6", ModelPartBuilder.create().uv(0, 15).mirrored().cuboid(-1.5F, -6.0F, -5.0F, 2.0F, 5.2F, 2.0F, new Dilation(0.0F)).mirrored(false), ModelTransform.of(0.5F, 8.5482F, 4.1214F, -0.3491F, 0.0F, 0.0F));

        ModelPartData leg3 = main.addChild("leg3", ModelPartBuilder.create().uv(42, 36).cuboid(-1.0F, 6.1482F, -1.4786F, 2.0F, 1.0F, 3.0F, new Dilation(0.1F)), ModelTransform.pivot(3.625F, -7.3306F, -8.5821F));

        ModelPartData leg3_r7 = leg3.addChild("leg3_r7", ModelPartBuilder.create().uv(14, 4).cuboid(-1.0F, -2.2F, -1.0F, 2.0F, 2.0F, 2.0F, new Dilation(0.5F))
                .uv(14, 4).cuboid(-1.0F, -2.2F, -1.0F, 2.0F, 4.4F, 2.0F, new Dilation(0.1F)), ModelTransform.of(0.0F, 0.704F, 1.3937F, 0.6109F, 0.0F, 0.0F));

        ModelPartData leg3_r8 = leg3.addChild("leg3_r8", ModelPartBuilder.create().uv(0, 15).cuboid(-0.5F, -6.0F, -5.0F, 2.0F, 5.2F, 2.0F, new Dilation(0.0F)), ModelTransform.of(-0.5F, 8.5482F, 4.1214F, -0.3491F, 0.0F, 0.0F));

        ModelPartData leg0 = main.addChild("leg0", ModelPartBuilder.create(), ModelTransform.pivot(-2.6875F, -8.7465F, 1.5332F));

        ModelPartData leg3_r9 = leg0.addChild("leg3_r9", ModelPartBuilder.create().uv(42, 36).cuboid(-0.5F, -1.0F, -6.0F, 2.0F, 1.0F, 3.0F, new Dilation(0.1F)), ModelTransform.of(-2.5875F, 8.864F, 3.9061F, 0.0F, 0.0436F, 0.0F));

        ModelPartData leg3_r10 = leg0.addChild("leg3_r10", ModelPartBuilder.create().uv(38, 40).cuboid(-1.0F, -2.2F, -3.0F, 2.0F, 6.0F, 4.0F, new Dilation(0.2F)), ModelTransform.of(-0.6875F, 1.3288F, 1.1589F, -0.5219F, 0.0436F, 0.0756F));

        ModelPartData leg3_r11 = leg0.addChild("leg3_r11", ModelPartBuilder.create().uv(14, 4).cuboid(-1.0F, -2.2F, -1.0F, 2.0F, 4.4F, 2.0F, new Dilation(0.1F)), ModelTransform.of(-1.1875F, 4.2288F, 0.4589F, 1.2735F, 0.1112F, 0.146F));

        ModelPartData leg3_r12 = leg0.addChild("leg3_r12", ModelPartBuilder.create().uv(0, 15).cuboid(-0.5F, -6.0F, -5.0F, 2.0F, 5.2F, 2.0F, new Dilation(0.0F)), ModelTransform.of(-2.8875F, 10.964F, 3.0061F, -0.5156F, 0.0955F, 0.1668F));
        return TexturedModelData.of(modelData, 128, 128);
    }
        @Override
	public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, int color) {

        super.render(matrices, vertexConsumer, light, overlay, color);

	}
    @Override
    public ModelPart getPart() {
        return main;
    }


    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        resetRotations();
        applyArchetypeProportions(entity.getDefinition().archetype());
        ProceduralAnimator.applyHeadTracking(head, headYaw, headPitch, animationProfile);
        var rightFrontLeg = leg3;
        var leftFrontLeg  = leg2;
        var rightHindLeg  = leg1;
        var leftHindLeg   = leg0;
        var s = animationState;
        if(entity.handSwinging){
            float progress = MathHelper.lerp(
                    tickDelta,
                    entity.lastHandSwingProgress,
                    entity.handSwingProgress
            );

            if (progress > 0.01f) {
                ProceduralAnimator.applyQuadrupedAttack(
                        leg3, leg2,
                        leg1, leg0,
                        main, head,
                        progress,
                        animationProfile
                );
            }
        }
        else
        if (true) {
            ProceduralAnimator.applyQuadrupedWalk(
                    leg2, leg3, leg0, leg1,
                    head, tail, limbAngle, limbDistance, animationProfile);
        } else {
            ProceduralAnimator.applyIdle(body, head, animationProgress, animationProfile);
        }

        if (s.isAttacking) {
            head.pitch -= animationProfile.attackAmplitude() * 0.5f;
        }
    }

    private void resetRotations() {
        head.pitch = head.yaw = head.roll = 0;
        body.pitch = body.yaw = body.roll = 0;
        tail.pitch = tail.yaw = tail.roll = 0;
        leg0.pitch = leg0.yaw = leg0.roll = 0;
        leg1.pitch = leg1.yaw = leg1.roll = 0;
        leg2.pitch = leg2.yaw = leg2.roll = 0;
        leg3.pitch = leg3.yaw = leg3.roll = 0;
        main.pitch = main.yaw = main.roll = 0;

        head.xScale      = head.yScale      = head.zScale      = 1f;
        body.xScale      = body.yScale      = body.zScale      = 1f;
        upperBody.xScale = upperBody.yScale = upperBody.zScale = 1f;
        leg0.xScale      = leg0.yScale      = leg0.zScale      = 1f;
        leg1.xScale      = leg1.yScale      = leg1.zScale      = 1f;
        leg2.xScale      = leg2.yScale      = leg2.zScale      = 1f;
        leg3.xScale      = leg3.yScale      = leg3.zScale      = 1f;
    }

    private void applyArchetypeProportions(String archetype) {
        if (archetype == null) return;
        switch (archetype) {
            case "bruiser" -> {
                // leg3=rightFront, leg2=leftFront, leg1=rightHind, leg0=leftHind
                leg3.xScale = leg2.xScale = 1.25f;
                leg3.zScale = leg2.zScale = 1.25f;
                leg1.xScale = leg0.xScale = 1.20f;
                leg1.zScale = leg0.zScale = 1.20f;
                upperBody.xScale = 1.15f;
                upperBody.yScale = 1.10f;
                upperBody.zScale = 1.10f;
                head.xScale = head.yScale = head.zScale = 1.15f;
            }
            case "ambusher" -> {
                leg3.xScale = leg2.xScale = 0.80f;
                leg3.zScale = leg2.zScale = 0.80f;
                leg1.xScale = leg0.xScale = 0.80f;
                leg1.zScale = leg0.zScale = 0.80f;
                body.zScale = 1.10f;
            }
            // skirmisher: neutral
        }
    }
}