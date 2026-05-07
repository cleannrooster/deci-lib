package com.cleannrooster.decilib.builder.scale;

public record ScaleProfile( float width, float height)  {
    public static ScaleProfile defaults() {
        return new ScaleProfile(0.6F,1.8F);
    }
}
