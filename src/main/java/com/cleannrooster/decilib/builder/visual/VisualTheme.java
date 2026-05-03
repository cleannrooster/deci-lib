package com.cleannrooster.decilib.builder.visual;

import com.cleannrooster.decilib.builder.MobProfileException;

/**
 * Visual identity theme that selects the texture for a mob's {@link Form}.
 *
 * <p>The {@link com.cleannrooster.decilib.client.render.TextureTable} maps
 * {@code (Form, VisualTheme)} to a texture identifier at render time.
 *
 * <p>JSON value: one of {@code sand | iron | magic | fantasy | beast | alien}
 * (case-insensitive). Defaults to {@code FANTASY} when omitted.
 */
public enum VisualTheme {

    SAND,

    IRON,

    MAGIC,

    FANTASY,

    BEAST,

    ALIEN;


    public static VisualTheme parse(String value, String profileId) {
        try {
            return VisualTheme.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new MobProfileException("Profile '" + profileId
                    + "': unknown theme '" + value
                    + "'. Valid values: sand, iron, magic, fantasy, beast, alien");
        }
    }
}
