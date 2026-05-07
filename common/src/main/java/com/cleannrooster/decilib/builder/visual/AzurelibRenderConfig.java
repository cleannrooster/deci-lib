package com.cleannrooster.decilib.builder.visual;

import org.jetbrains.annotations.Nullable;


public record AzurelibRenderConfig(
        String            geo,
        String            animation,
        String            texture,
        @Nullable LoopAnimSet loops
) {
    public AzurelibRenderConfig(String geo, String animation, String texture) {
        this(geo, animation, texture, null);
    }
}
