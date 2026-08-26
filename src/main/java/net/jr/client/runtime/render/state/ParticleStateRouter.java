package net.jr.client.runtime.render.state;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Map;
import java.util.Queue;
import javax.annotation.Nullable;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;
import net.jr.client.runtime.state.ParticleState;
import net.jr.mixin.accessors.ParticleEngineAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.core.particles.ParticleLimit;

public final class ParticleStateRouter {
    private static final ParticleState BOOTSTRAP_STATE = new ParticleState();

    private ParticleStateRouter() {
    }

    public static void installActiveLevel(ParticleEngine engine) {
        ((ParticleEngineAccessor)engine).javaReforged$setLevel(activeSlot().renderState().level());
    }

    public static Map<ParticleRenderType, ParticleGroup<?>> particles() {
        return state().particles();
    }

    public static Map<ParticleRenderType, ParticleGroup<?>> particles(ParticleEngine engine) {
        return state().particles();
    }

    public static Queue<TrackingEmitter> trackingEmitters() {
        return state().trackingEmitters();
    }

    public static Queue<TrackingEmitter> trackingEmitters(ParticleEngine engine) {
        return state().trackingEmitters();
    }

    public static Queue<Particle> particlesToAdd() {
        return state().particlesToAdd();
    }

    public static Queue<Particle> particlesToAdd(ParticleEngine engine) {
        return state().particlesToAdd();
    }

    public static Object2IntOpenHashMap<ParticleLimit> trackedParticleCounts() {
        return state().trackedParticleCounts();
    }

    public static Object2IntOpenHashMap<ParticleLimit> trackedParticleCounts(ParticleEngine engine) {
        return state().trackedParticleCounts();
    }

    public static void setGlobalLevel(ParticleEngine engine, @Nullable ClientLevel level) {
        ((ParticleEngineAccessor)engine).javaReforged$setLevel(level);
        clearAll(engine);
    }

    public static void clearAll(ParticleEngine engine) {
        BOOTSTRAP_STATE.clear();
        for (int slotId = 0; slotId < LocalClientSlotRegistry.MAX_SLOTS; slotId++) {
            ClientRuntime.INSTANCE.slots().slot(slotId).renderState().particles().clear();
        }
        ParticleEngineAccessor accessor = (ParticleEngineAccessor)engine;
        accessor.javaReforged$getParticles().clear();
        accessor.javaReforged$getTrackingEmitters().clear();
        accessor.javaReforged$getParticlesToAdd().clear();
        accessor.javaReforged$getTrackedParticleCounts().clear();
    }

    private static ParticleState state() {
        Integer slotId = SlotScope.idOrNull();
        return slotId != null
            ? ClientRuntime.INSTANCE.slots().slot(slotId).renderState().particles()
            : BOOTSTRAP_STATE;
    }

    private static LocalClientSlot activeSlot() {
        Integer slotId = SlotScope.idOrNull();
        return ClientRuntime.INSTANCE.slots().slot(slotId != null ? slotId : 0);
    }
}
