package com.cleannrooster.decilib.builder.feature;

import com.cleannrooster.decilib.builder.entity.DataDrivenMob;
import com.cleannrooster.decilib.builder.sound.SoundEntry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;


public record SoundEffect(SoundEntry entry) implements LifecycleEffect {

    @Override
    public void fire(LivingEntity entity, @Nullable ServerWorld world) {
        if (world == null) return;
        SoundEvent event = SoundEvent.of(Identifier.of(entry.soundId()));
        world.playSound(null, entity.getBlockPos(), event, SoundCategory.HOSTILE,
                entry.volume(), entry.pitch());
    }
}
