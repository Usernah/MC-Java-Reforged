package net.jr.client.sound.music;

import net.jr.api.client.resource.Asset;
import net.jr.client.sound.fade.FadeableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class MusicSoundInstance extends FadeableSoundInstance {
    private @Nullable SoundInstance delegate;
    private boolean delegateInitialized;

    public MusicSoundInstance(SoundEvent soundEvent) {
        super(soundEvent, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
        this.delegate = null;
        this.delegateInitialized = true;
        this.relative = true;
    }

    public MusicSoundInstance(SoundEvent soundEvent, float fadeIn) {
        this(soundEvent);
        if (fadeIn > 0.0F) {
            this.setFadeIn(fadeIn);
            this.volume = 0.0F;
        }
    }

    public MusicSoundInstance(Asset asset, SoundSource source, float fadeIn) {
        super(SoundEvent.createVariableRangeEvent(asset.res()), source, SoundInstance.createUnseededRandom());
        this.delegate = null;
        this.delegateInitialized = true;
        this.relative = source == SoundSource.MUSIC;

        if (fadeIn > 0.0F) {
            this.setFadeIn(fadeIn);
            this.volume = 0.0F;
        }
    }

    public MusicSoundInstance(SoundInstance original, float fadeIn) {
        this(new Asset(original.getIdentifier()), original.getSource(), fadeIn);
        this.delegate = original;
        this.delegateInitialized = false;
        this.x = original.getX();
        this.y = original.getY();
        this.z = original.getZ();
        this.looping = original.isLooping();
        this.delay = original.getDelay();
        this.attenuation = original.getAttenuation();
        this.relative = original.isRelative();
    }

    public float getDirectVolume() {
        return this.volume;
    }

    public boolean isFadingOut() {
        return this.fadeOut > 0.0F && !this.isStopped();
    }

    @Override
    public @Nullable WeighedSoundEvents resolve(SoundManager handler) {
        if (this.delegate != null) {
            WeighedSoundEvents resolved = this.delegate.resolve(handler);
            this.sound = this.delegate.getSound();
            if (!this.delegateInitialized) {
                this.maxVolume = this.delegate.getVolume();
                this.volume = this.fadeIn > 0.0F ? 0.0F : this.maxVolume;
                this.pitch = this.delegate.getPitch();
                this.delegateInitialized = true;
            }
            return resolved;
        }

        return super.resolve(handler);
    }

    @Override
    public float getVolume() {
        return this.volume;
    }

    @Override
    public float getPitch() {
        return this.pitch;
    }

    @Override
    public boolean canStartSilent() {
        return this.delegate != null ? this.delegate.canStartSilent() || super.canStartSilent() : super.canStartSilent();
    }

    @Override
    public boolean canPlaySound() {
        return this.delegate == null || this.delegate.canPlaySound();
    }

    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary soundBuffers, Sound sound, boolean looping) {
        return this.delegate != null
            ? this.delegate.getStream(soundBuffers, sound, looping)
            : super.getStream(soundBuffers, sound, looping);
    }

    @Override
    public String toString() {
        return "MusicSoundInstance{fadeOut=" + this.fadeOut
            + ", fadeIn=" + this.fadeIn
            + ", source=" + this.source
            + ", location=" + this.identifier
            + ", volume=" + this.volume + "}";
    }
}
