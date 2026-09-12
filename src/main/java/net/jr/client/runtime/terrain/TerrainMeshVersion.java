package net.jr.client.runtime.terrain;

import javax.annotation.Nullable;

public interface TerrainMeshVersion {
    @Nullable SharedTerrainStore.BuildTicket javaReforged$buildTicket();
    void javaReforged$buildTicket(SharedTerrainStore.BuildTicket ticket);
}
