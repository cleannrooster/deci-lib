package com.cleannrooster.decilib.builder.preset;

import com.cleannrooster.decilib.builder.MobProfile;
import com.cleannrooster.decilib.builder.feature.FeatureConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BurrowerPreset implements Preset {

    @Override
    public String id() { return "burrower"; }

    @Override
    public MobProfile apply(MobProfile base) {
        String archetype = base.archetype() != null ? base.archetype() : "ambusher";

        var features = new ArrayList<FeatureConfig>(base.features());
        var hasAmbush = features.stream().anyMatch(f -> "ambush".equals(f.type()));
        if (!hasAmbush) {
            features.add(new FeatureConfig("ambush", Map.of()));
        }

        return new MobProfile(base.id(), archetype, null, base.tuning(), base.attributeOverrides(),
                base.form(), base.theme(), features, base.renderConfig(),base.soundConfig(),base.scaleProfile());
    }
}
