package com.cleannrooster.decilib.client.render;

import com.cleannrooster.decilib.builder.visual.Form;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import static com.cleannrooster.decilib.builder.visual.Form.*;

public final class RendererResolver {

    private static final Map<Form, RendererPreset> TABLE = buildTable();

    private RendererResolver() {}


    public static RendererPreset resolve(Form form) {
        var preset = TABLE.get(form);
        if (preset == null) {
            throw new IllegalStateException("[deci-lib] RendererResolver: no preset for Form."
                    + form + ". Add it to RendererResolver.buildTable().");
        }
        return preset;
    }

    // ─────────────────────────────────────────────────────────────────────────

    private static Map<Form, RendererPreset> buildTable() {
        var t = new EnumMap<Form, RendererPreset>(Form.class);
        t.put(AGILE_BIPED,        RendererPreset.AGILE_BIPED);
        t.put(ARMORED_BIPED,      RendererPreset.ARMORED_BIPED);
        t.put(ARCANE_BIPED,       RendererPreset.ARCANE_BIPED);
        t.put(STANDARD_BIPED,     RendererPreset.STANDARD_BIPED);
        t.put(HEAVY_BIPED,        RendererPreset.HEAVY_BIPED);
        t.put(ALIEN_BIPED,        RendererPreset.ALIEN_BIPED);
        t.put(HEAVY_QUADRUPED,    RendererPreset.HEAVY_QUADRUPED);
        t.put(STANDARD_QUADRUPED, RendererPreset.STANDARD_QUADRUPED);
        t.put(SPECTRAL_QUADRUPED, RendererPreset.SPECTRAL_QUADRUPED);
        t.put(STALKER_QUADRUPED,  RendererPreset.STALKER_QUADRUPED);
        t.put(SWIFT_QUADRUPED,    RendererPreset.SWIFT_QUADRUPED);
        t.put(SMALL_QUADRUPED,    RendererPreset.SMALL_QUADRUPED);
        return Collections.unmodifiableMap(t);
    }
}
