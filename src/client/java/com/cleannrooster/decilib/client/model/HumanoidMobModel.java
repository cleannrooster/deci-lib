package com.cleannrooster.decilib.client.model;

import com.cleannrooster.decilib.builder.entity.DataDrivenMob;
import com.cleannrooster.decilib.client.animation.AnimationProfile;
import com.cleannrooster.decilib.client.animation.MobAnimationState;
import com.cleannrooster.decilib.client.animation.ProceduralAnimator;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class HumanoidMobModel<T extends LivingEntity> extends SinglePartEntityModel<T> {

    public static final EntityModelLayer LAYER =
            new EntityModelLayer(Identifier.of("deci-lib", "humanoid_mob"), "main");

    protected final ModelPart root;
    protected final ModelPart body;
    protected final ModelPart head;
    protected final ModelPart rightArm;
    protected final ModelPart leftArm;
    protected final ModelPart rightLeg;
    protected final ModelPart leftLeg;

    protected final AnimationProfile animationProfile;

    public final MobAnimationState animationState = new MobAnimationState();

    public HumanoidMobModel(ModelPart root, AnimationProfile profile) {
        this.root             = root;
        this.animationProfile = profile;
        this.body             = root.getChild("body");
        this.head             = body.getChild("head");
        this.rightArm         = body.getChild("right_arm");
        this.leftArm          = body.getChild("left_arm");
        this.rightLeg         = body.getChild("right_leg");
        this.leftLeg          = body.getChild("left_leg");
    }

    // -------------------------------------------------------------------------

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();

        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create().uv(16, 16).cuboid(-4, 0, -2, 8, 12, 4),
                ModelTransform.pivot(0, 0, 0));

        body.addChild("head",
                ModelPartBuilder.create().uv(0, 0).cuboid(-4, -8, -4, 8, 8, 8),
                ModelTransform.pivot(0, 0, 0));

        body.addChild("right_arm",
                ModelPartBuilder.create().uv(40, 16).cuboid(-3, -2, -2, 4, 12, 4),
                ModelTransform.pivot(-5, 2, 0));

        body.addChild("left_arm",
                ModelPartBuilder.create().uv(32, 48).cuboid(-1, -2, -2, 4, 12, 4),
                ModelTransform.pivot(5, 2, 0));

        body.addChild("right_leg",
                ModelPartBuilder.create().uv(0, 16).cuboid(-2, 0, -2, 4, 12, 4),
                ModelTransform.pivot(-2, 12, 0));

        body.addChild("left_leg",
                ModelPartBuilder.create().uv(16, 48).cuboid(-2, 0, -2, 4, 12, 4),
                ModelTransform.pivot(2, 12, 0));

        return TexturedModelData.of(data, 64, 64);
    }

    // -------------------------------------------------------------------------

    @Override
    public ModelPart getPart() {
        return root;
    }
    public float tickDelta;


    public void setTickDelta(float tickDelta) {
        this.tickDelta = tickDelta;
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance,
                           float animationProgress, float headYaw, float headPitch) {
        resetRotations();

        if (entity instanceof DataDrivenMob mob) {
            applyArchetypeProportions(mob.getDefinition().archetype());
        }

        ProceduralAnimator.applyHeadTracking(head, headYaw, headPitch, animationProfile);

        var s = animationState;

        if (s.isHidden) {
            ProceduralAnimator.applyAmbushCrouch(body, head);
        }
        {
            if (s.isEmerging) {
                ProceduralAnimator.applyEmerge(body, head, s.emergeProgress);
            }
            if (true) {
                ProceduralAnimator.applyWalk(rightArm, leftArm, rightLeg, leftLeg,
                        limbAngle, limbDistance, animationProfile);
            } else {
                ProceduralAnimator.applyIdle(body, head, animationProgress, animationProfile);
            }
            if (s.isAttacking) {
                float progress = MathHelper.lerp(
                        tickDelta,
                        entity.lastHandSwingProgress,
                        entity.handSwingProgress
                );
                ProceduralAnimator.applyAttack(rightArm, leftArm, animationProfile,progress);
            }
        }
    }

    // ── Proportion scaling ────────────────────────────────────────────────────

    private void applyArchetypeProportions(String archetype) {
        if (archetype == null) return;
        switch (archetype) {
            case "bruiser" -> {
                // Wide, muscular silhouette: thick arms and legs, broader torso
                rightArm.xScale = leftArm.xScale = 1.28f;
                rightArm.yScale = leftArm.yScale = 1.18f;
                rightArm.zScale = leftArm.zScale = 1.28f;
                rightLeg.xScale = leftLeg.xScale = 1.20f;
                rightLeg.yScale = leftLeg.yScale = 1.12f;
                rightLeg.zScale = leftLeg.zScale = 1.20f;
                body.xScale = 1.15f;
                body.zScale = 1.15f;
            }
            case "ambusher" -> {
                // Lean, narrow silhouette: slender arms and legs for low profile
                rightArm.xScale = leftArm.xScale = 0.75f;
                rightArm.yScale = leftArm.yScale = 0.88f;
                rightArm.zScale = leftArm.zScale = 0.75f;
                rightLeg.xScale = leftLeg.xScale = 0.80f;
                rightLeg.yScale = leftLeg.yScale = 0.90f;
                rightLeg.zScale = leftLeg.zScale = 0.80f;
            }
            // skirmisher: neutral — already reset to 1f
        }
    }

    protected void resetRotations() {
        head.pitch     = head.yaw     = head.roll     = 0;
        body.pitch     = body.yaw     = body.roll     = 0;
        rightArm.pitch = rightArm.yaw = rightArm.roll = 0;
        leftArm.pitch  = leftArm.yaw  = leftArm.roll  = 0;
        rightLeg.pitch = rightLeg.yaw = rightLeg.roll = 0;
        leftLeg.pitch  = leftLeg.yaw  = leftLeg.roll  = 0;

        head.xScale     = head.yScale     = head.zScale     = 1f;
        body.xScale     = body.yScale     = body.zScale     = 1f;
        rightArm.xScale = rightArm.yScale = rightArm.zScale = 1f;
        leftArm.xScale  = leftArm.yScale  = leftArm.zScale  = 1f;
        rightLeg.xScale = rightLeg.yScale = rightLeg.zScale = 1f;
        leftLeg.xScale  = leftLeg.yScale  = leftLeg.zScale  = 1f;
    }
}
