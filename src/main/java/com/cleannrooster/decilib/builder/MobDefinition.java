package com.cleannrooster.decilib.builder;

import com.cleannrooster.decilib.ai.profile.AiProfile;
import com.cleannrooster.decilib.builder.archetype.BaseStats;
import com.cleannrooster.decilib.builder.feature.BehaviorComposer;
import com.cleannrooster.decilib.builder.scale.ScaleProfile;
import com.cleannrooster.decilib.builder.sound.SoundConfig;
import com.cleannrooster.decilib.builder.visual.AzurelibRenderConfig;
import com.cleannrooster.decilib.builder.visual.Form;
import com.cleannrooster.decilib.builder.visual.VisualTheme;
import org.jetbrains.annotations.Nullable;


public record MobDefinition(
        String           id,
        String           archetype,
        Form             form,
        VisualTheme      theme,
        BaseStats        stats,
        MobStance        initialStance,
        MobState         initialState,
        BehaviorComposer composer,
        AiProfile        aiProfile,
        @Nullable AzurelibRenderConfig renderConfig,
        @Nullable SoundConfig          soundConfig,
        @Nullable ScaleProfile scaleProfile
) {}
