package net.alnv.javareforged.mixin.SSM;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Map;
import java.util.Queue;
import net.alnv.javareforged.ClientRuntime.runtime.ParticleEngineFields;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.core.particles.ParticleGroup;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineSSMixin {
    @Inject(
            method = {
                    "createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;)V",
                    "createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;I)V",
                    "destroy(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
                    "crack(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)V",
                    "addBlockHitEffects(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;)V",
                    "tick()V",
                    "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V",
                    "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V"
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

    @Redirect(
            method = {
                    "tick()V",
                    "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V",
                    "countParticles()Ljava/lang/String;",
                    "iterateParticles(Ljava/util/function/Consumer;)V",
                    "clearParticles()V"
            },
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/particle/ParticleEngine;particles:Ljava/util/Map;", opcode = Opcodes.GETFIELD)
    )
    private Map<ParticleRenderType, Queue<Particle>> splitTest$particles(ParticleEngine engine) {
        return ParticleEngineFields.particles();
    }

    @Redirect(
            method = {
                    "createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;)V",
                    "createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;I)V",
                    "tick()V",
                    "setLevel(Lnet/minecraft/client/multiplayer/ClientLevel;)V",
                    "clearParticles()V"
            },
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/particle/ParticleEngine;trackingEmitters:Ljava/util/Queue;", opcode = Opcodes.GETFIELD)
    )
    private Queue<TrackingEmitter> splitTest$trackingEmitters(ParticleEngine engine) {
        return ParticleEngineFields.trackingEmitters();
    }

    @Redirect(
            method = {
                    "add(Lnet/minecraft/client/particle/Particle;)V",
                    "tick()V",
                    "clearParticles()V"
            },
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/particle/ParticleEngine;particlesToAdd:Ljava/util/Queue;", opcode = Opcodes.GETFIELD)
    )
    private Queue<Particle> splitTest$particlesToAdd(ParticleEngine engine) {
        return ParticleEngineFields.particlesToAdd();
    }

    @Redirect(
            method = {
                    "updateCount(Lnet/minecraft/core/particles/ParticleGroup;I)V",
                    "hasSpaceInParticleLimit(Lnet/minecraft/core/particles/ParticleGroup;)Z",
                    "clearParticles()V"
            },
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/particle/ParticleEngine;trackedParticleCounts:Lit/unimi/dsi/fastutil/objects/Object2IntOpenHashMap;", opcode = Opcodes.GETFIELD)
    )
    private Object2IntOpenHashMap<ParticleGroup> splitTest$trackedParticleCounts(ParticleEngine engine) {
        return ParticleEngineFields.trackedParticleCounts();
    }
}