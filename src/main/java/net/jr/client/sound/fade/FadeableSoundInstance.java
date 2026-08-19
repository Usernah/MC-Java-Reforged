package net.jr.client.sound.fade;

import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public class FadeableSoundInstance extends AbstractTickableSoundInstance implements Fadeable {
    private static final float JUMP_LIMIT = 0.1F;

    protected final DeltaTracker.Timer timer = new DeltaTracker.Timer(
        20.0F,
        System.currentTimeMillis(),
        FloatUnaryOperator.identity()
    );
    protected float maxVolume = 1.0F;
    protected float fadeOut;
    protected float fadeIn;

    protected final void advanceTimer() {
        this.timer.advanceRealTime(System.currentTimeMillis());
    }

    protected final float getTickDelta() {
        return this.timer.getRealtimeDeltaTicks();
    }

    protected FadeableSoundInstance(SoundEvent soundEvent, SoundSource soundSource, RandomSource randomSource) {
        super(soundEvent, soundSource, randomSource);
    }

    @Override
    public void tick() {
        this.advanceTimer();

        if (this.fadeOut > 0.0F && this.volume > 0.0F) {
            if (this.volume > 1.0F) {
                this.volume /= 2.0F;
            }

            float newVolume = Math.max(this.volume - Math.min(this.getTickDelta() / this.fadeOut, JUMP_LIMIT), 0.0F);
            if (!Float.isNaN(newVolume)) {
                this.volume = newVolume;
            }
        }

        if (this.fadeIn > 0.0F && this.volume < this.maxVolume) {
            float newVolume = Math.min(this.volume + Math.min(this.getTickDelta() / this.fadeIn, JUMP_LIMIT), this.maxVolume);
            if (!Float.isNaN(newVolume)) {
                this.volume = newVolume;
            }
        }
    }

    @Override
    public boolean canStartSilent() {
        return this.fadeOut <= 0.0F && this.fadeIn > 0.0F;
    }

    @Override
    public boolean isStopped() {
        return super.isStopped() || this.fadeOut > 0.0F && this.volume <= 0.0F;
    }

    @Override
    public void setFadeOut(float fadeOut) {
        this.fadeOut = fadeOut;
        this.fadeIn = 0.0F;
    }

    @Override
    public void setFadeIn(float fadeIn) {
        this.fadeIn = fadeIn;
        this.fadeOut = 0.0F;
    }
}
