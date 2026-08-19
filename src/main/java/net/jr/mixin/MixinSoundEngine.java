package net.jr.mixin;

import com.mojang.blaze3d.audio.Channel;
import net.jr.client.sound.bridge.SoundEngineBridge;
import net.jr.client.sound.config.SoundTransitionConfig;
import net.jr.client.sound.music.MusicSoundInstance;
import net.jr.client.sound.music.MusicTransitionManager;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(SoundEngine.class)
public abstract class MixinSoundEngine implements SoundEngineBridge {
    @Shadow @Final private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;
    @Shadow @Final private Map<SoundInstance, Integer> queuedSounds;
    @Shadow @Final private List<TickableSoundInstance> queuedTickableSounds;
    @Shadow private boolean loaded;

    @Unique private final Set<TickableSoundInstance> tickingWhilePaused = new LinkedHashSet<>();

    @Shadow
    public abstract void stop(SoundInstance sound);

    @Shadow
    private float calculateVolume(SoundInstance sound) {
        throw new AssertionError();
    }

    @Shadow
    private float calculatePitch(SoundInstance sound) {
        throw new AssertionError();
    }

    @Shadow
    public abstract SoundEngine.PlayResult play(SoundInstance sound);

    @Inject(method = "stopAll()V", at = @At("HEAD"), cancellable = true)
    private void moods$protectMusicOnLoad(CallbackInfo ci) {
        if (MusicTransitionManager.shouldProtectWorldLoadAudio()) {
            ci.cancel();
        }
    }

    @Inject(method = "reload()V", at = @At("HEAD"), cancellable = true)
    private void moods$protectMusicOnReload(CallbackInfo ci) {
        if (MusicTransitionManager.shouldProtectWorldLoadAudio()) {
            ci.cancel();
        }
    }

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void moods$onPlayInterceptor(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (sound == null || sound instanceof MusicSoundInstance || !this.loaded) {
            return;
        }

        if (sound.getSource() == SoundSource.MUSIC) {
            MusicSoundInstance custom = new MusicSoundInstance(sound, (float) SoundTransitionConfig.fadeInTicks);
            cir.setReturnValue(this.play(custom));
        }
    }

    @Inject(method = "stop(Lnet/minecraft/client/resources/sounds/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
    private void moods$fadeManagedSoundStops(SoundInstance sound, CallbackInfo ci) {
        if (sound == null || !this.loaded || !(sound instanceof MusicSoundInstance musicSound)) {
            return;
        }

        if (SoundTransitionConfig.fadeOutTicks <= 0 || musicSound.isStopped() || musicSound.getDirectVolume() <= 0.0F) {
            return;
        }

        musicSound.setFadeOut((float) SoundTransitionConfig.fadeOutTicks);
        ci.cancel();
    }

    @Inject(method = "tick(Z)V", at = @At("TAIL"))
    private void moods$tickHook(boolean paused, CallbackInfo ci) {
        if (!paused || SoundTransitionConfig.allowPausingMusic) {
            return;
        }

        Iterator<TickableSoundInstance> iterator = this.tickingWhilePaused.iterator();
        while (iterator.hasNext()) {
            TickableSoundInstance sound = iterator.next();
            if (!sound.canPlaySound()) {
                this.stop(sound);
                iterator.remove();
            } else {
                sound.tick();
                if (sound.isStopped()) {
                    this.stop(sound);
                    iterator.remove();
                } else {
                    ChannelAccess.ChannelHandle handle = this.instanceToChannel.get(sound);
                    if (handle != null) {
                        this.moods$tickSound(sound, handle);
                    } else {
                        iterator.remove();
                    }
                }
            }
        }
    }

    @Inject(method = "pauseAllExcept([Lnet/minecraft/sounds/SoundSource;)V", at = @At("HEAD"), cancellable = true)
    private void moods$collectPausedProtectedSounds(SoundSource[] ignoredSources, CallbackInfo ci) {
        if (SoundTransitionConfig.allowPausingMusic) {
            return;
        }

        ci.cancel();
        this.tickingWhilePaused.clear();
        for (Map.Entry<SoundInstance, ChannelAccess.ChannelHandle> entry : new ArrayList<>(this.instanceToChannel.entrySet())) {
            if (entry.getKey() instanceof MusicSoundInstance musicSound) {
                this.tickingWhilePaused.add(musicSound);
            } else if (entry.getValue() != null) {
                entry.getValue().execute(Channel::pause);
            }
        }
    }

    @Inject(method = "resume()V", at = @At("HEAD"))
    private void moods$onResume(CallbackInfo ci) {
        this.tickingWhilePaused.clear();
    }

    @Inject(method = "stopAll()V", at = @At("TAIL"))
    private void moods$clearPausedSoundsAfterStopAll(CallbackInfo ci) {
        this.tickingWhilePaused.clear();
    }

    @Unique
    private void moods$tickSound(SoundInstance sound, ChannelAccess.ChannelHandle handle) {
        if (sound instanceof MusicSoundInstance music) {
            float volume = this.calculateVolume(music);
            float pitch = this.calculatePitch(music);
            handle.execute(channel -> {
                channel.setVolume(volume);
                channel.setPitch(pitch);
            });
        }
    }

    @Override
    public void moods$stopMusic() {
        if (!this.loaded) {
            return;
        }

        for (Map.Entry<SoundInstance, ChannelAccess.ChannelHandle> entry : new ArrayList<>(this.instanceToChannel.entrySet())) {
            if (entry.getKey() instanceof MusicSoundInstance && entry.getValue() != null) {
                entry.getValue().execute(Channel::stop);
            }
        }
    }

    @Override
    public void moods$fadeSounds(float ticks) {
        if (!this.loaded) {
            return;
        }

        for (SoundInstance sound : new ArrayList<>(this.instanceToChannel.keySet())) {
            if (sound instanceof MusicSoundInstance musicSound) {
                musicSound.setFadeOut(ticks);
            }
        }
    }

    @Override
    public void moods$stopSounds() {
    }

    @Override
    public void moods$clearQueued() {
        this.queuedSounds.clear();
        this.queuedTickableSounds.clear();
    }

    @Override
    public boolean moods$hasFadingMusic() {
        if (!this.loaded) {
            return false;
        }

        for (SoundInstance sound : this.instanceToChannel.keySet()) {
            if (sound instanceof MusicSoundInstance musicSound && musicSound.isFadingOut()) {
                return true;
            }
        }

        return false;
    }
}
