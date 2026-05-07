package com.cleannrooster.decilib.entity;

public class MobScale {

    public static float canineHeight    = 0.8F;
    public static float canineWidth     = 0.6F;
    public static float humanoidHeight  = 1.8F;
    public static float humanoidWidth   = 0.6F;
    public static float large           = 1.2F;
    public static float small           = 0.8F;
    public static float extra           = 1.2F;
    public static float mini            = 0.8F;

    // ── Biped ────────────────────────────────────────────────────────────────
    public static Preset AGILE_BIPED       = new Preset(1F,          humanoidWidth, humanoidHeight);
    public static Preset ARMORED_BIPED     = new Preset(large,        humanoidWidth, humanoidHeight);
    public static Preset ARCANE_BIPED      = new Preset(1F,           humanoidWidth, humanoidHeight);
    public static Preset STANDARD_BIPED    = new Preset(extra * large, humanoidWidth, humanoidHeight);
    public static Preset HEAVY_BIPED       = new Preset(large,         humanoidWidth, humanoidHeight);
    public static Preset ALIEN_BIPED       = new Preset(1F,            humanoidWidth, humanoidHeight * large);

    // ── Quadruped ────────────────────────────────────────────────────────────
    public static Preset HEAVY_QUADRUPED   = new Preset(extra * large, canineWidth, canineHeight);
    public static Preset STANDARD_QUADRUPED= new Preset(1F,            1.8F,        1.2F);
    public static Preset SPECTRAL_QUADRUPED= new Preset(large,         canineWidth, canineHeight);
    public static Preset STALKER_QUADRUPED = new Preset(1F,            canineWidth, canineHeight);
    public static Preset SWIFT_QUADRUPED   = new Preset(large,         canineWidth, canineHeight);
    public static Preset SMALL_QUADRUPED   = new Preset(small,         humanoidWidth, humanoidHeight);

    public record Preset(float scale, float width, float height) {}
}
