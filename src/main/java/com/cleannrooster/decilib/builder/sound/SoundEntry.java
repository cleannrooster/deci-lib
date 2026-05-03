package com.cleannrooster.decilib.builder.sound;


public record SoundEntry(String soundId, float volume, float pitch) {

    public static final float DEFAULT_VOLUME = 1.0f;
    public static final float DEFAULT_PITCH  = 1.0f;

    public static SoundEntry of(String soundId) {
        return new SoundEntry(soundId, DEFAULT_VOLUME, DEFAULT_PITCH);
    }
}
