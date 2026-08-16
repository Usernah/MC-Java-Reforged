package net.jr.mixin.runtime;

import java.util.List;
import java.util.Map;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SoundEngine.class)
public interface SoundEngineActiveSoundsAccessor {
    @Accessor("instanceToChannel")
    Map<SoundInstance, ChannelAccess.ChannelHandle> splitTest$instanceToChannel();

    @Accessor("queuedSounds")
    Map<SoundInstance, Integer> splitTest$queuedSounds();

    @Accessor("queuedTickableSounds")
    List<TickableSoundInstance> splitTest$queuedTickableSounds();
}
