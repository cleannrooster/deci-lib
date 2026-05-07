package com.cleannrooster.decilib.builder.validation;

import com.cleannrooster.decilib.builder.AttributeOverrides;
import com.cleannrooster.decilib.builder.MobProfile;
import com.cleannrooster.decilib.builder.archetype.Archetype;
import com.cleannrooster.decilib.builder.feature.BehaviorFeature;
import com.cleannrooster.decilib.builder.feature.FeatureConfig;
import com.cleannrooster.decilib.builder.registry.ArchetypeRegistry;
import com.cleannrooster.decilib.builder.registry.FeatureRegistry;
import com.cleannrooster.decilib.builder.visual.Form;


public final class MobProfileValidator {

    private MobProfileValidator() {}


    public static ValidationResult validate(MobProfile profile) {
        ValidationResult result = new ValidationResult();

        if (profile.id() == null || profile.id().isBlank()) {
            result.error("Profile is missing a required 'id' field");
            return result;
        }

        // AzureLib form requires a render block; render block requires AZURELIB form
        if (profile.form() == Form.AZURELIB && profile.renderConfig() == null) {
            result.error("Profile '" + profile.id()
                    + "': form 'azurelib' requires a 'render.assets' block with 'geo', 'animation', and 'texture'");
        }
        if (profile.renderConfig() != null && profile.form() != Form.AZURELIB) {
            result.error("Profile '" + profile.id()
                    + "': a 'render' block is only valid when form is 'azurelib'");
        }

        if (profile.archetype() == null) {
            result.error("Profile '" + profile.id()
                    + "' is missing required 'archetype' field"
                    + " (or a 'preset' that supplies one)");
            return result;
        }

        Archetype archetype = ArchetypeRegistry.get(profile.archetype());
        if (archetype == null) {
            result.error("Profile '" + profile.id() + "': unknown archetype '"
                    + profile.archetype() + "'. Known archetypes: "
                    + ArchetypeRegistry.knownIds());
            return result;
        }

        // Required features
        for (String required : archetype.requiredFeatures()) {
            var present = profile.features().stream()
                    .anyMatch(f -> required.equals(f.type()));
            if (!present) {
                result.error("Profile '" + profile.id() + "': archetype '"
                        + archetype.id() + "' requires feature '" + required
                        + "' but it is not present in the features list");
            }
        }

        // Incompatible features
        for (String incompatible : archetype.incompatibleFeatures()) {
            var present = profile.features().stream()
                    .anyMatch(f -> incompatible.equals(f.type()));
            if (present) {
                result.error("Profile '" + profile.id() + "': feature '"
                        + incompatible + "' is incompatible with archetype '"
                        + archetype.id() + "'");
            }
        }

        // Attribute overrides
        AttributeOverrides attrs = profile.attributeOverrides();
        if (attrs != null) {
            if (attrs.maxHealth() != null && attrs.maxHealth() <= 0)
                result.error("Profile '" + profile.id() + "': attributes.max_health must be > 0");
            if (attrs.attackDamage() != null && attrs.attackDamage() < 0)
                result.error("Profile '" + profile.id() + "': attributes.attack_damage must be >= 0");
            if (attrs.movementSpeed() != null && attrs.movementSpeed() <= 0)
                result.error("Profile '" + profile.id() + "': attributes.movement_speed must be > 0");
            if (attrs.followRange() != null && attrs.followRange() <= 0)
                result.error("Profile '" + profile.id() + "': attributes.follow_range must be > 0");
            if (attrs.knockbackResistance() != null
                    && (attrs.knockbackResistance() < 0 || attrs.knockbackResistance() > 1))
                result.error("Profile '" + profile.id()
                        + "': attributes.knockback_resistance must be in [0.0, 1.0]");
        }

        // Per-feature validation
        for (FeatureConfig fc : profile.features()) {
            BehaviorFeature feature = FeatureRegistry.get(fc.type());
            if (feature == null) {
                result.error("Profile '" + profile.id() + "': unknown feature type '"
                        + fc.type() + "' (not yet implemented). Known features: "
                        + FeatureRegistry.knownIds());
            } else {
                feature.validate(fc, profile, result);
            }
        }

        return result;
    }
}
