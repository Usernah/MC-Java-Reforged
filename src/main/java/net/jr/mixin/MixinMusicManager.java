package net.jr.mixin;

import net.jr.client.sound.bridge.MusicManagerBridge;
import net.jr.client.sound.bridge.SoundEngineBridge;
import net.jr.client.sound.config.SoundTransitionConfig;
import net.jr.client.sound.music.MusicSoundInstance;
import net.jr.client.sound.music.MusicTransitionState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MusicManager.class)
public abstract class MixinMusicManager implements MusicManagerBridge {
    @Shadow private @Nullable SoundInstance currentMusic;
    @Shadow private int nextSongDelay;
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private RandomSource random;

    @Unique private int stopGraceTicks = 0;
    @Unique private boolean loadingMode = false;
    @Unique private boolean fadeInNextTrack = false;
    @Unique private boolean configuredStartDelayArmed = false;
    @Unique private boolean resumeMusicAfterWorldExit = false;
    @Unique private boolean initialMusicPlayback = true;
    @Unique private boolean worldExitIntervalArmed = false;

    @Override
    public void moods$setStopGraceTicks(int ticks) {
        this.stopGraceTicks = Math.max(0, ticks);
    }

    @Override
    public void moods$setLoadingMode(boolean loading) {
        if (loading) {
            this.resumeMusicAfterWorldExit = false;
            this.worldExitIntervalArmed = false;
        }

        if (this.loadingMode && !loading) {
            this.fadeInNextTrack = true;
            this.javareforged$releaseProtectedFadeDelay();
        }

        this.loadingMode = loading;
    }

    @Override
    public void moods$resetAfterWorldExit() {
        this.loadingMode = false;
        this.stopGraceTicks = 0;
        this.fadeInNextTrack = true;
        this.configuredStartDelayArmed = false;
        this.resumeMusicAfterWorldExit = true;
        this.initialMusicPlayback = false;
        this.worldExitIntervalArmed = false;

        if (this.currentMusic != null) {
            SoundManager soundManager = this.minecraft.getSoundManager();
            if (this.currentMusic instanceof MusicSoundInstance musicSound
                && !musicSound.isStopped()
                && musicSound.getDirectVolume() > 0.0F
                && SoundTransitionConfig.fadeOutTicks > 0) {
                musicSound.setFadeOut((float) SoundTransitionConfig.fadeOutTicks);
            } else {
                soundManager.stop(this.currentMusic);
            }
        }

        this.currentMusic = null;
        this.nextSongDelay = 0;
    }

    @Override
    public void moods$fadeOutCurrentMusic() {
        if (this.currentMusic == null) {
            return;
        }

        SoundManager soundManager = this.minecraft.getSoundManager();
        if (this.currentMusic instanceof MusicSoundInstance musicSound) {
            musicSound.setFadeOut((float) SoundTransitionConfig.fadeOutTicks);
        } else {
            soundManager.stop(this.currentMusic);
        }

        this.stopGraceTicks = Math.max(this.stopGraceTicks, SoundTransitionConfig.fadeOutTicks);
        this.nextSongDelay = Integer.MAX_VALUE;
    }

    @Override
    public boolean moods$intrudeMusic(@NotNull Music music) {
        SoundManager soundManager = this.minecraft.getSoundManager();
        if (this.currentMusic != null) {
            if (this.javareforged$isCurrentTrackStillValid(music) && soundManager.isActive(this.currentMusic)) {
                this.stopGraceTicks = 0;
                this.nextSongDelay = Integer.MAX_VALUE;
                return true;
            }

            if (this.currentMusic instanceof MusicSoundInstance musicSound) {
                musicSound.setFadeOut((float) SoundTransitionConfig.fadeOutTicks);
            } else {
                soundManager.stop(this.currentMusic);
            }
        }

        this.javareforged$startPlayingCommon(
            music.sound(),
            this.currentMusic == null ? 0.0F : (float) SoundTransitionConfig.fadeInTicks
        );
        return true;
    }

    @Override
    public boolean moods$isCurrentlyPlaying(SoundEvent soundEvent) {
        return this.currentMusic != null
            && this.currentMusic.getIdentifier().equals(soundEvent.location())
            && this.minecraft.getSoundManager().isActive(this.currentMusic);
    }

