package net.jr.mixin.SSM;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.multiplayer.ClientChunkCache$Storage")
public interface ClientChunkCacheStorageAccessor {
    @Accessor("viewCenterX")
    int splitTest$viewCenterX();

    @Accessor("viewCenterZ")
    int splitTest$viewCenterZ();

    @Accessor("chunkRadius")
    int splitTest$chunkRadius();

    @Accessor("viewRange")
    int splitTest$viewRange();

    @Accessor("chunkCount")
    int splitTest$chunkCount();
}
