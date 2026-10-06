package net.jr.client.compat.sodium;

import java.util.IdentityHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Owns the terrain data that Sodium normally keeps in one renderer instance.
 *
 * <p>A state has its own section storage, render regions, visibility data and
 * uniforms. New states borrow the first manager's {@code ChunkBuilder}, so
 * they do not start another worker pool.</p>
 */
final class SodiumRendererStateRouter {
    private final Map<ClientLevel, State> states = new IdentityHashMap<>();
    private @Nullable Object sharedBuilder;

    void registerInitial(Object renderer) {
        ClientLevel level = SodiumRendererBridge.rendererLevel(renderer);
        Object manager = SodiumRendererBridge.sectionManager(renderer);
        Object uniforms = SodiumRendererBridge.uniformBufferManager(renderer);
        if (level == null || manager == null || uniforms == null) {
            return;
        }

        State existing = this.states.get(level);
        if (existing != null && existing.manager() != manager) {
            SodiumRendererBridge.releaseTerrainState(existing.manager());
            SodiumRendererBridge.deleteUniforms(existing.uniforms());
            if (this.sharedBuilder != null) {
                SodiumRendererBridge.replaceDedicatedBuilder(manager, this.sharedBuilder);
                SodiumRendererBridge.stopDedicatedCullExecutor(manager);
            }
            this.states.put(level, new State(manager, uniforms));
        } else if (existing == null) {
            if (this.sharedBuilder != null) {
                SodiumRendererBridge.replaceDedicatedBuilder(manager, this.sharedBuilder);
                SodiumRendererBridge.stopDedicatedCullExecutor(manager);
            }
            this.states.put(level, new State(manager, uniforms));
        }
        if (this.sharedBuilder == null) {
            this.sharedBuilder = SodiumRendererBridge.chunkBuilder(manager);
        }
    }

    boolean bindExistingRenderer(Object renderer, ClientLevel level) {
        this.registerInitial(renderer);
        if (this.sharedBuilder == null) {
            return false;
        }

        State state = this.stateFor(renderer, level);
        SodiumRendererBridge.bindRenderer(renderer, level, state.manager(), state.uniforms());
        return true;
    }

    void bindForView(Object renderer, ClientLevel level) {
        if (!this.bindExistingRenderer(renderer, level)) {
            throw new IllegalStateException("Sodium renderer is not initialized");
        }
    }

    boolean releaseRendererState(Object renderer) {
        Object manager = SodiumRendererBridge.sectionManager(renderer);
        Object uniforms = SodiumRendererBridge.uniformBufferManager(renderer);
        if (manager == null || uniforms == null) {
            return false;
        }

        this.states.entrySet().removeIf(entry -> entry.getValue().manager() == manager);
        SodiumRendererBridge.releaseTerrainState(manager);
        SodiumRendererBridge.deleteUniforms(uniforms);
        SodiumRendererBridge.clearRendererState(renderer);
        return true;
    }

    private State stateFor(Object renderer, ClientLevel level) {
        State state = this.states.get(level);
        if (state != null) {
            return state;
        }

        Object currentManager = SodiumRendererBridge.sectionManager(renderer);
        Object currentUniforms = SodiumRendererBridge.uniformBufferManager(renderer);
        if (currentManager == null || currentUniforms == null || this.sharedBuilder == null) {
            throw new IllegalStateException("Sodium renderer state is incomplete");
        }

        Object manager = SodiumRendererBridge.createManagerLike(currentManager, level);
        SodiumRendererBridge.replaceDedicatedBuilder(manager, this.sharedBuilder);
        SodiumRendererBridge.stopDedicatedCullExecutor(manager);
        Object uniforms = SodiumRendererBridge.createUniformsLike(
            currentUniforms,
            level,
            SodiumRendererBridge.renderDistance(currentManager)
        );
        state = new State(manager, uniforms);
        this.states.put(level, state);
        return state;
    }

    private record State(Object manager, Object uniforms) {
    }
}
