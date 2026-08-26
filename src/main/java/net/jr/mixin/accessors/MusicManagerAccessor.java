package net.jr.mixin.accessors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MusicManager.class)
public interface MusicManagerAccessor {
    @Accessor("minecraft")
    Minecraft javaReforged$getMinecraft();

    @Accessor("random")
    RandomSource javaReforged$getRandom();

    @Accessor("currentMusic")
    @Nullable SoundInstance javaReforged$getCurrentMusic();

    @Accessor("currentMusic")
    void javaReforged$setCurrentMusic(@Nullable SoundInstance music);

    @Accessor("nextSongDelay")
    int javaReforged$getNextSongDelay();

    @Accessor("nextSongDelay")
    void javaReforged$setNextSongDelay(int delay);
}
