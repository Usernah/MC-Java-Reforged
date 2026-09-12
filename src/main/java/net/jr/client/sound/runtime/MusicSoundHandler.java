package net.jr.client.sound.runtime;

import com.mojang.blaze3d.audio.Channel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import net.jr.client.sound.config.SoundTransitionConfig;
import net.jr.client.sound.music.MusicSoundInstance;
import net.jr.client.sound.music.MusicTransitionManager;
import net.jr.mixin.accessors.SoundEngineAccessor;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

public final class MusicSoundHandler {
    private MusicSoundHandler() {
    }

    public static boolean protectTransitionStop() {
        return MusicTransitionManager.shouldProtectWorldLoadAudio();
    }

    public static @Nullable SoundEngine.PlayResult interceptPlay(SoundEngine engine, SoundInstance sound) {
        SoundEngineAccessor accessor = accessor(engine);
        if (sound == null || sound instanceof MusicSoundInstance || !accessor.javaReforged$isLoaded()
                || sound.getSource() != SoundSource.MUSIC) return null;
        return engine.play(new MusicSoundInstance(sound, (float) SoundTransitionConfig.fadeInTicks));
    }

    public static boolean fadeManagedStop(SoundEngine engine, SoundInstance sound) {
        if (sound == null || !accessor(engine).javaReforged$isLoaded()
                || !(sound instanceof MusicSoundInstance music)) return false;
        if (SoundTransitionConfig.fadeOutTicks <= 0 || music.isStopped() || music.getDirectVolume() <= 0.0F) return false;
        music.setFadeOut((float) SoundTransitionConfig.fadeOutTicks);
        return true;
    }

    public static void tickPaused(SoundEngine engine, PausedMusicState state, boolean paused) {
        if (!paused || SoundTransitionConfig.allowPausingMusic) return;
        SoundEngineAccessor accessor = accessor(engine);
        Iterator<TickableSoundInstance> iterator = state.tickingSounds().iterator();
        while (iterator.hasNext()) {
            TickableSoundInstance sound = iterator.next();
            if (!sound.canPlaySound()) {
                engine.stop(sound);
                iterator.remove();
                continue;
            }
            sound.tick();
            if (sound.isStopped()) {
                engine.stop(sound);
                iterator.remove();
                continue;
            }
            ChannelAccess.ChannelHandle handle = accessor.javaReforged$instanceToChannel().get(sound);
            if (handle == null) {
                iterator.remove();
                continue;
            }
            float volume = accessor.javaReforged$calculateVolume(sound);
            float pitch = accessor.javaReforged$calculatePitch(sound);
            handle.execute(channel -> {
                channel.setVolume(volume);
                channel.setPitch(pitch);
            });
        }
    }

    public static boolean pauseExcept(SoundEngine engine, PausedMusicState state) {
        if (SoundTransitionConfig.allowPausingMusic) return false;
        state.clear();
        for (Map.Entry<SoundInstance, ChannelAccess.ChannelHandle> entry
                : new ArrayList<>(accessor(engine).javaReforged$instanceToChannel().entrySet())) {
            if (entry.getKey() instanceof MusicSoundInstance music) state.tickingSounds().add(music);
            else if (entry.getValue() != null) entry.getValue().execute(Channel::pause);
        }
        return true;
    }

    public static void stopMusic(SoundEngine engine) {
        SoundEngineAccessor accessor = accessor(engine);
        if (!accessor.javaReforged$isLoaded()) return;
        for (Map.Entry<SoundInstance, ChannelAccess.ChannelHandle> entry
                : new ArrayList<>(accessor.javaReforged$instanceToChannel().entrySet())) {
            if (entry.getKey() instanceof MusicSoundInstance && entry.getValue() != null) {
                entry.getValue().execute(Channel::stop);
            }
        }
    }

    public static void fadeSounds(SoundEngine engine, float ticks) {
        SoundEngineAccessor accessor = accessor(engine);
        if (!accessor.javaReforged$isLoaded()) return;
        for (SoundInstance sound : new ArrayList<>(accessor.javaReforged$instanceToChannel().keySet())) {
            if (sound instanceof MusicSoundInstance music) music.setFadeOut(ticks);
        }
    }

    public static void clearQueued(SoundEngine engine) {
        accessor(engine).javaReforged$queuedSounds().clear();
        accessor(engine).javaReforged$queuedTickableSounds().clear();
    }

    public static boolean hasFadingMusic(SoundEngine engine) {
        SoundEngineAccessor accessor = accessor(engine);
        if (!accessor.javaReforged$isLoaded()) return false;
        return accessor.javaReforged$instanceToChannel().keySet().stream()
                .anyMatch(sound -> sound instanceof MusicSoundInstance music && music.isFadingOut());
    }

    public static boolean hasManagedMusic(SoundEngine engine) {
        SoundEngineAccessor accessor = accessor(engine);
        if (!accessor.javaReforged$isLoaded()) return false;
        return accessor.javaReforged$instanceToChannel().keySet().stream()
                .anyMatch(MusicSoundInstance.class::isInstance);
    }

    private static SoundEngineAccessor accessor(SoundEngine engine) { return (SoundEngineAccessor) engine; }
}
