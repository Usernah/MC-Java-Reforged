package net.jr.mixin.runtime;

import net.jr.client.runtime.terrain.TerrainCoordinator;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection")
public abstract class RenderSectionBufferGenerationMixin {
    @Inject(method = "vertexBufferUploadCallback", at = @At("RETURN"))
    private void javaReforged$invalidateTopologyAfterVertexUpload(
        CompiledSectionMesh sectionMesh,
        ChunkSectionLayer layer,
        CallbackInfo ci
    ) {
        TerrainCoordinator.onSectionBuffersChanged((SectionRenderDispatcher.RenderSection)(Object)this);
    }

    @Inject(method = "indexBufferUploadCallback", at = @At("RETURN"))
    private void javaReforged$invalidateTopologyAfterIndexUpload(
        CompiledSectionMesh sectionMesh,
        ChunkSectionLayer layer,
        boolean sortedIndexBuffer,
        CallbackInfo ci
    ) {
        TerrainCoordinator.onSectionBuffersChanged((SectionRenderDispatcher.RenderSection)(Object)this);
    }
}
