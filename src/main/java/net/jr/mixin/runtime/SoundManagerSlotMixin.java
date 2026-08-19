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
public abstract class SoundManagerSlotMixin {
    @Shadow
    @Final
    private SoundEngine soundEngine;

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void splitTest$routeSlotSound(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        SoundInstance routed = SlotSoundRouter.route(sound);
        if (routed == sound) {
            return;
        }
        cir.cancel();
        if (routed != null) {
            cir.setReturnValue(this.soundEngine.play(routed));
        }
    }

    @Inject(method = "playDelayed", at = @At("HEAD"), cancellable = true)
    private void splitTest$routeDelayedSlotSound(SoundInstance sound, int delay, CallbackInfo ci) {
        SoundInstance routed = SlotSoundRouter.route(sound);
        if (routed == sound) {
            return;
        }
        ci.cancel();
        if (routed != null) {
            this.soundEngine.playDelayed(routed, delay);
        }
    }

    @Inject(method = "queueTickingSound", at = @At("HEAD"), cancellable = true)
    private void splitTest$routeTickingSlotSound(TickableSoundInstance tickableSound, CallbackInfo ci) {
        SoundInstance routed = SlotSoundRouter.route(tickableSound);
        if (routed == tickableSound) {
            return;
        }
        ci.cancel();
        if (routed instanceof TickableSoundInstance routedTickableSound) {
            this.soundEngine.queueTickingSound(routedTickableSound);
        } else if (routed != null) {
            throw new IllegalStateException("Routed ticking sound lost tickable type");
        }
    }

    @Inject(method = "stop(Lnet/minecraft/client/resources/sounds/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
    private void splitTest$stopSlotExactSound(SoundInstance sound, CallbackInfo ci) {
        SlotSoundRouter.stopExact((SoundManager)(Object)this, sound);
        ci.cancel();
    }

    @Inject(method = "stop(Lnet/minecraft/resources/Identifier;Lnet/minecraft/sounds/SoundSource;)V", at = @At("HEAD"), cancellable = true)
    private void splitTest$stopSlotMatchedSounds(
        @Nullable Identifier soundName,
        @Nullable SoundSource source,
        CallbackInfo ci
    ) {
        SlotSoundRouter.stopMatching((SoundManager)(Object)this, soundName, source);
        ci.cancel();
    }
}
