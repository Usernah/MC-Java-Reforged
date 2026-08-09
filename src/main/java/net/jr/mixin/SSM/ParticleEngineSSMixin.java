package net.jr.mixin.SSM;

import net.jr.ClientRuntime.runtime.ParticleEngineFields;
import net.jr.ClientRuntime.runtime.Client;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineSSMixin {
    @Inject(method = "setLevel", at = @At("HEAD"), cancellable = true)
    private void splitTest$setGlobalParticleLevel(ClientLevel level, CallbackInfo callback) {
        if (Client.currentOrNull() == null) {
            ParticleEngineFields.setGlobalLevel((ParticleEngine)(Object)this, level);
            callback.cancel();
        }
    }

    @Inject(method = "clearParticles", at = @At("HEAD"), cancellable = true)
    private void splitTest$clearGlobalParticleStates(CallbackInfo callback) {
        if (Client.currentOrNull() == null) {
            ParticleEngineFields.clearAll((ParticleEngine)(Object)this);
            callback.cancel();
        }
    }

    @Inject(
            method = {
                    "createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;)V",
                    "createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;I)V",
                    "tick()V",
                    "extract(Lnet/minecraft/client/renderer/state/level/ParticlesRenderState;Lnet/minecraft/client/renderer/culling/Frustum;Lnet/minecraft/client/Camera;F)V"
            },
            at = @At("HEAD")
    )
    private void splitTest$installParticleSlotLevel(CallbackInfo ci) {
        ParticleEngineFields.installActiveLevel((ParticleEngine)(Object)this);
    }

    @Inject(
            method = "createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD")
    )
    private void splitTest$installParticleSlotLevelForCreate(CallbackInfoReturnable<Particle> cir) {
        ParticleEngineFields.installActiveLevel((ParticleEngine)(Object)this);
    }

}
