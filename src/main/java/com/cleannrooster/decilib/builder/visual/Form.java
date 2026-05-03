package com.cleannrooster.decilib.builder.visual;

import com.cleannrooster.decilib.builder.MobProfileException;


public enum Form {

    // ── Biped forms ──────────────────────────────────────────────────────────
    AGILE_BIPED,
    ARMORED_BIPED,
    ARCANE_BIPED,
    STANDARD_BIPED,
    HEAVY_BIPED,
    ALIEN_BIPED,

    // ── Quadruped forms ──────────────────────────────────────────────────────
    HEAVY_QUADRUPED,
    STANDARD_QUADRUPED,
    SPECTRAL_QUADRUPED,
    STALKER_QUADRUPED,
    SWIFT_QUADRUPED,
    SMALL_QUADRUPED,

    // ── Custom / external renderer ────────────────────────────────────────────
    /** Delegates rendering to AzureLib. Requires the AzureLib mod and a {@code render} block in the profile. */
    AZURELIB;

    /**
     * Parses a JSON string to a {@code Form}, case-insensitively.
     *
     * @throws MobProfileException with an actionable message on unknown values
     */
    public static Form parse(String value, String profileId) {
        try {
            return Form.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            StringBuilder valid = new StringBuilder();
            for (Form f : values()) {
                if (valid.length() > 0) valid.append(", ");
                valid.append(f.name().toLowerCase());
            }
            throw new MobProfileException("Profile '" + profileId
                    + "': unknown form '" + value
                    + "'. Valid values: " + valid);
        }
    }
}
