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

public class QuadrupedMobModel<T extends LivingEntity> extends SinglePartEntityModel<T> {

    public static final EntityModelLayer LAYER =
            new EntityModelLayer(Identifier.of("deci-lib", "quadruped_mob"), "main");

    private ModelPart root;
    private ModelPart body;
    private ModelPart head;
    private ModelPart tail;
    private ModelPart rightFrontLeg;
    private ModelPart leftFrontLeg;
    private ModelPart rightHindLeg;
    private ModelPart leftHindLeg;

    private AnimationProfile animationProfile;

    public final MobAnimationState animationState = new MobAnimationState();
    public float tickDelta;

    public QuadrupedMobModel() {}

    public void setTickDelta(float tickDelta) {
        this.tickDelta = tickDelta;
    }

    public QuadrupedMobModel(ModelPart root, AnimationProfile profile) {
        // Guard: ApexPredatorModel roots have a "main" child and use their own setup.
        if (root.hasChild("main")) return;

        this.root             = root;
        this.animationProfile = profile;
        this.body             = root.getChild("body");
        this.head             = body.getChild("head");
        this.tail             = body.getChild("tail");
        this.rightFrontLeg    = body.getChild("right_front_leg");
        this.leftFrontLeg     = body.getChild("left_front_leg");
        this.rightHindLeg     = body.getChild("right_hind_leg");
        this.leftHindLeg      = body.getChild("left_hind_leg");
    }

    // -------------------------------------------------------------------------

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();

        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create().uv(0, 0).cuboid(-5, 0, -7, 10, 8, 14),
                ModelTransform.pivot(0, 4, 2));

        body.addChild("head",
                ModelPartBuilder.create().uv(0, 22).cuboid(-4, -4, -8, 8, 8, 8),
                ModelTransform.pivot(0, 4, -7));

        body.addChild("tail",
                ModelPartBuilder.create().uv(50, 0).cuboid(-1, -1, 0, 2, 2, 6),
                ModelTransform.pivot(0, 4, 7));

        ModelPartBuilder legBuilder = ModelPartBuilder.create().uv(40, 22).cuboid(-2, 0, -2, 4, 8, 4);
        body.addChild("right_front_leg", legBuilder, ModelTransform.pivot(-4, 8, -3));
        body.addChild("left_front_leg",  legBuilder, ModelTransform.pivot( 4, 8, -3));
        body.addChild("right_hind_leg",  legBuilder, ModelTransform.pivot(-4, 8,  5));
        body.addChild("left_hind_leg",   legBuilder, ModelTransform.pivot( 4, 8,  5));

        return TexturedModelData.of(data, 64, 64);
    }

    // -------------------------------------------------------------------------

    @Override
    public ModelPart getPart() {
        return root;
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

        if (limbDistance > 0.01f) {
            ProceduralAnimator.applyQuadrupedWalk(
                    rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg,
                    head, tail, limbAngle, limbDistance, animationProfile);
        } else {
            ProceduralAnimator.applyIdle(body, head, animationProgress, animationProfile);
        }

        if (s.isAttacking) {
            head.pitch -= animationProfile.attackAmplitude() * 0.5f;
        }
    }

    // ── Proportion scaling ────────────────────────────────────────────────────

    private void applyArchetypeProportions(String archetype) {
        if (archetype == null) return;
        switch (archetype) {
            case "bruiser" -> {
                // Heavier, thicker build: wide legs and broad body
                rightFrontLeg.xScale = leftFrontLeg.xScale = 1.25f;
                rightFrontLeg.zScale = leftFrontLeg.zScale = 1.25f;
                rightHindLeg.xScale  = leftHindLeg.xScale  = 1.20f;
                rightHindLeg.zScale  = leftHindLeg.zScale  = 1.20f;
                body.xScale = 1.15f;
                body.yScale = 1.10f;
                head.xScale = head.yScale = head.zScale = 1.15f;
            }
            case "ambusher" -> {
                // Long, lean build: narrow legs, elongated body
                rightFrontLeg.xScale = leftFrontLeg.xScale = 0.80f;
                rightFrontLeg.zScale = leftFrontLeg.zScale = 0.80f;
                rightHindLeg.xScale  = leftHindLeg.xScale  = 0.80f;
                rightHindLeg.zScale  = leftHindLeg.zScale  = 0.80f;
                body.zScale = 1.10f;
            }
            // skirmisher: neutral — already reset to 1f
        }
    }

    private void resetRotations() {
        if (head == null) return; // guard for subclass constructors that skip field init

        head.pitch          = head.yaw          = head.roll          = 0;
        body.pitch          = body.yaw          = body.roll          = 0;
        tail.pitch          = tail.yaw          = tail.roll          = 0;
        rightFrontLeg.pitch = rightFrontLeg.yaw = rightFrontLeg.roll = 0;
        leftFrontLeg.pitch  = leftFrontLeg.yaw  = leftFrontLeg.roll  = 0;
        rightHindLeg.pitch  = rightHindLeg.yaw  = rightHindLeg.roll  = 0;
        leftHindLeg.pitch   = leftHindLeg.yaw   = leftHindLeg.roll   = 0;

        head.xScale          = head.yScale          = head.zScale          = 1f;
        body.xScale          = body.yScale          = body.zScale          = 1f;
        rightFrontLeg.xScale = rightFrontLeg.yScale = rightFrontLeg.zScale = 1f;
        leftFrontLeg.xScale  = leftFrontLeg.yScale  = leftFrontLeg.zScale  = 1f;
        rightHindLeg.xScale  = rightHindLeg.yScale  = rightHindLeg.zScale  = 1f;
        leftHindLeg.xScale   = leftHindLeg.yScale   = leftHindLeg.zScale   = 1f;
    }
}
