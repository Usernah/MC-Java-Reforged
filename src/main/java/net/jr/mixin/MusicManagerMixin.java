package net.jr.mixin;

import net.jr.client.sound.bridge.MusicManagerBridge;
import net.jr.client.sound.music.MusicPlaybackManager;
import net.jr.client.sound.music.MusicPlaybackState;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MusicManager.class)
public abstract class MusicManagerMixin implements MusicManagerBridge {
    @Unique private final MusicPlaybackState javaReforged$playbackState = new MusicPlaybackState();

    @Override public void javaReforged$setStopGraceTicks(int ticks) {
        MusicPlaybackManager.setStopGraceTicks(this.javaReforged$playbackState, ticks);
    }
    @Override public void javaReforged$setLoadingMode(boolean loading) {
        MusicPlaybackManager.setLoadingMode(this.javaReforged$manager(), this.javaReforged$playbackState, loading);
    }
    @Override public void javaReforged$resetAfterWorldExit() {
        MusicPlaybackManager.resetAfterWorldExit(this.javaReforged$manager(), this.javaReforged$playbackState);
    }
    @Override public void javaReforged$fadeOutCurrentMusic() {
        MusicPlaybackManager.fadeOutCurrent(this.javaReforged$manager(), this.javaReforged$playbackState);
    }
    @Override public boolean javaReforged$intrudeMusic(@NotNull Music music) {
        return MusicPlaybackManager.intrude(this.javaReforged$manager(), this.javaReforged$playbackState, music);
    }
    @Override public boolean javaReforged$isCurrentlyPlaying(SoundEvent event) {
        return MusicPlaybackManager.isPlaying(this.javaReforged$manager(), event);
    }
    @Override public Component javaReforged$getCurrentMusicName() {
        return MusicPlaybackManager.currentName(this.javaReforged$manager());
    }

    @Overwrite public void tick() {
        MusicPlaybackManager.tick(this.javaReforged$manager(), this.javaReforged$playbackState);
    }
    @Overwrite public void startPlaying(Music music) {
        MusicPlaybackManager.startPlaying(this.javaReforged$manager(), this.javaReforged$playbackState, music);
    }

    @Unique private MusicManager javaReforged$manager() { return (MusicManager) (Object) this; }
}
