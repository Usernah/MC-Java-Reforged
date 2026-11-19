package net.jr.client.compat.sodium;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.jr.client.runtime.render.world.WorldRenderView;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;


final class SodiumTerrainUnion {
    private final Map<ChunkKey, ChunkOwner> desiredChunks = new LinkedHashMap<>();
    private final Map<ChunkKey, ChunkOwner> appliedChunks = new HashMap<>();
    @Nullable
    private Object appliedManager;

    void beginFrame(List<WorldRenderView> views) {
        Map<ChunkKey, ChunkOwner> next = new LinkedHashMap<>();



        for (WorldRenderView view : views) {
            if (view.updatesTerrain()) {
                collectReadyChunks(next, view);
            }
        }
        for (WorldRenderView view : views) {
            if (!view.updatesTerrain()) {
                collectReadyChunks(next, view);
            }
        }

        this.desiredChunks.clear();
        this.desiredChunks.putAll(next);
    }

    private void collectReadyChunks(Map<ChunkKey, ChunkOwner> chunks, WorldRenderView view) {
        ClientLevel level = view.level();
        for (long position : SodiumRendererBridge.readyChunks(level)) {
            ChunkKey key = new ChunkKey(
                level.dimension(),
                ChunkPos.getX(position),
                ChunkPos.getZ(position)
            );
            ChunkOwner previous = this.desiredChunks.get(key);
            if (previous != null && previous.level() == level) {
                chunks.putIfAbsent(key, previous);
            } else {
                chunks.putIfAbsent(key, new ChunkOwner(level));
            }
        }
    }

    void synchronize(Object renderer, ClientLevel activeLevel) {
        Object manager = SodiumRendererBridge.sectionManager(renderer);
        if (manager == null) {
            return;
        }
        if (manager != this.appliedManager) {
            this.appliedManager = manager;
            this.appliedChunks.clear();
        }

        SodiumRendererBridge.beforeSectionUpdates(manager);

        for (ChunkKey key : List.copyOf(this.appliedChunks.keySet())) {
            if (!this.desiredChunks.containsKey(key)) {
                SodiumRendererBridge.onChunkRemoved(manager, key.x(), key.z());
                this.appliedChunks.remove(key);
            }
        }

        for (Map.Entry<ChunkKey, ChunkOwner> entry : this.desiredChunks.entrySet()) {
            ChunkKey key = entry.getKey();
            ChunkOwner owner = entry.getValue();
            if (!this.appliedChunks.containsKey(key)) {
                SodiumRendererBridge.activateTerrainSource(manager, owner.level());
                SodiumRendererBridge.onChunkAdded(manager, key.x(), key.z());
            }
            this.appliedChunks.put(key, owner);
        }

        SodiumRendererBridge.activateTerrainSource(manager, activeLevel);
    }

    ClientLevel sourceFor(SectionPos position, ClientLevel fallback) {
        ChunkKey key = new ChunkKey(fallback.dimension(), position.x(), position.z());
        ChunkOwner owner = this.desiredChunks.get(key);
        return owner != null ? owner.level() : fallback;
    }

    private record ChunkKey(ResourceKey<Level> dimension, int x, int z) {
    }

    private record ChunkOwner(ClientLevel level) {
    }
}
