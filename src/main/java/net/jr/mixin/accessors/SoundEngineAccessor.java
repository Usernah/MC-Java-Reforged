package net.jr.mixin.accessors;

import java.util.List;
import java.util.Map;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SoundEngine.class)
public interface SoundEngineAccessor {
    @Accessor("loaded")
    boolean javaReforged$isLoaded();

    @Accessor("instanceToChannel")
    Map<SoundInstance, ChannelAccess.ChannelHandle> javaReforged$instanceToChannel();

    @Accessor("queuedSounds")
    Map<SoundInstance, Integer> javaReforged$queuedSounds();

    @Accessor("queuedTickableSounds")
    List<TickableSoundInstance> javaReforged$queuedTickableSounds();

    @Invoker("calculateVolume")
    float javaReforged$calculateVolume(SoundInstance sound);

    @Invoker("calculatePitch")
    float javaReforged$calculatePitch(SoundInstance sound);
}
