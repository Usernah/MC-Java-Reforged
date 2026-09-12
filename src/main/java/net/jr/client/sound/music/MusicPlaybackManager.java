package net.jr.client.sound.music;

import net.jr.client.sound.bridge.SoundEngineBridge;
import net.jr.client.sound.config.SoundTransitionConfig;
import net.jr.mixin.accessors.MusicManagerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MusicPlaybackManager {
    private static final int RETRY_DELAY_TICKS = 20;

    private MusicPlaybackManager() {
    }

    public static void setStopGraceTicks(MusicPlaybackState state, int ticks) {
        state.stopGraceTicks = Math.max(0, ticks);
    }

    public static void setLoadingMode(MusicManager manager, MusicPlaybackState state, boolean loading) {
        if (loading) {
            state.resumeAfterWorldExit = false;
            state.worldExitIntervalArmed = false;
        }
        if (state.loadingMode && !loading) {
            state.fadeInNextTrack = true;
            releaseProtectedFadeDelay(manager, state);
        }
        state.loadingMode = loading;
    }

    public static void resetAfterWorldExit(MusicManager manager, MusicPlaybackState state) {
        MusicManagerAccessor accessor = accessor(manager);
        state.loadingMode = false;
        state.stopGraceTicks = 0;
        state.fadeInNextTrack = true;
        state.configuredStartDelayArmed = false;
        state.resumeAfterWorldExit = true;
        state.initialPlayback = false;
        state.worldExitIntervalArmed = false;
        SoundInstance current = accessor.javaReforged$getCurrentMusic();
        if (current != null) {
            SoundManager sounds = accessor.javaReforged$getMinecraft().getSoundManager();
            if (current instanceof MusicSoundInstance music && !music.isStopped()
                    && music.getDirectVolume() > 0.0F && SoundTransitionConfig.fadeOutTicks > 0) {
                music.setFadeOut((float) SoundTransitionConfig.fadeOutTicks);
            } else {
                sounds.stop(current);
            }
        }
        accessor.javaReforged$setCurrentMusic(null);
        accessor.javaReforged$setNextSongDelay(0);
    }

    public static void fadeOutCurrent(MusicManager manager, MusicPlaybackState state) {
        MusicManagerAccessor accessor = accessor(manager);
        SoundInstance current = accessor.javaReforged$getCurrentMusic();
        if (current == null) return;
        if (current instanceof MusicSoundInstance music) {
            music.setFadeOut((float) SoundTransitionConfig.fadeOutTicks);
        } else {
            accessor.javaReforged$getMinecraft().getSoundManager().stop(current);
        }
        state.stopGraceTicks = Math.max(state.stopGraceTicks, SoundTransitionConfig.fadeOutTicks);
        accessor.javaReforged$setNextSongDelay(Integer.MAX_VALUE);
    }

    public static boolean intrude(MusicManager manager, MusicPlaybackState state, @NotNull Music music) {
        MusicManagerAccessor accessor = accessor(manager);
        SoundManager sounds = accessor.javaReforged$getMinecraft().getSoundManager();
        SoundInstance current = accessor.javaReforged$getCurrentMusic();
        if (current != null) {
            if (isCurrentValid(manager, music) && sounds.isActive(current)) {
                state.stopGraceTicks = 0;
                accessor.javaReforged$setNextSongDelay(Integer.MAX_VALUE);
                return true;
            }
            if (current instanceof MusicSoundInstance managed) managed.setFadeOut((float) SoundTransitionConfig.fadeOutTicks);
            else sounds.stop(current);
        }
        return startCommon(manager, state, music.sound(),
                current == null ? 0.0F : (float) SoundTransitionConfig.fadeInTicks);
    }

    public static boolean isPlaying(MusicManager manager, SoundEvent event) {
        MusicManagerAccessor accessor = accessor(manager);
        SoundInstance current = accessor.javaReforged$getCurrentMusic();
        return current != null && current.getIdentifier().equals(event.location())
                && accessor.javaReforged$getMinecraft().getSoundManager().isActive(current);
    }

    public static Component currentName(MusicManager manager) {
        SoundInstance current = accessor(manager).javaReforged$getCurrentMusic();
        return current == null ? Component.empty() : Component.literal(current.getIdentifier().toString());
    }

    public static void tick(MusicManager manager, MusicPlaybackState state) {
        MusicTransitionManager.tickMusicContinuityProtection();
        MusicManagerAccessor accessor = accessor(manager);
        Minecraft minecraft = accessor.javaReforged$getMinecraft();
        SoundInstance current = accessor.javaReforged$getCurrentMusic();
        Music music = net.neoforged.neoforge.client.ClientHooks.selectMusic(minecraft.getSituationalMusic(), current);
        SoundManager sounds = minecraft.getSoundManager();
        boolean protectLoad = state.loadingMode || MusicTransitionManager.shouldProtectWorldLoadAudio();
        boolean protectStop = protectLoad
                || MusicTransitionManager.shouldProtectForcedSoundStop()
                || MusicTransitionManager.shouldProtectRespawnMusicStop()
                || MusicTransitionManager.shouldProtectMusicContinuity();

        if (current != null && clearFinished(manager, state, sounds)) scheduleNext(manager, state, true);
        current = accessor.javaReforged$getCurrentMusic();
        if (current != null && !sounds.isActive(current)) {
            clearCurrent(manager, state);
            scheduleNext(manager, state, true);
        }
        if (music == null) {
            if (!state.resumeAfterWorldExit) {
                accessor.javaReforged$setNextSongDelay(0);
                state.configuredStartDelayArmed = false;
            }
            return;
        }
        current = accessor.javaReforged$getCurrentMusic();
        if (current != null) {
            if (!isCurrentValid(manager, music) && music.replaceCurrentMusic() && !protectStop && state.stopGraceTicks <= 0) {
                fadeForReplacement(manager, state, sounds);
            }
        }
        if (protectLoad) return;
        if (state.stopGraceTicks > 0) state.stopGraceTicks--;
        if (accessor.javaReforged$getCurrentMusic() != null) return;

        if (accessor.javaReforged$getNextSongDelay() == Integer.MAX_VALUE) {
            accessor.javaReforged$setNextSongDelay(0);
            state.configuredStartDelayArmed = false;
        }

        if (!state.initialPlayback) {
            if (state.resumeAfterWorldExit) {
                if (((SoundEngineBridge) sounds).javaReforged$hasFadingMusic()) return;
                if (!state.worldExitIntervalArmed) {
                    accessor.javaReforged$setNextSongDelay(deriveDelay(manager, false));
                    state.configuredStartDelayArmed = true;
                    state.worldExitIntervalArmed = true;
                }
            } else if (!state.configuredStartDelayArmed) {
                accessor.javaReforged$setNextSongDelay(deriveDelay(manager, false));
                state.configuredStartDelayArmed = true;
            }
        }
        if (accessor.javaReforged$getNextSongDelay() > 0) {
            accessor.javaReforged$setNextSongDelay(accessor.javaReforged$getNextSongDelay() - 1);
            return;
        }
        if (!startPlaying(manager, state, music)) return;
        state.resumeAfterWorldExit = false;
        state.worldExitIntervalArmed = false;
        accessor.javaReforged$setNextSongDelay(deriveDelay(manager, false));
        state.configuredStartDelayArmed = false;
    }

    public static boolean startPlaying(MusicManager manager, MusicPlaybackState state, Music music) {
        if (music == null) return false;
        float fade = state.fadeInNextTrack ? (float) SoundTransitionConfig.fadeInTicks : 0.0F;
        if (!startCommon(manager, state, music.sound(), fade)) return false;
        state.fadeInNextTrack = false;
        return true;
    }

    private static int deriveDelay(MusicManager manager, boolean ended) {
        Minecraft minecraft = accessor(manager).javaReforged$getMinecraft();
        boolean menu = globalLevel(minecraft) == null;
        int min = ended ? (menu ? SoundTransitionConfig.menuMinDelayTicks : SoundTransitionConfig.minDelayTicks)
                : (menu ? SoundTransitionConfig.menuMinStartTicks : SoundTransitionConfig.minStartTicks);
        int max = ended ? (menu ? SoundTransitionConfig.menuMaxDelayTicks : SoundTransitionConfig.maxDelayTicks)
                : (menu ? SoundTransitionConfig.menuMaxStartTicks : SoundTransitionConfig.maxStartTicks);
        return min + accessor(manager).javaReforged$getRandom().nextInt(Math.max(1, max - min + 1));
    }

    private static boolean isCurrentValid(MusicManager manager, Music music) {
        SoundInstance current = accessor(manager).javaReforged$getCurrentMusic();
        return current != null && music.sound().value().location().equals(current.getIdentifier());
    }

    private static void fadeForReplacement(MusicManager manager, MusicPlaybackState state, SoundManager sounds) {
        MusicManagerAccessor accessor = accessor(manager);
        SoundInstance current = accessor.javaReforged$getCurrentMusic();
        if (current instanceof MusicSoundInstance music) music.setFadeOut((float) SoundTransitionConfig.fadeOutTicks);
        else sounds.stop(current);
        accessor.javaReforged$setNextSongDelay(deriveDelay(manager, true));
        state.stopGraceTicks = Math.max(state.stopGraceTicks, SoundTransitionConfig.fadeOutTicks);
    }

    private static boolean startCommon(MusicManager manager, MusicPlaybackState state,
                                       Holder<SoundEvent> event, float fadeTicks) {
        MusicManagerAccessor accessor = accessor(manager);
        SoundManager sounds = accessor.javaReforged$getMinecraft().getSoundManager();
        if (accessor.javaReforged$getCurrentMusic() == null
                && ((SoundEngineBridge) sounds).javaReforged$hasManagedMusic()) {
            accessor.javaReforged$setNextSongDelay(RETRY_DELAY_TICKS);
            state.configuredStartDelayArmed = true;
            return false;
        }
        state.initialPlayback = false;
        state.resumeAfterWorldExit = false;
        state.worldExitIntervalArmed = false;
        SoundInstance music = new MusicSoundInstance(event.value(), fadeTicks);
        accessor.javaReforged$setCurrentMusic(music);
        accessor.javaReforged$setNextSongDelay(Integer.MAX_VALUE);
        state.configuredStartDelayArmed = false;
        SoundEngine.PlayResult result = sounds.play(music);
        state.stopGraceTicks = 0;
        if (result == SoundEngine.PlayResult.NOT_STARTED) {
            accessor.javaReforged$setCurrentMusic(null);
            accessor.javaReforged$setNextSongDelay(RETRY_DELAY_TICKS);
            state.configuredStartDelayArmed = true;
            return false;
        }
        return true;
    }

    private static void clearCurrent(MusicManager manager, MusicPlaybackState state) {
        accessor(manager).javaReforged$setCurrentMusic(null);
        state.stopGraceTicks = 0;
    }

    private static void releaseProtectedFadeDelay(MusicManager manager, MusicPlaybackState state) {
        MusicManagerAccessor accessor = accessor(manager);
        if (accessor.javaReforged$getNextSongDelay() != Integer.MAX_VALUE) return;
        accessor.javaReforged$setNextSongDelay(deriveDelay(manager, accessor.javaReforged$getCurrentMusic() != null));
        state.configuredStartDelayArmed = true;
    }

    private static boolean clearFinished(MusicManager manager, MusicPlaybackState state, SoundManager sounds) {
        SoundInstance current = accessor(manager).javaReforged$getCurrentMusic();
        if (!(current instanceof MusicSoundInstance music) || !music.isStopped()) return false;
        sounds.stop(current);
        clearCurrent(manager, state);
        return true;
    }

    private static void scheduleNext(MusicManager manager, MusicPlaybackState state, boolean ended) {
        MusicManagerAccessor accessor = accessor(manager);
        int delay = deriveDelay(manager, ended);
        accessor.javaReforged$setNextSongDelay(accessor.javaReforged$getNextSongDelay() == Integer.MAX_VALUE
                ? delay : Math.min(accessor.javaReforged$getNextSongDelay(), delay));
        state.configuredStartDelayArmed = true;
    }

    private static @Nullable ClientLevel globalLevel(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        return player != null && level != null && player.level() == level && !player.isRemoved() ? level : null;
    }

    private static MusicManagerAccessor accessor(MusicManager manager) {
        return (MusicManagerAccessor) manager;
    }
}
