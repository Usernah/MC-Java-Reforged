package net.jr.mixin.runtime;

import net.jr.client.runtime.render.state.ParticleStateRouter;
import net.jr.client.runtime.context.SlotScope;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Inject(method = "setLevel", at = @At("HEAD"), cancellable = true)
    private void javaReforged$setGlobalParticleLevel(ClientLevel level, CallbackInfo callback) {
        if (SlotScope.idOrNull() == null) {
            ParticleStateRouter.setGlobalLevel((ParticleEngine)(Object)this, level);
            callback.cancel();
        }
    }

    @Inject(method = "clearParticles", at = @At("HEAD"), cancellable = true)
    private void javaReforged$clearGlobalParticleStates(CallbackInfo callback) {
        if (SlotScope.idOrNull() == null) {
            ParticleStateRouter.clearAll((ParticleEngine)(Object)this);
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
    private void javaReforged$installParticleSlotLevel(CallbackInfo ci) {
        ParticleStateRouter.installActiveLevel((ParticleEngine)(Object)this);
    }

    @Inject(
            method = "createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD")
    )
    private void javaReforged$installParticleSlotLevelForCreate(CallbackInfoReturnable<Particle> cir) {
        ParticleStateRouter.installActiveLevel((ParticleEngine)(Object)this);
    }

}