    @Override
    public Component moods$getCurrentMusicName() {
        return this.currentMusic == null
            ? Component.empty()
            : Component.literal(this.currentMusic.getIdentifier().toString());
    }

    /**
     * @author Java Reforged
     * @reason Preserve the original fade transition scheduler while adapting it to Minecraft 26.2 music signatures.
     */
    @Overwrite
    public void tick() {
        MusicTransitionState.tickMusicContinuityProtection();

        Music musicInfo = net.neoforged.neoforge.client.ClientHooks.selectMusic(
            this.minecraft.getSituationalMusic(),
            this.currentMusic
        );
        SoundManager soundManager = this.minecraft.getSoundManager();
        boolean protectDuringLoad = this.loadingMode
            || MusicTransitionState.shouldProtectWorldLoadAudio()
            || MusicTransitionState.shouldProtectForcedSoundStop()
            || MusicTransitionState.shouldProtectRespawnMusicStop();
        boolean protectPassiveStop = protectDuringLoad || MusicTransitionState.shouldProtectMusicContinuity();

        if (musicInfo == null) {
            if (!this.resumeMusicAfterWorldExit) {
                this.nextSongDelay = 0;
                this.configuredStartDelayArmed = false;
            }
            return;
        }

        if (this.currentMusic != null
            && !protectPassiveStop
            && this.javareforged$clearFinishedManagedMusic(soundManager)) {
            this.javareforged$scheduleNextSongAfterCurrentCleared(true);
        }

        if (this.currentMusic != null) {
            if (!this.javareforged$isCurrentTrackStillValid(musicInfo)
                && musicInfo.replaceCurrentMusic()
                && !protectPassiveStop
                && this.stopGraceTicks <= 0) {
                this.javareforged$fadeOutCurrentMusic(soundManager, musicInfo);
            }

            if (!protectPassiveStop && !soundManager.isActive(this.currentMusic)) {
                this.javareforged$clearCurrent();
                this.javareforged$scheduleNextSongAfterCurrentCleared(true);
            }
        }

        if (protectDuringLoad) {
            return;
        }

        if (this.stopGraceTicks > 0) {
            this.stopGraceTicks--;
        }

        if (this.currentMusic == null) {
            if (this.initialMusicPlayback) {
                this.nextSongDelay = 0;
                this.configuredStartDelayArmed = true;
            } else if (this.resumeMusicAfterWorldExit) {
                SoundEngineBridge soundEngine = (SoundEngineBridge) soundManager;
                if (soundEngine.moods$hasFadingMusic()) {
                    return;
                }

                if (!this.worldExitIntervalArmed) {
                    this.nextSongDelay = this.javareforged$deriveNextSongDelay(false);
                    this.configuredStartDelayArmed = true;
                    this.worldExitIntervalArmed = true;
                }
            } else if (!this.configuredStartDelayArmed) {
                this.nextSongDelay = this.javareforged$deriveNextSongDelay(false);
                this.configuredStartDelayArmed = true;
            }

            if (this.nextSongDelay > 0) {
                this.nextSongDelay--;
                return;
            }

            this.startPlaying(musicInfo);
            this.resumeMusicAfterWorldExit = false;
            this.worldExitIntervalArmed = false;
            this.nextSongDelay = this.javareforged$deriveNextSongDelay(false);
            this.configuredStartDelayArmed = false;
        }
    }

    @Unique
    private int javareforged$deriveNextSongDelay(boolean afterTrackEnded) {
        int minTicks = afterTrackEnded
            ? this.javareforged$getConfiguredMinDelayTicks()
            : this.javareforged$getConfiguredMinStartTicks();
        int maxTicks = afterTrackEnded
            ? this.javareforged$getConfiguredMaxDelayTicks()
            : this.javareforged$getConfiguredMaxStartTicks();

        return minTicks + this.random.nextInt(Math.max(1, maxTicks - minTicks + 1));
    }

    @Unique
    private int javareforged$getConfiguredMinStartTicks() {
        return this.javareforged$globalMusicLevel() == null
            ? SoundTransitionConfig.menuMinStartTicks
            : SoundTransitionConfig.minStartTicks;
    }

