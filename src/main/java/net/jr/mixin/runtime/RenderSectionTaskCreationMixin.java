package net.jr.mixin.runtime;

import java.util.List;
import net.jr.client.runtime.terrain.TerrainTaskOwnership;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection")
public abstract class RenderSectionTaskCreationMixin {
    @Inject(
        method = "createCompileTask(Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection$SectionTask;",
        at = @At("RETURN")
    )
    private void javaReforged$registerSharedCompileTask(
        RenderSectionRegion region,
        List<?> additionalRenderers,
        CallbackInfoReturnable<SectionRenderDispatcher.RenderSection.SectionTask> callback
    ) {
        SectionRenderDispatcher.RenderSection.SectionTask task = callback.getReturnValue();
        if (task != null) {
            TerrainTaskOwnership.taskCreated(
                (SectionRenderDispatcher.RenderSection)(Object)this,
                task,
                true,
                region
            );
        }
    }
}
