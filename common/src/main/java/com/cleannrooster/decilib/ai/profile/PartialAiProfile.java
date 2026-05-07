package com.cleannrooster.decilib.ai.profile;

import org.jetbrains.annotations.Nullable;

/**
 * Holds raw per-axis values parsed from a mob JSON {@code ai_profile} block.
 * Any axis not present in the JSON is {@code null}, indicating that the
 * archetype's default for that axis should be used.
 *
 * @see AiProfile#merge
 */
public record PartialAiProfile(
        @Nullable AggressionModel aggression,
        @Nullable SpatialModel    spatial,
        @Nullable AdaptationModel adaptation,
        @Nullable TargetEvalModel targetEval
) {}
