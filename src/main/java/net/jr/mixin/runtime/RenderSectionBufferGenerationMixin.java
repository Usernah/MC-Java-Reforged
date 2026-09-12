package net.jr.mixin.runtime;

import net.jr.client.runtime.terrain.TerrainCoordinator;
import net.jr.client.runtime.terrain.TerrainMeshVersion;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection")
public abstract class RenderSectionBufferGenerationMixin {
    @WrapMethod(method = "setSectionMesh")
    private SectionMesh javaReforged$publishOnlyCurrentRevision(SectionMesh candidate, Operation<SectionMesh> original) {
        if (candidate instanceof TerrainMeshVersion version && version.javaReforged$buildTicket() != null) {
            return version.javaReforged$buildTicket().publish(candidate, () -> original.call(candidate));
        }
        return original.call(candidate);
    }

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
