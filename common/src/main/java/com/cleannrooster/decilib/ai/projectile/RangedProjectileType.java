package com.cleannrooster.decilib.ai.projectile;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.entity.projectile.SpectralArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;

public enum RangedProjectileType {

    ARROW {
        @Override
        public void launch(MobEntity entity, ServerWorld world, float speed, float divergence) {
            var arrow = new ArrowEntity(world, entity, new ItemStack(Items.ARROW), null);
            arrow.setVelocity(entity, entity.getPitch(), entity.getYaw(), 0f, speed, divergence);
            world.spawnEntity(arrow);
        }
    },

    SPECTRAL_ARROW {
        @Override
        public void launch(MobEntity entity, ServerWorld world, float speed, float divergence) {
            var arrow = new SpectralArrowEntity(world, entity, new ItemStack(Items.SPECTRAL_ARROW), null);
            arrow.setVelocity(entity, entity.getPitch(), entity.getYaw(), 0f, speed, divergence);
            world.spawnEntity(arrow);
        }
    },

    TRIDENT {
        @Override
        public void launch(MobEntity entity, ServerWorld world, float speed, float divergence) {
            var trident = new TridentEntity(world, entity, new ItemStack(Items.TRIDENT));
            trident.setVelocity(entity, entity.getPitch(), entity.getYaw(), 0f, speed, divergence);
            world.spawnEntity(trident);
        }
    },

    SNOWBALL {
        @Override
        public void launch(MobEntity entity, ServerWorld world, float speed, float divergence) {
            var ball = new SnowballEntity(world, entity);
            ball.setVelocity(entity, entity.getPitch(), entity.getYaw(), 0f, speed, divergence);
            world.spawnEntity(ball);
        }
    },

    EGG {
        @Override
        public void launch(MobEntity entity, ServerWorld world, float speed, float divergence) {
            var egg = new EggEntity(world, entity);
            egg.setVelocity(entity, entity.getPitch(), entity.getYaw(), 0f, speed, divergence);
            world.spawnEntity(egg);
        }
    },

    SMALL_FIREBALL {
        @Override
        public void launch(MobEntity entity, ServerWorld world, float speed, float divergence) {
            var dir = entity.getRotationVec(1.0f);
            var fireball = new SmallFireballEntity(world, entity, dir.multiply(speed));
            fireball.setPosition(entity.getX(), entity.getEyeY() - 0.1, entity.getZ());
            world.spawnEntity(fireball);
        }
    };

    public abstract void launch(MobEntity entity, ServerWorld world, float speed, float divergence);

    // ── Aiming ───────────────────────────────────────────────────────────────

    /** Snaps the entity's yaw/pitch to face {@code target} before launching. */
    public static void aimAt(MobEntity entity, net.minecraft.entity.LivingEntity target) {
        double dx      = target.getX() - entity.getX();
        double dy      = target.getBodyY(0.3333) - entity.getEyeY();
        double dz      = target.getZ() - entity.getZ();
        double horiz   = Math.sqrt(dx * dx + dz * dz);
        float  pitch   = -(float) Math.toDegrees(Math.atan2(dy, horiz));
        float  yaw     = (float)  Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        entity.setYaw(yaw);
        entity.setBodyYaw(yaw);
        entity.setHeadYaw(yaw);
        entity.setPitch(pitch);
    }

    public static RangedProjectileType fromString(String s) {
        return switch (s.toLowerCase()) {
            case "spectral_arrow" -> SPECTRAL_ARROW;
            case "trident"        -> TRIDENT;
            case "snowball"       -> SNOWBALL;
            case "egg"            -> EGG;
            case "small_fireball",
                 "fireball"       -> SMALL_FIREBALL;
            default               -> ARROW;
        };
    }
}
