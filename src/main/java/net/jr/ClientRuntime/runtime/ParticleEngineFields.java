package net.jr.ClientRuntime.runtime;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Map;
import java.util.Queue;
import net.jr.mixin.SSM.ParticleEngineSSAccessor;
import net.jr.ClientRuntime.state.ParticleState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.core.particles.ParticleLimit;
import javax.annotation.Nullable;

public final class ParticleEngineFields {
    private ParticleEngineFields() {
    }

    public static void installActiveLevel(ParticleEngine engine) {
        ClientLevel level = Client.level();
        ((ParticleEngineSSAccessor)engine).splitTest$setLevel(level);
    }

    public static Map<ParticleRenderType, ParticleGroup<?>> particles() {
        return state().particles();
    }

    public static Map<ParticleRenderType, ParticleGroup<?>> particles(ParticleEngine engine) {
        LocalClient client = Client.currentOrNull();
        return client != null
            ? client.render().particles().particles()
            : ((ParticleEngineSSAccessor)engine).splitTest$getParticles();
    }

    public static Queue<TrackingEmitter> trackingEmitters() {
        return state().trackingEmitters();
    }

    public static Queue<TrackingEmitter> trackingEmitters(ParticleEngine engine) {
        LocalClient client = Client.currentOrNull();
        return client != null
            ? client.render().particles().trackingEmitters()
            : ((ParticleEngineSSAccessor)engine).splitTest$getTrackingEmitters();
    }

    public static Queue<Particle> particlesToAdd() {
        return state().particlesToAdd();
    }

    public static Queue<Particle> particlesToAdd(ParticleEngine engine) {
        LocalClient client = Client.currentOrNull();
        return client != null
            ? client.render().particles().particlesToAdd()
            : ((ParticleEngineSSAccessor)engine).splitTest$getParticlesToAdd();
    }

    public static Object2IntOpenHashMap<ParticleLimit> trackedParticleCounts() {
        return state().trackedParticleCounts();
    }

    public static Object2IntOpenHashMap<ParticleLimit> trackedParticleCounts(ParticleEngine engine) {
        LocalClient client = Client.currentOrNull();
        return client != null
            ? client.render().particles().trackedParticleCounts()
            : ((ParticleEngineSSAccessor)engine).splitTest$getTrackedParticleCounts();
    }

    public static void setGlobalLevel(ParticleEngine engine, @Nullable ClientLevel level) {
        ((ParticleEngineSSAccessor)engine).splitTest$setLevel(level);
        clearAll(engine);
    }

    public static void clearAll(ParticleEngine engine) {
        for (int slotId = 0; slotId < net.jr.ClientRuntime.slot.PlayerSlots.MAX_SLOTS; slotId++) {
            LocalPlayers.INSTANCE.slots().slot(slotId).renderState().particles().clear();
        }
        ParticleEngineSSAccessor accessor = (ParticleEngineSSAccessor)engine;
        accessor.splitTest$getParticles().clear();
        accessor.splitTest$getTrackingEmitters().clear();
        accessor.splitTest$getParticlesToAdd().clear();
        accessor.splitTest$getTrackedParticleCounts().clear();
    }

    private static ParticleState state() {
        return Client.render().particles();
    }
}
