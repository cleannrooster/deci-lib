package com.cleannrooster.decilib.builder;

import com.cleannrooster.decilib.builder.feature.FeatureConfig;
import com.cleannrooster.decilib.builder.scale.ScaleProfile;
import com.cleannrooster.decilib.builder.sound.SoundConfig;
import com.cleannrooster.decilib.builder.tuning.TuningProfile;
import com.cleannrooster.decilib.builder.visual.AzurelibRenderConfig;
import com.cleannrooster.decilib.builder.visual.Form;
import com.cleannrooster.decilib.builder.visual.VisualTheme;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public record MobProfile(
        String                       id,
        @Nullable String             archetype,
        @Nullable String             preset,
        @Nullable TuningProfile      tuning,
        @Nullable AttributeOverrides attributeOverrides,
        @Nullable Form               form,
        @Nullable VisualTheme        theme,
        List<FeatureConfig>          features,
        @Nullable AzurelibRenderConfig renderConfig,
        @Nullable SoundConfig        soundConfig,
        @Nullable ScaleProfile       scaleProfile
        ) {
    public MobProfile {
        features = List.copyOf(features);
    }
}
