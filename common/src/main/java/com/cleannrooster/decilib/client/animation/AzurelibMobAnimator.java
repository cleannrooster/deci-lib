package com.cleannrooster.decilib.client.animation;

import com.cleannrooster.decilib.builder.entity.DataDrivenMob;
import com.cleannrooster.decilib.builder.visual.AzurelibRenderConfig;

import mod.azure.azurelib.common.animation.controller.AzAnimationController;
import mod.azure.azurelib.common.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.common.animation.impl.AzEntityAnimator;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class AzurelibMobAnimator extends AzEntityAnimator<DataDrivenMob> {

    @Override
    public void registerControllers(AzAnimationControllerContainer<DataDrivenMob> container) {
        container.add(AzAnimationController.builder(this, "movement_loop").setTransitionLength(2).build());
    }

    @Override
    public @NotNull Identifier getAnimationLocation(DataDrivenMob entity) {
        var rc = entity.getDefinition().renderConfig();
        if (rc == null) throw new IllegalStateException(
                "[deci-lib] Entity '" + entity.getDefinition().id()
                        + "' is using AzurelibMobAnimator but has no renderConfig.");
        return Identifier.of(rc.animation());
    }
}
