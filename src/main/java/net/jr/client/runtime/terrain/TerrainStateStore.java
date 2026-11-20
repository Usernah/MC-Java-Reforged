package net.jr.client.runtime.terrain;

import javax.annotation.Nullable;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotExecution;
import net.jr.client.runtime.state.TerrainState;
import net.jr.client.runtime.terrain.SharedTerrainStore;

public final class TerrainStateStore {
    private static final TerrainState BOOTSTRAP_TERRAIN = new TerrainState();
    @Nullable
    private static SharedTerrainStore terrainStore;

    private TerrainStateStore() {
    }

    public static TerrainState terrain() {
        Integer slotId = SlotExecution.currentSlotIdOrNull();
        return slotId != null
            ? ClientRuntime.INSTANCE.slots().slot(slotId).renderState().terrain()
            : BOOTSTRAP_TERRAIN;
    }

    public static @Nullable SharedTerrainStore nullableTerrainStore() {
        return terrainStore;
    }

    public static void setTerrainStore(@Nullable SharedTerrainStore store) {
        terrainStore = store;
    }
}
