package net.jr.client.runtime.bridge;

import javax.annotation.Nullable;

import net.jr.client.runtime.context.LocalClientAcces;
import net.jr.client.runtime.context.LocalClient;
import net.jr.client.runtime.state.TerrainState;
import net.jr.client.runtime.terrain.SharedTerrainStore;

/** Runtime bridge for the one shared terrain pool and the active player's state. */
public final class LevelRendererStateAccess {
    private static final TerrainState BOOTSTRAP_TERRAIN = new TerrainState();
    @Nullable
    private static SharedTerrainStore terrainStore;

    private LevelRendererStateAccess() {
    }

    public static TerrainState terrain() {
        LocalClient client = LocalClientAcces.currentOrNull();
        return client != null ? client.render().terrain() : BOOTSTRAP_TERRAIN;
    }

    public static @Nullable SharedTerrainStore nullableTerrainStore() {
        return terrainStore;
    }

    public static void setTerrainStore(@Nullable SharedTerrainStore store) {
        terrainStore = store;
    }
}
