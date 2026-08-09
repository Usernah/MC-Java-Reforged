package net.alnv.javareforged.ClientRuntime.state;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.core.particles.ParticleGroup;
import net.neoforged.neoforge.client.ClientHooks;

public final class ParticleState {
    private static final List<ParticleRenderType> RENDER_ORDER = List.of(
            ParticleRenderType.TERRAIN_SHEET,
            ParticleRenderType.PARTICLE_SHEET_OPAQUE,
            ParticleRenderType.PARTICLE_SHEET_LIT,
            ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT,
            ParticleRenderType.CUSTOM
    );

    private final Map<ParticleRenderType, Queue<Particle>> particles =
            new TreeMap<>(ClientHooks.makeParticleRenderTypeComparator(RENDER_ORDER));
    private final Queue<TrackingEmitter> trackingEmitters = new ArrayDeque<>();
    private final Queue<Particle> particlesToAdd = new ArrayDeque<>();
    private final Object2IntOpenHashMap<ParticleGroup> trackedParticleCounts = new Object2IntOpenHashMap<>();

    public Map<ParticleRenderType, Queue<Particle>> particles() {
        return this.particles;
    }

    public Queue<TrackingEmitter> trackingEmitters() {
        return this.trackingEmitters;
    }

    public Queue<Particle> particlesToAdd() {
        return this.particlesToAdd;
    }

    public Object2IntOpenHashMap<ParticleGroup> trackedParticleCounts() {
        return this.trackedParticleCounts;
    }

    public void clear() {
        this.particles.clear();
        this.trackingEmitters.clear();
        this.particlesToAdd.clear();
        this.trackedParticleCounts.clear();
    }
}