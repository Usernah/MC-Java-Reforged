package net.jr.mixin.runtime;

import net.jr.client.runtime.terrain.SharedTerrainStore;
import net.jr.client.runtime.terrain.TerrainSnapshotVersion;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(RenderSectionRegion.class)
public abstract class TerrainSnapshotVersionMixin implements TerrainSnapshotVersion {
    @Unique private SharedTerrainStore.SnapshotVersion javaReforged$version;

    @Override public SharedTerrainStore.SnapshotVersion javaReforged$snapshotVersion() {
        return this.javaReforged$version;
    }

    @Override public void javaReforged$snapshotVersion(SharedTerrainStore.SnapshotVersion version) {
        this.javaReforged$version = version;
    }
}
