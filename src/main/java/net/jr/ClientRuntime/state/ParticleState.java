package net.jr.ClientRuntime.state;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Queue;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.core.particles.ParticleLimit;

public final class ParticleState {
    private final Map<ParticleRenderType, ParticleGroup<?>> particles = new IdentityHashMap<>();
    private final Queue<TrackingEmitter> trackingEmitters = new ArrayDeque<>();
    private final Queue<Particle> particlesToAdd = new ArrayDeque<>();
    private final Object2IntOpenHashMap<ParticleLimit> trackedParticleCounts = new Object2IntOpenHashMap<>();

    public Map<ParticleRenderType, ParticleGroup<?>> particles() {
        return this.particles;
    }

    public Queue<TrackingEmitter> trackingEmitters() {
        return this.trackingEmitters;
    }

    public Queue<Particle> particlesToAdd() {
        return this.particlesToAdd;
    }

    public Object2IntOpenHashMap<ParticleLimit> trackedParticleCounts() {
        return this.trackedParticleCounts;
    }

    public void clear() {
        this.particles.clear();
        this.trackingEmitters.clear();
        this.particlesToAdd.clear();
        this.trackedParticleCounts.clear();
    }
}
