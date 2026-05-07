package com.cleannrooster.decilib.builder.entity;

import net.minecraft.block.Blocks;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;


public enum ParticleStyle {

    NONE {
        @Override public void spawn(MobEntity e, ServerWorld w, float range, float halfAngleDeg) {}
    },


    SMOKE_RING {
        @Override public void spawn(MobEntity e, ServerWorld w, float range, float halfAngleDeg) {
            double cx = e.getX(), cy = e.getY(), cz = e.getZ();

            // Directed outer ring: 20 LARGE_SMOKE particles fly outward + upward from ring edge
            for (int i = 0; i < 20; i++) {
                double a   = (2 * Math.PI / 20) * i;
                double cos = Math.cos(a), sin = Math.sin(a);
                w.spawnParticles(ParticleTypes.LARGE_SMOKE,
                        cx + range * cos, cy + 0.15, cz + range * sin,
                        0, cos, 0.22, sin, 0.30); // count=0 → directed outward
            }

            // Block debris: 12 directed chunks ejected outward + strong upward arc
            var ground = w.getBlockState(e.getBlockPos().down());
            if (!ground.isAir()) {
                for (int i = 0; i < 12; i++) {
                    double a   = (2 * Math.PI / 12) * i;
                    double cos = Math.cos(a), sin = Math.sin(a);
                    w.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, ground),
                            cx, cy + 0.1, cz,
                            0, cos, 0.60, sin, 0.55); // outward + high upward arc
                }
                // Dense central debris cloud
                w.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, ground),
                        cx, cy + 0.1, cz, 22, range * 0.35, 0.2, range * 0.35, 0.20);
            }

            // Central explosion flash + upward smoke column
            w.spawnParticles(ParticleTypes.EXPLOSION, cx, cy + 0.3, cz, 1, 0, 0, 0, 0);
            w.spawnParticles(ParticleTypes.LARGE_SMOKE, cx, cy + 0.6, cz,
                    8, 0.45, 0.40, 0.45, 0.05);
        }
    },


    PORTAL_RING {
        @Override public void spawn(MobEntity e, ServerWorld w, float range, float halfAngleDeg) {
            double cx = e.getX(), cy = e.getY() + 0.5, cz = e.getZ();

            // Inward ring: 24 particles fly from ring edge toward center
            for (int i = 0; i < 24; i++) {
                double a   = (2 * Math.PI / 24) * i;
                double cos = Math.cos(a), sin = Math.sin(a);
                w.spawnParticles(ParticleTypes.PORTAL,
                        cx + range * cos, cy, cz + range * sin,
                        0, -cos, 0.06, -sin, 0.40); // count=0 → directed inward
            }

            // Dense central detonation burst
            w.spawnParticles(ParticleTypes.PORTAL, cx, cy + 0.3, cz,
                    36, 0.55, 0.40, 0.55, 0.20);

            // Outer scatter at height — residual void energy
            w.spawnParticles(ParticleTypes.PORTAL, cx, cy + 0.9, cz,
                    18, range * 0.7, 0.25, range * 0.7, 0.07);
        }
    },


    PORTAL_PULSE {
        @Override public void spawn(MobEntity e, ServerWorld w, float range, float halfAngleDeg) {
            double cx = e.getX(), cy = e.getY() + 0.5, cz = e.getZ();

            // Particle 1: random radius 0.8–2.2, directed inward
            double r1 = 0.8 + w.random.nextFloat() * 1.4;
            double a1 = w.random.nextFloat() * 2 * Math.PI;
            double c1 = Math.cos(a1), s1 = Math.sin(a1);
            w.spawnParticles(ParticleTypes.PORTAL,
                    cx + r1 * c1, cy + w.random.nextFloat() * 0.8, cz + r1 * s1,
                    0, -c1, 0.04, -s1, 0.35);

            // Particle 2: different angle
            double r2 = 0.6 + w.random.nextFloat() * 1.6;
            double a2 = w.random.nextFloat() * 2 * Math.PI;
            double c2 = Math.cos(a2), s2 = Math.sin(a2);
            w.spawnParticles(ParticleTypes.PORTAL,
                    cx + r2 * c2, cy + w.random.nextFloat() * 0.6, cz + r2 * s2,
                    0, -c2, 0.03, -s2, 0.32);
        }
    },


    SOUL_RING {
        @Override public void spawn(MobEntity e, ServerWorld w, float range, float halfAngleDeg) {
            double cx = e.getX(), cy = e.getY(), cz = e.getZ();

            // Outer ring: directed outward + upward
            for (int i = 0; i < 20; i++) {
                double a   = (2 * Math.PI / 20) * i;
                double cos = Math.cos(a), sin = Math.sin(a);
                w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        cx + range * cos, cy + 0.1, cz + range * sin,
                        0, cos * 0.14, 0.28, sin * 0.14, 1.0); // outward + upward
            }

            // Inner ring at mid-height, offset rotation, directed straight upward
            for (int i = 0; i < 14; i++) {
                double a   = (2 * Math.PI / 14) * i + Math.PI / 14; // half-step offset
                double cos = Math.cos(a), sin = Math.sin(a);
                w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        cx + range * 0.52 * cos, cy + 0.65, cz + range * 0.52 * sin,
                        0, 0, 0.22, 0, 1.0); // straight upward column
            }

            // Central burst
            w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, cx, cy + 0.25, cz,
                    12, 0.50, 0.30, 0.50, 0.07);
            w.spawnParticles(ParticleTypes.SOUL, cx, cy + 0.1, cz,
                    6, 0.40, 0.20, 0.40, 0.05);
        }
    },

    SWEEP_ARC {
        @Override public void spawn(MobEntity e, ServerWorld w, float range, float halfAngleDeg) {
            if (halfAngleDeg <= 0f) return;

            double yaw     = Math.toRadians(e.getYaw());
            double halfRad = Math.toRadians(halfAngleDeg);
            double cx = e.getX(), cy = e.getY() + 0.55, cz = e.getZ();

            // Sample density: ~1 sample per 4.5° of arc; minimum 10
            int    samples  = Math.max(10, (int) (halfAngleDeg * 2.0 / 4.5));
            double maxDepth = range * 0.30; // maximum radial inset at arc centre
            double jitterRad = Math.toRadians(3.0);

            // Quarter-sample indices for SWEEP_ATTACK placement
            int q1 = samples / 4;
            int q2 = samples / 2;
            int q3 = samples - q1;

            for (int i = 0; i <= samples; i++) {
                double t   = (double) i / samples;           // 0 → 1 across arc
                double u   = t * 2.0 - 1.0;                  // −1 at edges, 0 at centre
                double ang = yaw - halfRad + t * 2.0 * halfRad; // world angle for this sample

                // cos² taper: 1.0 at centre, 0.0 at edges
                double cosHalf = Math.cos(u * Math.PI * 0.5);
                double taper   = cosHalf * cosHalf;

                // Per-sample angular jitter for visual variation
                double jitteredAng = ang + (w.random.nextDouble() - 0.5) * jitterRad * 2.0;
                double dirX = -Math.sin(jitteredAng);
                double dirZ =  Math.cos(jitteredAng);

                // Number of depth layers: 1 at edges, up to 3 at centre
                int layers = 1 + (int) (taper * 2.5);

                for (int layer = 0; layer < layers; layer++) {
                    // depthFrac 0 = outermost, 1 = innermost (closest to entity)
                    double depthFrac = (layers > 1) ? (double) layer / (layers - 1) : 0.0;
                    double r  = range - maxDepth * taper * depthFrac;
                    double px = cx + r * dirX;
                    double pz = cz + r * dirZ;
                    // Height rises toward centre (taper drives the arc crown)
                    double hy = cy + taper * 0.25 - layer * 0.09;

                    if (layer == 0) {
                        // Outermost edge: directed outward so particles fly away visibly
                        w.spawnParticles(ParticleTypes.CRIT, px, hy, pz,
                                0, dirX, 0.05, dirZ, 0.14);
                    } else {
                        // Inner fill: scattered cloud, more spread at edges for softness
                        int    cnt    = 1 + (int) (taper * 1.5);
                        double spread = 0.05 + (1.0 - taper) * 0.10;
                        w.spawnParticles(ParticleTypes.CRIT, px, hy, pz,
                                cnt, spread, 0.08, spread, 0.04);
                    }
                }

                // SWEEP_ATTACK stars at quarter and centre points
                if (i == q1 || i == q2 || i == q3) {
                    w.spawnParticles(ParticleTypes.SWEEP_ATTACK,
                            cx + range * dirX, cy + taper * 0.25, cz + range * dirZ,
                            1, 0, 0, 0, 0);
                }
            }
        }
    },


    CRIT_ARC {
        @Override public void spawn(MobEntity e, ServerWorld w, float range, float halfAngleDeg) {
            double yaw = Math.toRadians(e.getYaw());
            double fx  = -Math.sin(yaw), fz = Math.cos(yaw);
            double cx  = e.getX() + fx * (range * 0.55);
            double cy  = e.getY() + 0.5;
            double cz  = e.getZ() + fz * (range * 0.55);

            // Large sweep star
            w.spawnParticles(ParticleTypes.SWEEP_ATTACK, cx, cy, cz, 1, 0, 0, 0, 0);

            // Primary CRIT scatter — wide arc volume
            w.spawnParticles(ParticleTypes.CRIT, cx, cy, cz,
                    16, range * 0.50, 0.40, range * 0.50, 0.13);

            // Secondary forward CRIT wave — slightly deeper
            w.spawnParticles(ParticleTypes.CRIT,
                    e.getX() + fx * (range * 0.78), cy - 0.1, e.getZ() + fz * (range * 0.78),
                    10, range * 0.30, 0.28, range * 0.30, 0.09);

            // ENCHANT glyphs scattered through the arc
            w.spawnParticles(ParticleTypes.ENCHANT, cx, cy + 0.2, cz,
                    6, range * 0.42, 0.35, range * 0.42, 0.06);
        }
    },

    /**
     * POOF fan bursting in three backward jets + a SMOKE tail.
     * Used for dash/charge take-off on-start.
     */
    POOF_BURST {
        @Override public void spawn(MobEntity e, ServerWorld w, float range, float halfAngleDeg) {
            double yaw = Math.toRadians(e.getYaw());
            double bx  =  Math.sin(yaw); // backward vector
            double bz  = -Math.cos(yaw);
            double cx  = e.getX(), cy = e.getY(), cz = e.getZ();

            // Main cloud behind entity
            w.spawnParticles(ParticleTypes.POOF, cx + bx * 0.7, cy + 0.35, cz + bz * 0.7,
                    18, 0.40, 0.25, 0.40, 0.08);

            // Three directed jets: center-back and two diagonal fans
            for (int fan = -1; fan <= 1; fan++) {
                double fanAngle = yaw + Math.PI + fan * 0.48;
                w.spawnParticles(ParticleTypes.POOF, cx, cy + 0.38, cz,
                        0, -Math.sin(fanAngle), 0.12, Math.cos(fanAngle), 0.55);
            }

            // SMOKE tail
            w.spawnParticles(ParticleTypes.SMOKE, cx + bx * 0.5, cy + 0.25, cz + bz * 0.5,
                    6, 0.28, 0.12, 0.28, 0.02);
        }
    },


    ENCHANT_RING {
        @Override public void spawn(MobEntity e, ServerWorld w, float range, float halfAngleDeg) {
            double cx = e.getX(), cy = e.getY(), cz = e.getZ();

            // Lower ring at waist height: 16 ENCHANT glyphs flowing inward
            for (int i = 0; i < 16; i++) {
                double a   = (2 * Math.PI / 16) * i;
                double cos = Math.cos(a), sin = Math.sin(a);
                w.spawnParticles(ParticleTypes.ENCHANT,
                        cx + 1.5 * cos, cy + 0.7, cz + 1.5 * sin,
                        0, -cos, 0.08, -sin, 0.22); // directed inward
            }

            // Upper ring at head height: 12 glyphs, half-step rotation offset
            for (int i = 0; i < 12; i++) {
                double a   = (2 * Math.PI / 12) * i + Math.PI / 12;
                double cos = Math.cos(a), sin = Math.sin(a);
                w.spawnParticles(ParticleTypes.ENCHANT,
                        cx + 1.1 * cos, cy + 1.5, cz + 1.1 * sin,
                        0, -cos, 0.05, -sin, 0.16);
            }

            // Central upward geyser
            w.spawnParticles(ParticleTypes.ENCHANT, cx, cy + 0.5, cz,
                    16, 0.30, 0.60, 0.30, 0.10);

            // CRIT sparkle burst at chest level for additional readability
            w.spawnParticles(ParticleTypes.CRIT, cx, cy + 1.0, cz,
                    8, 0.50, 0.40, 0.50, 0.06);
        }
    },


    FALLING_IRON {
        @Override public void spawn(MobEntity e, ServerWorld w, float range, float halfAngleDeg) {
            double cx = e.getX(), cy = e.getY(), cz = e.getZ();
            var iron = new BlockStateParticleEffect(ParticleTypes.BLOCK,
                    Blocks.IRON_BLOCK.getDefaultState());

            // 8-direction directed spray: iron chunks fly outward and strongly upward
            for (int i = 0; i < 8; i++) {
                double a   = (2 * Math.PI / 8) * i;
                double cos = Math.cos(a), sin = Math.sin(a);
                w.spawnParticles(iron, cx, cy + 0.2, cz,
                        0, cos * 0.55, 0.72, sin * 0.55, 1.0); // outward + strong upward
            }

            // Dense central upward eruption
            w.spawnParticles(iron, cx, cy + 0.1, cz,
                    28, 0.70, 0.30, 0.70, 0.18);

            // LARGE_SMOKE pillars
            w.spawnParticles(ParticleTypes.LARGE_SMOKE, cx, cy + 0.4, cz,
                    10, 0.50, 0.45, 0.50, 0.05);
        }
    },


    EMBER_TRAIL {
        @Override public void spawn(MobEntity e, ServerWorld w, float range, float halfAngleDeg) {
            double cx = e.getX(), cy = e.getY(), cz = e.getZ();

            for (int i = 0; i < 3; i++) {
                float ox = (w.random.nextFloat() - 0.5f) * 0.5f;
                float oz = (w.random.nextFloat() - 0.5f) * 0.5f;
                float oy = w.random.nextFloat() * 0.35f;
                // Directed upward + slight random outward scatter
                w.spawnParticles(ParticleTypes.FLAME,
                        cx + ox * 0.6, cy + 0.25 + oy, cz + oz * 0.6,
                        0, ox * 0.28, 0.12, oz * 0.28, 1.0);
            }

            // Occasional smoke puff (~1 in 3 ticks)
            if (w.random.nextFloat() < 0.33f) {
                w.spawnParticles(ParticleTypes.SMOKE, cx, cy + 0.6, cz,
                        1, 0.20, 0.10, 0.20, 0.01);
            }
        }
    },


    DUST_TRAIL {
        @Override public void spawn(MobEntity e, ServerWorld w, float range, float halfAngleDeg) {
            double cx = e.getX(), cy = e.getY(), cz = e.getZ();

            w.spawnParticles(ParticleTypes.SMOKE, cx, cy + 0.1, cz,
                    3, 0.22, 0.06, 0.22, 0.008);

            // Occasional POOF for visibility at play distance (~1 in 4 ticks)
            if (w.random.nextFloat() < 0.25f) {
                w.spawnParticles(ParticleTypes.POOF, cx, cy + 0.05, cz,
                        1, 0.18, 0.05, 0.18, 0.0);
            }
        }
    },


    SPLASH_BURST {
        @Override public void spawn(MobEntity e, ServerWorld w, float range, float halfAngleDeg) {
            double cx = e.getX(), cy = e.getY(), cz = e.getZ();

            // 8-direction directed water jets: outward + strong upward
            for (int i = 0; i < 8; i++) {
                double a   = (2 * Math.PI / 8) * i;
                double cos = Math.cos(a), sin = Math.sin(a);
                w.spawnParticles(ParticleTypes.SPLASH, cx, cy + 0.15, cz,
                        0, cos, 0.85, sin, 0.45);
            }

            // Dense central upward cloud
            w.spawnParticles(ParticleTypes.SPLASH, cx, cy + 0.2, cz,
                    30, 0.60, 0.35, 0.60, 0.14);

            // RAIN particles falling from above — residual water spray
            w.spawnParticles(ParticleTypes.RAIN, cx, cy + 1.8, cz,
                    16, 1.00, 0.0, 1.00, 0.20);
        }
    },


    SMOKE_BURST {
        @Override public void spawn(MobEntity e, ServerWorld w, float range, float halfAngleDeg) {
            double cx = e.getX(), cy = e.getY() + 0.5, cz = e.getZ();

            // 6-direction outward jets
            for (int i = 0; i < 6; i++) {
                double a   = (2 * Math.PI / 6) * i;
                double cos = Math.cos(a), sin = Math.sin(a);
                w.spawnParticles(ParticleTypes.LARGE_SMOKE, cx, cy, cz,
                        0, cos, 0.32, sin, 0.28);
            }

            // Dense smoke cloud
            w.spawnParticles(ParticleTypes.LARGE_SMOKE, cx, cy + 0.2, cz,
                    16, 0.50, 0.42, 0.50, 0.05);

            // POOF burst for the visual pop of disappearance
            w.spawnParticles(ParticleTypes.POOF, cx, cy - 0.1, cz,
                    10, 0.45, 0.30, 0.45, 0.06);
        }
    };


    public abstract void spawn(MobEntity entity, ServerWorld world, float range, float halfAngleDeg);

    public static ParticleStyle fromString(String key) {
        return switch (key.toLowerCase()) {
            case "smoke_ring"    -> SMOKE_RING;
            case "portal_ring"   -> PORTAL_RING;
            case "portal_pulse"  -> PORTAL_PULSE;
            case "soul_ring"     -> SOUL_RING;
            case "sweep_arc"     -> SWEEP_ARC;
            case "crit_arc"      -> CRIT_ARC;
            case "poof_burst"    -> POOF_BURST;
            case "enchant_ring"  -> ENCHANT_RING;
            case "falling_iron"  -> FALLING_IRON;
            case "ember_trail"   -> EMBER_TRAIL;
            case "dust_trail"    -> DUST_TRAIL;
            case "splash_burst"  -> SPLASH_BURST;
            case "smoke_burst"   -> SMOKE_BURST;
            default              -> NONE;
        };
    }
}
