package net.jr.ClientRuntime.runtime;

import javax.annotation.Nullable;
import net.jr.ClientRuntime.state.TerrainState;
import net.jr.ClientRuntime.terrain.GlobalTerrainStore;

/** Runtime bridge for the one shared terrain pool and the active player's state. */
public final class LevelRendererFields {
    private static final TerrainState BOOTSTRAP_TERRAIN = new TerrainState();
    @Nullable
    private static GlobalTerrainStore terrainStore;

    private LevelRendererFields() {
    }

    public static TerrainState terrain() {
        LocalClient client = Client.currentOrNull();
        return client != null ? client.render().terrain() : BOOTSTRAP_TERRAIN;
    }

    public static @Nullable GlobalTerrainStore nullableTerrainStore() {
        return terrainStore;
    }

    public static void setTerrainStore(@Nullable GlobalTerrainStore store) {
        terrainStore = store;
    }
}
