package net.jr.mixin.runtime;

import javax.annotation.Nullable;
import net.jr.client.runtime.audio.SlotSoundRouter;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
public abstract class SoundManagerMixin {
    @Shadow
    @Final
    private SoundEngine soundEngine;

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void javaReforged$routeSlotSound(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        SlotSoundRouter.PlayRoute route = SlotSoundRouter.play(this.soundEngine, sound);
        if (!route.handled()) {
            return;
        }
        cir.cancel();
        if (route.result() != null) {
            cir.setReturnValue(route.result());
        }
    }

    @Inject(method = "playDelayed", at = @At("HEAD"), cancellable = true)
    private void javaReforged$routeDelayedSlotSound(SoundInstance sound, int delay, CallbackInfo ci) {
        if (SlotSoundRouter.playDelayed(this.soundEngine, sound, delay)) {
            ci.cancel();
        }
    }

    @Inject(method = "queueTickingSound", at = @At("HEAD"), cancellable = true)
    private void javaReforged$routeTickingSlotSound(TickableSoundInstance tickableSound, CallbackInfo ci) {
        if (SlotSoundRouter.queueTicking(this.soundEngine, tickableSound)) {
            ci.cancel();
        }
    }

    @Inject(method = "stop(Lnet/minecraft/client/resources/sounds/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
    private void javaReforged$stopSlotExactSound(SoundInstance sound, CallbackInfo ci) {
        SlotSoundRouter.stopExact((SoundManager)(Object)this, sound);
        ci.cancel();
    }

    @Inject(method = "stop(Lnet/minecraft/resources/Identifier;Lnet/minecraft/sounds/SoundSource;)V", at = @At("HEAD"), cancellable = true)
    private void javaReforged$stopSlotMatchedSounds(
        @Nullable Identifier soundName,
        @Nullable SoundSource source,
        CallbackInfo ci
    ) {
        SlotSoundRouter.stopMatching((SoundManager)(Object)this, soundName, source);
        ci.cancel();
    }
}
