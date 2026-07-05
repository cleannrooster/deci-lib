package com.cleannrooster.decilib.builder;

import com.cleannrooster.decilib.ai.profile.AiProfile;
import com.cleannrooster.decilib.builder.archetype.Archetype;
import com.cleannrooster.decilib.builder.archetype.BaseStats;
import com.cleannrooster.decilib.builder.feature.BehaviorComposer;
import com.cleannrooster.decilib.builder.feature.BehaviorFeature;
import com.cleannrooster.decilib.builder.preset.Preset;
import com.cleannrooster.decilib.builder.registry.ArchetypeRegistry;
import com.cleannrooster.decilib.builder.registry.BuiltInRegistrations;
import com.cleannrooster.decilib.builder.registry.FeatureRegistry;
import com.cleannrooster.decilib.builder.registry.PresetRegistry;
import com.cleannrooster.decilib.builder.scale.ScaleProfile;
import com.cleannrooster.decilib.builder.tuning.TuningProfile;
import com.cleannrooster.decilib.builder.validation.MobProfileValidator;
import com.cleannrooster.decilib.builder.validation.ValidationResult;
import com.cleannrooster.decilib.builder.visual.Form;
import com.cleannrooster.decilib.builder.visual.VisualTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class MobBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(MobBuilder.class);

    private MobBuilder() {}


    public static MobDefinition build(MobProfile profile) {
        BuiltInRegistrations.register();

        // 1. Resolve preset
        MobProfile resolved = profile;
        if (profile.preset() != null) {
            Preset preset = PresetRegistry.get(profile.preset());
            if (preset == null) {
                throw new MobProfileException("Profile '" + profile.id()
                        + "': unknown preset '" + profile.preset()
                        + "'. Known presets: " + PresetRegistry.knownIds());
            }
            resolved = preset.apply(profile);
        }

        // 2. Validate — fail fast
        ValidationResult validation = MobProfileValidator.validate(resolved);
        if (validation.hasWarnings()) {
            for (String warning : validation.warnings()) {
                LOGGER.warn("[deci-lib] Profile '{}': {}", resolved.id(), warning);
            }
        }
        if (validation.hasErrors()) {
            var errors = String.join("\n  - ", validation.errors());
            throw new MobProfileException(
                    "Profile '" + resolved.id() + "' failed validation:\n  - " + errors);
        }

        // 3. Visual identity — apply defaults if omitted in JSON
        Form        form  = resolved.form()  != null ? resolved.form()  : Form.STANDARD_BIPED;
        VisualTheme theme = resolved.theme() != null ? resolved.theme() : VisualTheme.FANTASY;

        // 4. Tuning
        TuningProfile tuning = resolved.tuning() != null
                ? resolved.tuning()
                : TuningProfile.defaults();
        ScaleProfile scaleProfile = resolved.scaleProfile() != null
                ? resolved.scaleProfile()
                : ScaleProfile.defaults();

        // 5. Archetype
        Archetype archetype = ArchetypeRegistry.get(resolved.archetype());
        // Safe — validator already confirmed it exists

        // 5a. Resolve AiProfile — archetype default, overridden per-axis by JSON
        AiProfile resolvedAiProfile = archetype.defaultAiProfile();
        if (resolved.aiProfile() != null) {
            resolvedAiProfile = resolvedAiProfile.merge(
                    resolved.aiProfile().aggression(),
                    resolved.aiProfile().spatial(),
                    resolved.aiProfile().adaptation(),
                    resolved.aiProfile().targetEval());
        }

        // 6. Compose
        BehaviorComposer composer = new BehaviorComposer();
        composer.setAiProfile(resolvedAiProfile);  // must be set before archetype.apply()
        archetype.apply(composer, tuning);

        for (var fc : resolved.features()) {
            BehaviorFeature feature = FeatureRegistry.get(fc.type());
            // Safe — validator confirmed all feature types are known
            feature.apply(composer, fc, tuning);
        }

        // 7. Stats — tuning first, then any explicit attribute overrides
        BaseStats stats = archetype.baseStats(tuning);
        AttributeOverrides overrides = resolved.attributeOverrides();
        if (overrides != null) {
            stats = new BaseStats(
                    overrides.maxHealth()           != null ? overrides.maxHealth()           : stats.maxHealth(),
                    overrides.attackDamage()        != null ? overrides.attackDamage()        : stats.attackDamage(),
                    overrides.movementSpeed()       != null ? overrides.movementSpeed()       : stats.movementSpeed(),
                    overrides.followRange()         != null ? overrides.followRange()         : stats.followRange(),
                    overrides.knockbackResistance() != null ? overrides.knockbackResistance() : stats.knockbackResistance(),
                    overrides.meleeRange()          != null ? overrides.meleeRange()          : stats.meleeRange()
            );
        }

        return new MobDefinition(
                resolved.id(),
                resolved.archetype(),
                form,
                theme,
                stats,
                archetype.initialStance(),
                archetype.initialState(),
                composer,
                resolvedAiProfile,
                resolved.renderConfig(),
                resolved.soundConfig(),
                scaleProfile,
                resolved.bossBarConfig(),
                resolved.faction()
        );
    }
}