    @Unique
    private int javareforged$getConfiguredMaxStartTicks() {
        return this.javareforged$globalMusicLevel() == null
            ? SoundTransitionConfig.menuMaxStartTicks
            : SoundTransitionConfig.maxStartTicks;
    }

    @Unique
    private int javareforged$getConfiguredMinDelayTicks() {
        return this.javareforged$globalMusicLevel() == null
            ? SoundTransitionConfig.menuMinDelayTicks
            : SoundTransitionConfig.minDelayTicks;
    }

    @Unique
    private int javareforged$getConfiguredMaxDelayTicks() {
        return this.javareforged$globalMusicLevel() == null
            ? SoundTransitionConfig.menuMaxDelayTicks
            : SoundTransitionConfig.maxDelayTicks;
    }

    @Unique
    private int javareforged$getConfiguredDelayAfterReplacement(Music nextMusic) {
        return this.javareforged$deriveNextSongDelay(true);
    }

    @Unique
    private boolean javareforged$isCurrentTrackStillValid(Music music) {
        return this.currentMusic != null
            && music.sound().value().location().equals(this.currentMusic.getIdentifier());
    }

    @Unique
    private void javareforged$fadeOutCurrentMusic(SoundManager soundManager, Music nextMusic) {
        if (this.currentMusic instanceof MusicSoundInstance musicSound) {
            musicSound.setFadeOut((float) SoundTransitionConfig.fadeOutTicks);
        } else {
            soundManager.stop(this.currentMusic);
        }

        this.nextSongDelay = this.javareforged$getConfiguredDelayAfterReplacement(nextMusic);
        this.stopGraceTicks = Math.max(this.stopGraceTicks, SoundTransitionConfig.fadeOutTicks);
    }

    /**
     * @author Java Reforged
     * @reason Construct the managed music instance used by the original fade system.
     */
    @Overwrite
    public void startPlaying(Music music) {
        if (music != null) {
            float fadeInTicks = this.fadeInNextTrack ? (float) SoundTransitionConfig.fadeInTicks : 0.0F;
            this.fadeInNextTrack = false;
            this.javareforged$startPlayingCommon(music.sound(), fadeInTicks);
        }
    }

    @Unique
    private void javareforged$startPlayingCommon(Holder<SoundEvent> soundEvent, float fadeInTicks) {
        this.initialMusicPlayback = false;
        this.resumeMusicAfterWorldExit = false;
        this.worldExitIntervalArmed = false;
        this.currentMusic = new MusicSoundInstance(soundEvent.value(), fadeInTicks);
        this.nextSongDelay = Integer.MAX_VALUE;
        this.configuredStartDelayArmed = false;
        this.minecraft.getSoundManager().play(this.currentMusic);
        this.stopGraceTicks = 0;
    }

    @Unique
    private void javareforged$clearCurrent() {
        this.currentMusic = null;
        this.stopGraceTicks = 0;
    }

    @Unique
    private void javareforged$releaseProtectedFadeDelay() {
        if (this.nextSongDelay != Integer.MAX_VALUE) {
            return;
        }

        this.nextSongDelay = this.currentMusic == null
            ? this.javareforged$deriveNextSongDelay(false)
            : this.javareforged$deriveNextSongDelay(true);
        this.configuredStartDelayArmed = true;
    }

    @Unique
    private boolean javareforged$clearFinishedManagedMusic(SoundManager soundManager) {
        if (!(this.currentMusic instanceof MusicSoundInstance musicSound) || !musicSound.isStopped()) {
            return false;
        }

        soundManager.stop(this.currentMusic);
        this.javareforged$clearCurrent();
        return true;
    }

    @Unique
    private void javareforged$scheduleNextSongAfterCurrentCleared(boolean afterTrackEnded) {
        int nextDelay = this.javareforged$deriveNextSongDelay(afterTrackEnded);
        this.nextSongDelay = this.nextSongDelay == Integer.MAX_VALUE
            ? nextDelay
            : Math.min(this.nextSongDelay, nextDelay);
        this.configuredStartDelayArmed = true;
    }

    @Unique
    private @Nullable ClientLevel javareforged$globalMusicLevel() {
        LocalPlayer player = this.minecraft.player;
        ClientLevel level = this.minecraft.level;
        return player != null && level != null && player.level() == level && !player.isRemoved()
            ? level
            : null;
    }
}
