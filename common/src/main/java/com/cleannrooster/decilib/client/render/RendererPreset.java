package com.cleannrooster.decilib.client.render;

import com.cleannrooster.decilib.builder.entity.DataDrivenMob;
import com.cleannrooster.decilib.client.render.mob.AgileBipedRenderer;
import com.cleannrooster.decilib.client.render.mob.AlienBipedRenderer;
import com.cleannrooster.decilib.client.render.mob.ArcaneBipedRenderer;
import com.cleannrooster.decilib.client.render.mob.ArmoredBipedRenderer;
import com.cleannrooster.decilib.client.render.mob.HeavyBipedRenderer;
import com.cleannrooster.decilib.client.render.mob.HeavyQuadrupedRenderer;
import com.cleannrooster.decilib.client.render.mob.SmallQuadrupedRenderer;
import com.cleannrooster.decilib.client.render.mob.SpectralQuadrupedRenderer;
import com.cleannrooster.decilib.client.render.mob.StalkerQuadrupedRenderer;
import com.cleannrooster.decilib.client.render.mob.StandardBipedRenderer;
import com.cleannrooster.decilib.client.render.mob.StandardQuadrupedRenderer;
import com.cleannrooster.decilib.client.render.mob.SwiftQuadrupedRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;

import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum RendererPreset {

    // ── Biped ────────────────────────────────────────────────────────────────
    AGILE_BIPED       (AgileBipedRenderer::new),
    ARMORED_BIPED     (ArmoredBipedRenderer::new),
    ARCANE_BIPED      (ArcaneBipedRenderer::new),
    STANDARD_BIPED    (StandardBipedRenderer::new),
    HEAVY_BIPED       (HeavyBipedRenderer::new),
    ALIEN_BIPED       (AlienBipedRenderer::new),

    // ── Quadruped ────────────────────────────────────────────────────────────
    HEAVY_QUADRUPED   (HeavyQuadrupedRenderer::new),
    STANDARD_QUADRUPED(StandardQuadrupedRenderer::new),
    SPECTRAL_QUADRUPED(SpectralQuadrupedRenderer::new),
    STALKER_QUADRUPED (StalkerQuadrupedRenderer::new),
    SWIFT_QUADRUPED   (SwiftQuadrupedRenderer::new),
    SMALL_QUADRUPED   (SmallQuadrupedRenderer::new);

    // ─────────────────────────────────────────────────────────────────────────

    private final Function<EntityRendererFactory.Context, EntityRenderer<DataDrivenMob>> factory;

    RendererPreset(Function<EntityRendererFactory.Context, ? extends EntityRenderer<DataDrivenMob>> factory) {
        @SuppressWarnings("unchecked")
        Function<EntityRendererFactory.Context, EntityRenderer<DataDrivenMob>> typed =
                (Function<EntityRendererFactory.Context, EntityRenderer<DataDrivenMob>>) (Function<?, ?>) factory;
        this.factory = typed;
    }

    public EntityRenderer<DataDrivenMob> create(EntityRendererFactory.Context ctx) {
        return factory.apply(ctx);
    }
}
