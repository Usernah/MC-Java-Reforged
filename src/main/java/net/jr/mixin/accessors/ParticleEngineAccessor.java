package net.jr.mixin.accessors;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Map;
import java.util.Queue;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.core.particles.ParticleLimit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ParticleEngine.class)
public interface ParticleEngineAccessor {
    @Accessor("level")
    ClientLevel javaReforged$getLevel();

    @Accessor("level")
    void javaReforged$setLevel(ClientLevel level);

    @Accessor("particles")
    Map<ParticleRenderType, ParticleGroup<?>> javaReforged$getParticles();

    @Accessor("trackingEmitters")
    Queue<TrackingEmitter> javaReforged$getTrackingEmitters();

    @Accessor("particlesToAdd")
    Queue<Particle> javaReforged$getParticlesToAdd();

    @Accessor("trackedParticleCounts")
    Object2IntOpenHashMap<ParticleLimit> javaReforged$getTrackedParticleCounts();
}
