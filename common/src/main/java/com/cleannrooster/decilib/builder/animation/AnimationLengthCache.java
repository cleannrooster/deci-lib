package com.cleannrooster.decilib.builder.animation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side lookup of {@code animation_length} values from bundled AzureLib
 * animation JSONs, so one-shot triggers know how long to suppress the movement
 * loop. Reads the file off the classpath ({@code /assets/<ns>/<path>}); if the
 * file or animation can't be found, falls back to {@link #DEFAULT_TICKS}.
 */
public final class AnimationLengthCache {

    private static final int DEFAULT_TICKS = 20;

    private static final Map<String, Map<String, Integer>> CACHE = new ConcurrentHashMap<>();

    private AnimationLengthCache() {}

    /** Length of {@code animationName} in ticks, per the given animation file id
     *  (e.g. {@code "decilib:animations/rogue.animations.json"}). */
    public static int lengthTicks(String animationFile, String animationName) {
        return CACHE.computeIfAbsent(animationFile, AnimationLengthCache::parse)
                    .getOrDefault(animationName, DEFAULT_TICKS);
    }

    private static Map<String, Integer> parse(String animationFile) {
        Map<String, Integer> lengths = new HashMap<>();
        String[] parts = animationFile.split(":", 2);
        if (parts.length != 2) return lengths;
        String resource = "/assets/" + parts[0] + "/" + parts[1];

        try (var in = AnimationLengthCache.class.getResourceAsStream(resource)) {
            if (in == null) return lengths;
            JsonObject root = JsonParser
                    .parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            JsonObject anims = root.getAsJsonObject("animations");
            if (anims == null) return lengths;
            for (Map.Entry<String, JsonElement> e : anims.entrySet()) {
                if (!e.getValue().isJsonObject()) continue;
                JsonElement len = e.getValue().getAsJsonObject().get("animation_length");
                if (len != null && len.isJsonPrimitive()) {
                    lengths.put(e.getKey(), (int) Math.ceil(len.getAsDouble() * 20.0));
                }
            }
        } catch (Exception ignored) {
            // Malformed or unreadable file: defaults apply.
        }
        return lengths;
    }
}
