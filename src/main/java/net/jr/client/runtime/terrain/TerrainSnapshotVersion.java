package net.jr.client.runtime.terrain;

import javax.annotation.Nullable;

public interface TerrainSnapshotVersion {
    @Nullable SharedTerrainStore.SnapshotVersion javaReforged$snapshotVersion();
    void javaReforged$snapshotVersion(@Nullable SharedTerrainStore.SnapshotVersion version);
}
