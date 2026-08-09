package net.alnv.javareforged.ClientRuntime.runtime;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Map;
import java.util.Queue;
import net.alnv.javareforged.mixin.SSM.ParticleEngineSSAccessor;
import net.alnv.javareforged.ClientRuntime.state.ParticleState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.core.particles.ParticleGroup;

public final class ParticleEngineFields {
    private ParticleEngineFields() {
    }

    public static void installActiveLevel(ParticleEngine engine) {
        ClientLevel level = Client.level();
        ((ParticleEngineSSAccessor)engine).splitTest$setLevel(level);
    }

    public static Map<ParticleRenderType, Queue<Particle>> particles() {
        return state().particles();
    }

    public static Queue<TrackingEmitter> trackingEmitters() {
        return state().trackingEmitters();
    }

    public static Queue<Particle> particlesToAdd() {
        return state().particlesToAdd();
    }

    public static Object2IntOpenHashMap<ParticleGroup> trackedParticleCounts() {
        return state().trackedParticleCounts();
    }

    private static ParticleState state() {
        return Client.render().particles();
    }
}
