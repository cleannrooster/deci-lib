package com.cleannrooster.decilib.ai.ambush;

import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public interface CanAmbush {

    /**
     * Transitions the entity into its hidden/latent state.
     *
     * Implementations typically make the entity invisible, invulnerable, or
     * otherwise inactive.
     *
     */
    void enterHiddenState();

    /**
     * Transitions the entity out of its hidden/latent state.
     */
    void exitHiddenState();

    /**
     * Returns {@code true} while the entity is currently in its hidden/latent
     * state.
     */
    boolean isHidden();

    /**
     * Returns the world position the entity should teleport to when surfacing,
     * or null to surface at the current position.
     */
    @Nullable Vec3d getSurfacePosition();
}
