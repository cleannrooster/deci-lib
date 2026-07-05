package com.cleannrooster.decilib.builder.bossbar;

import net.minecraft.entity.boss.BossBar;

/**
 * Optional bossbar configuration for a data-driven mob, parsed from the root
 * {@code "bossbar"} block in the mob's JSON profile.
 *
 * <h3>JSON</h3>
 * <pre>{@code
 * "bossbar": {
 *   "color": "red",
 *   "style": "progress"
 * }
 * }</pre>
 *
 * <p>Both fields are optional. {@code color} defaults to {@code red};
 * {@code style} defaults to {@code progress}.
 *
 * <p>Valid {@code color} values: pink, blue, red, green, yellow, purple, white.<br>
 * Valid {@code style} values: progress, notched_6, notched_10, notched_12, notched_20.
 *
 * <p>Omitting the {@code "bossbar"} block entirely disables the bossbar (default).
 */
public record BossBarConfig(BossBar.Color color, BossBar.Style style) {}
