package net.jr.mixin.accessors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.multiplayer.ClientChunkCache$Storage")
public interface ClientChunkCacheStorageAccessor {
    @Accessor("viewCenterX")
    int javaReforged$viewCenterX();

    @Accessor("viewCenterZ")
    int javaReforged$viewCenterZ();

    @Accessor("chunkRadius")
    int javaReforged$chunkRadius();

    @Accessor("viewRange")
    int javaReforged$viewRange();

    @Accessor("chunkCount")
    int javaReforged$chunkCount();
}
