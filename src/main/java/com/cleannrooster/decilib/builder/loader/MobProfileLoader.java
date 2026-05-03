package com.cleannrooster.decilib.builder.loader;

import com.cleannrooster.decilib.builder.AttributeOverrides;
import com.cleannrooster.decilib.builder.MobProfile;
import com.cleannrooster.decilib.builder.MobProfileException;
import com.cleannrooster.decilib.builder.feature.AnimationEffect;
import com.cleannrooster.decilib.builder.feature.FeatureConfig;
import com.cleannrooster.decilib.builder.feature.FeatureHooks;
import com.cleannrooster.decilib.builder.feature.FeatureLifecycleEvent;
import com.cleannrooster.decilib.builder.feature.LifecycleEffect;
import com.cleannrooster.decilib.builder.feature.SoundEffect;
import com.cleannrooster.decilib.builder.scale.ScaleProfile;
import com.cleannrooster.decilib.builder.sound.SoundConfig;
import com.cleannrooster.decilib.builder.sound.SoundEntry;
import com.cleannrooster.decilib.builder.tuning.TuningBand;
import com.cleannrooster.decilib.builder.tuning.TuningProfile;
import com.cleannrooster.decilib.builder.visual.AzurelibRenderConfig;
import com.cleannrooster.decilib.builder.visual.Form;
import com.cleannrooster.decilib.builder.visual.LoopAnimSet;
import com.cleannrooster.decilib.builder.visual.VisualTheme;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public final class MobProfileLoader {

    private MobProfileLoader() {}


    public static MobProfile load(Path path) {
        try (Reader reader = Files.newBufferedReader(path)) {
            return load(reader);
        } catch (IOException e) {
            throw new MobProfileException("Failed to read mob profile from " + path, e);
        }
    }


    public static MobProfile load(Reader reader) {
        JsonObject json;
        try {
            json = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            throw new MobProfileException("Mob profile JSON is malformed: " + e.getMessage(), e);
        }

        if (!json.has("id")) {
            throw new MobProfileException("Mob profile is missing required 'id' field");
        }

        var id        = json.get("id").getAsString();
        String archetype = json.has("archetype") ? json.get("archetype").getAsString() : null;
        String preset    = json.has("preset")    ? json.get("preset").getAsString()    : null;

        TuningProfile tuning = null;
        if (json.has("tuning")) {
            JsonObject t = json.getAsJsonObject("tuning");
            tuning = new TuningProfile(
                    parseBand(t, "health",    id),
                    parseBand(t, "damage",    id),
                    parseBand(t, "speed",     id),
                    parseBand(t, "detection", id)
            );
        }
        ScaleProfile scaleProfile = null;

        if (json.has("scale")) {
            JsonObject t = json.getAsJsonObject("scale");
            scaleProfile = new ScaleProfile(
                    t.get("width").getAsFloat(),
                    t.get("height").getAsFloat()
            );
        }
        Form form = null;
        if (json.has("form")) {
            form = Form.parse(json.get("form").getAsString(), id);
        }

        VisualTheme theme = null;
        if (json.has("theme")) {
            theme = VisualTheme.parse(json.get("theme").getAsString(), id);
        }

        AzurelibRenderConfig renderConfig = null;
        if (json.has("render")) {
            JsonObject render = json.getAsJsonObject("render");
            if (!render.has("assets")) {
                throw new MobProfileException("Profile '" + id
                        + "': 'render' block is missing required 'assets' object");
            }
            JsonObject assets = render.getAsJsonObject("assets");
            for (String key : new String[]{"geo", "animation", "texture"}) {
                if (!assets.has(key)) {
                    throw new MobProfileException("Profile '" + id
                            + "': render.assets is missing required '" + key + "' field");
                }
            }
            LoopAnimSet loops = render.has("loops")
                    ? parseLoopAnimSet(render.getAsJsonObject("loops"), id)
                    : null;
            renderConfig = new AzurelibRenderConfig(
                    requireNamespacedAsset(assets, "geo",       id),
                    requireNamespacedAsset(assets, "animation", id),
                    requireNamespacedAsset(assets, "texture",   id),
                    loops
            );
        }

        AttributeOverrides attributeOverrides = null;
        if (json.has("attributes")) {
            JsonObject a = json.getAsJsonObject("attributes");
            attributeOverrides = new AttributeOverrides(
                    a.has("max_health")           ? a.get("max_health").getAsDouble()           : null,
                    a.has("attack_damage")        ? a.get("attack_damage").getAsDouble()        : null,
                    a.has("movement_speed")       ? a.get("movement_speed").getAsDouble()       : null,
                    a.has("follow_range")         ? a.get("follow_range").getAsDouble()         : null,
                    a.has("knockback_resistance") ? a.get("knockback_resistance").getAsDouble() : null,
                    a.has("melee_range")          ? a.get("melee_range").getAsDouble()          : null
            );
        }

        SoundConfig soundConfig = null;
        if (json.has("sounds")) {
            soundConfig = parseSoundConfig(json.getAsJsonObject("sounds"), id);
        }

        var features = new ArrayList<FeatureConfig>();
        if (json.has("features")) {
            var arr = json.getAsJsonArray("features");
            for (JsonElement elem : arr) {
                JsonObject fo = elem.getAsJsonObject();
                if (!fo.has("type")) {
                    throw new MobProfileException(
                            "Profile '" + id + "': feature entry is missing required 'type' field");
                }
                var type = fo.get("type").getAsString();
                var params = new LinkedHashMap<String, Object>();
                Map<FeatureLifecycleEvent, List<LifecycleEffect>> hooksMap = null;
                for (Map.Entry<String, JsonElement> entry : fo.entrySet()) {
                    if ("type".equals(entry.getKey())) continue;
                    if ("animations".equals(entry.getKey())) {
                        if (hooksMap == null) hooksMap = new EnumMap<>(FeatureLifecycleEvent.class);
                        mergeAnimationHooks(hooksMap, entry.getValue().getAsJsonObject());
                        continue;
                    }
                    if ("sounds".equals(entry.getKey())) {
                        if (hooksMap == null) hooksMap = new EnumMap<>(FeatureLifecycleEvent.class);
                        mergeSoundHooks(hooksMap, entry.getValue().getAsJsonObject());
                        continue;
                    }
                    params.put(entry.getKey(), toJavaValue(entry.getValue()));
                }
                FeatureHooks hooks = hooksMap != null ? buildFeatureHooks(hooksMap) : null;
                features.add(new FeatureConfig(type, params, hooks));
            }
        }

        return new MobProfile(id, archetype, preset, tuning, attributeOverrides, form, theme, features, renderConfig, soundConfig,scaleProfile);
    }

    // -------------------------------------------------------------------------

    private static String requireNamespacedAsset(JsonObject assets, String key, String profileId) {
        var value = assets.get(key).getAsString();
        if (!value.contains(":")) {
            throw new MobProfileException("Profile '" + profileId
                    + "': render.assets." + key + " must be a namespaced identifier"
                    + " (e.g. \"deci-lib:geo/rogue.geo.json\"), got \"" + value + "\"");
        }
        return value;
    }

    private static TuningBand parseBand(JsonObject obj, String key, String profileId) {
        if (!obj.has(key)) return TuningBand.MEDIUM;
        var raw = obj.get(key).getAsString().toUpperCase();
        try {
            return TuningBand.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new MobProfileException("Profile '" + profileId + "': invalid tuning band '"
                    + raw + "' for key '" + key + "'. Valid values: NONE, LOW, MEDIUM, HIGH");
        }
    }

    private static Object toJavaValue(JsonElement elem) {
        if (elem.isJsonNull())      return null;
        if (elem.isJsonPrimitive()) {
            var p = elem.getAsJsonPrimitive();
            if (p.isBoolean()) return p.getAsBoolean();
            if (p.isNumber())  return p.getAsDouble();
            return p.getAsString();
        }
        return elem.toString(); // objects/arrays serialised as-is (not used by current features)
    }

    private static LoopAnimSet parseLoopAnimSet(JsonObject obj, String profileId) {
        if (!obj.has("idle")) {
            throw new MobProfileException("Profile '" + profileId
                    + "': render.loops is missing required 'idle' animation");
        }
        return new LoopAnimSet(
                obj.get("idle").getAsString(),
                obj.has("idle_hostile")   ? obj.get("idle_hostile").getAsString()   : null,
                obj.has("moving")         ? obj.get("moving").getAsString()         : null,
                obj.has("moving_hostile") ? obj.get("moving_hostile").getAsString() : null,
                obj.has("running")        ? obj.get("running").getAsString()        : null,
                obj.has("aiming")         ? obj.get("aiming").getAsString()         : null
        );
    }

    private static final Map<String, FeatureLifecycleEvent> HOOK_KEY_MAP = Map.of(
            "on_start",    FeatureLifecycleEvent.START,
            "on_windup",   FeatureLifecycleEvent.WINDUP,
            "on_release",  FeatureLifecycleEvent.RELEASE,
            "on_action",   FeatureLifecycleEvent.ON_ACTION,
            "on_complete", FeatureLifecycleEvent.COMPLETE,
            "on_cancel",   FeatureLifecycleEvent.CANCEL
    );

    private static void mergeAnimationHooks(Map<FeatureLifecycleEvent, List<LifecycleEffect>> map,
                                            JsonObject obj) {
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            FeatureLifecycleEvent event = HOOK_KEY_MAP.get(entry.getKey());
            if (event != null) {
                map.computeIfAbsent(event, k -> new ArrayList<>())
                   .add(new AnimationEffect(entry.getValue().getAsString()));
            }
        }
    }

    private static void mergeSoundHooks(Map<FeatureLifecycleEvent, List<LifecycleEffect>> map,
                                        JsonObject obj) {
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            FeatureLifecycleEvent event = HOOK_KEY_MAP.get(entry.getKey());
            if (event != null) {
                map.computeIfAbsent(event, k -> new ArrayList<>())
                   .add(new SoundEffect(parseSoundEntry(entry.getValue())));
            }
        }
    }

    private static FeatureHooks buildFeatureHooks(Map<FeatureLifecycleEvent, List<LifecycleEffect>> mutable) {
        var frozen = new EnumMap<FeatureLifecycleEvent, List<LifecycleEffect>>(FeatureLifecycleEvent.class);
        mutable.forEach((k, v) -> frozen.put(k, List.copyOf(v)));
        return new FeatureHooks(frozen);
    }

    // -------------------------------------------------------------------------
    // Sound parsing
    // -------------------------------------------------------------------------

    private static SoundConfig parseSoundConfig(JsonObject obj, String profileId) {
        return new SoundConfig(
                obj.has("idle")   ? parseSoundEntry(obj.get("idle"))   : null,
                obj.has("hurt")   ? parseSoundEntry(obj.get("hurt"))   : null,
                obj.has("death")  ? parseSoundEntry(obj.get("death"))  : null,
                obj.has("step")   ? parseSoundEntry(obj.get("step"))   : null,
                obj.has("attack") ? parseSoundEntry(obj.get("attack")) : null
        );
    }

    private static SoundEntry parseSoundEntry(JsonElement elem) {
        if (elem.isJsonPrimitive()) {
            return SoundEntry.of(elem.getAsString());
        }
        JsonObject obj = elem.getAsJsonObject();
        if (!obj.has("id")) {
            throw new MobProfileException("Sound entry object is missing required 'id' field");
        }
        float volume = obj.has("volume") ? obj.get("volume").getAsFloat() : SoundEntry.DEFAULT_VOLUME;
        float pitch  = obj.has("pitch")  ? obj.get("pitch").getAsFloat()  : SoundEntry.DEFAULT_PITCH;
        return new SoundEntry(obj.get("id").getAsString(), volume, pitch);
    }
}
