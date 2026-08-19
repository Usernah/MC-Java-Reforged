package net.jr.client.sound.bridge;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.NotNull;

public interface MusicManagerBridge {
    boolean moods$intrudeMusic(@NotNull Music music);

    boolean moods$isCurrentlyPlaying(SoundEvent soundEvent);

    Component moods$getCurrentMusicName();

    void moods$setStopGraceTicks(int ticks);

    void moods$setLoadingMode(boolean loading);

    void moods$fadeOutCurrentMusic();

    void moods$resetAfterWorldExit();
}
