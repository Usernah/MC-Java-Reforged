package net.jr.mixin;

import net.jr.client.sound.bridge.SoundEngineBridge;
import net.jr.client.sound.runtime.MusicSoundHandler;
import net.jr.client.sound.runtime.PausedMusicState;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin implements SoundEngineBridge {
    @Unique private final PausedMusicState javaReforged$pausedMusicState = new PausedMusicState();

    @Inject(method = {"stopAll()V", "reload()V"}, at = @At("HEAD"), cancellable = true)
    private void javaReforged$protectWorldTransitionAudio(CallbackInfo ci) {
        if (MusicSoundHandler.protectTransitionStop()) {
            MusicSoundHandler.stopSounds(this.javaReforged$engine());
            ci.cancel();
        }
    }

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void javaReforged$wrapMusic(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        SoundEngine.PlayResult result = MusicSoundHandler.interceptPlay(this.javaReforged$engine(), sound);
        if (result != null) cir.setReturnValue(result);
    }

    @Inject(method = "stop(Lnet/minecraft/client/resources/sounds/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
    private void javaReforged$fadeManagedStop(SoundInstance sound, CallbackInfo ci) {
        if (MusicSoundHandler.fadeManagedStop(this.javaReforged$engine(), sound)) ci.cancel();
    }

    @Inject(method = "tick(Z)V", at = @At("TAIL"))
    private void javaReforged$tickPausedMusic(boolean paused, CallbackInfo ci) {
        MusicSoundHandler.tickPaused(this.javaReforged$engine(), this.javaReforged$pausedMusicState, paused);
    }

    @Inject(method = "pauseAllExcept([Lnet/minecraft/sounds/SoundSource;)V", at = @At("HEAD"), cancellable = true)
    private void javaReforged$preservePausedMusic(SoundSource[] ignored, CallbackInfo ci) {
        if (MusicSoundHandler.pauseExcept(this.javaReforged$engine(), this.javaReforged$pausedMusicState)) ci.cancel();
    }

    @Inject(method = {"resume()V", "stopAll()V"}, at = @At("TAIL"))
    private void javaReforged$clearPausedMusic(CallbackInfo ci) { this.javaReforged$pausedMusicState.clear(); }

    @Override public void javaReforged$stopMusic() { MusicSoundHandler.stopMusic(this.javaReforged$engine()); }
    @Override public void javaReforged$fadeSounds(float ticks) { MusicSoundHandler.fadeSounds(this.javaReforged$engine(), ticks); }
    @Override public void javaReforged$stopSounds() { MusicSoundHandler.stopSounds(this.javaReforged$engine()); }
    @Override public void javaReforged$clearQueued() { MusicSoundHandler.clearQueued(this.javaReforged$engine()); }
    @Override public boolean javaReforged$hasFadingMusic() { return MusicSoundHandler.hasFadingMusic(this.javaReforged$engine()); }
    @Override public boolean javaReforged$hasManagedMusic() { return MusicSoundHandler.hasManagedMusic(this.javaReforged$engine()); }

    @Unique private SoundEngine javaReforged$engine() { return (SoundEngine) (Object) this; }
}
