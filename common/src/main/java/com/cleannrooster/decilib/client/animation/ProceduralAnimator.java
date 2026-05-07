package com.cleannrooster.decilib.client.animation;

import net.minecraft.client.model.ModelPart;
import net.minecraft.util.math.MathHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ProceduralAnimator {

    private static final float PI = (float) Math.PI;

    private ProceduralAnimator() {}

    // -------------------------------------------------------------------------
    // Shared (humanoid + quadruped)
    // -------------------------------------------------------------------------

    public static void applyHeadTracking(ModelPart head, float headYaw, float headPitch,
                                          AnimationProfile profile) {
        float s = profile.headTrackStrength();
        head.yaw   = headYaw   * (PI / 180.0f) * s;
        head.pitch = headPitch * (PI / 180.0f) * s;
    }

    public static void applyIdle(ModelPart body, ModelPart head,
                                  float animationProgress,
                                  AnimationProfile profile) {
        float sway = MathHelper.sin(animationProgress * profile.idleSpeed()) * profile.idleAmplitude();
        body.pitch = sway;
        head.pitch -= sway * 0.5f;
    }

    // -------------------------------------------------------------------------
    // Humanoid
    // -------------------------------------------------------------------------

    public static void applyWalk(ModelPart rightArm, ModelPart leftArm,
                                  ModelPart rightLeg, ModelPart leftLeg,
                                  float limbAngle, float limbDistance,
                                  AnimationProfile profile) {
        float movement = Math.max(limbDistance, 0.2f);
        float amp = profile.walkAmplitude() * movement;
        float freq = 0.6662f;
        rightLeg.pitch =  MathHelper.cos(limbAngle * freq)       * 1.4f * amp;
        leftLeg.pitch  =  MathHelper.cos(limbAngle * freq + PI)  * 1.4f * amp;
        // arms swing opposite phase to their paired leg
        rightArm.pitch =  MathHelper.cos(limbAngle * freq + PI)  * amp;
        leftArm.pitch  =  MathHelper.cos(limbAngle * freq)       * amp;
    }
    public static void applyQuadrupedAttack(
            ModelPart rightFrontLeg, ModelPart leftFrontLeg,
            ModelPart rightHindLeg,  ModelPart leftHindLeg,
            ModelPart body, ModelPart head,
            float progress,
            AnimationProfile profile
    ) {
        float t = MathHelper.clamp(progress, 0.0f, 1.0f);

        // Global smoothing curve (removes linear stepping feel)
        float s = MathHelper.sin(t * PI * 0.5f);

        // Continuous envelopes (no phase split)
        float rear   = 2*MathHelper.sin(t * PI) * (1.0f - t); // early emphasis
        float strike = MathHelper.sin(t * PI) * t;          // late emphasis

        float amp = profile.attackAmplitude()*2;

        // -------------------------
        // Body (core motion)
        // -------------------------
        float bodyBack   = -0.35f * rear;
        float bodyForward=  0.65f * strike;

        body.pitch += (bodyBack + bodyForward) * amp;

        // Head follows with slight exaggeration
        head.pitch += (0.20f * rear - 0.50f * strike) * amp;

        // -------------------------
        // Front legs (claw motion)
        // -------------------------
        float frontLift  = -0.6f * rear;
        float frontSwing =  1.2f * strike;

        float front = (frontLift + frontSwing) * amp;

        rightFrontLeg.pitch += front;
        leftFrontLeg.pitch  += front;

        // Asymmetry for swipe readability
        rightFrontLeg.roll -= 0.2f * strike * amp;
        leftFrontLeg.roll  += 0.2f * strike * amp;

        // -------------------------
        // Hind legs (support + push)
        // -------------------------
        float hind = (0.45f * rear - 0.30f * strike) * amp;

        rightHindLeg.pitch += hind;
        leftHindLeg.pitch  += hind;
    }
    public static void applyAttack(ModelPart rightArm, ModelPart leftArm,
                                    AnimationProfile profile, float progress) {
        float pull = PI * 0.4f * profile.attackAmplitude()*progress;
        rightArm.pitch -= pull;
        leftArm.pitch  -= pull * 0.4f;
    }

    public static void applyAmbushCrouch(ModelPart body, ModelPart head) {
        body.pitch  =  0.35f;
        head.pitch -= 0.35f;
    }

    public static void applyEmerge(ModelPart body, ModelPart head, float emergeProgress) {
        body.pitch  =  0.35f * emergeProgress;
        head.pitch -= 0.35f * emergeProgress;
    }

    public static void applyHeavyStance(ModelPart body, ModelPart rightArm, ModelPart leftArm) {
        body.pitch    =  0.15f;
        rightArm.roll = -0.3f;
        leftArm.roll  =  0.3f;
    }

    // -------------------------------------------------------------------------
    // Quadruped
    // -------------------------------------------------------------------------

    public static void applyQuadrupedWalk(ModelPart rightFrontLeg, ModelPart leftFrontLeg,
                                           ModelPart rightHindLeg,  ModelPart leftHindLeg,
                                           ModelPart head,          ModelPart tail,
                                           float limbAngle, float limbDistance,
                                           AnimationProfile profile) {
        float movement = Math.max(limbDistance, 0.2f);
        float amp = profile.walkAmplitude() * movement;
        float freq = 0.6662f;
        // diagonal pairs: right-front + left-hind in phase; left-front + right-hind in opposite phase
        rightFrontLeg.pitch =  MathHelper.cos(limbAngle * freq)       * 1.4f * amp;
        leftHindLeg.pitch   =  MathHelper.cos(limbAngle * freq)       * 1.4f * amp;
        leftFrontLeg.pitch  =  MathHelper.cos(limbAngle * freq + PI)  * 1.4f * amp;
        rightHindLeg.pitch  =  MathHelper.cos(limbAngle * freq + PI)  * 1.4f * amp;
        head.pitch  = MathHelper.cos(limbAngle * freq * 2.0f) * 0.05f * limbDistance;
        tail.yaw    = MathHelper.sin(limbAngle * freq)         * 0.4f  * limbDistance;
    }
}
