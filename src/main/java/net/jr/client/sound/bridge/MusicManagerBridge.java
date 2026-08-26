package net.jr.client.sound.bridge;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.NotNull;

public interface MusicManagerBridge {
    boolean javaReforged$intrudeMusic(@NotNull Music music);

    boolean javaReforged$isCurrentlyPlaying(SoundEvent soundEvent);

    Component javaReforged$getCurrentMusicName();

    void javaReforged$setStopGraceTicks(int ticks);

    void javaReforged$setLoadingMode(boolean loading);

    void javaReforged$fadeOutCurrentMusic();

    void javaReforged$resetAfterWorldExit();
}
