package net.jr.mixin.runtime;

import com.llamalad7.mixinextras.sugar.Local;
import net.jr.client.runtime.render.experimental.ExperimentalRenderMetrics;
import net.jr.client.runtime.terrain.TerrainTaskOwnership;

import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$CompileTask")
public abstract class SectionCompileTaskMixin {
    @Inject(
        method = "doTask",
        at = @At(
            value = "NEW",
            target = "Lnet/minecraft/client/renderer/chunk/CompiledSectionMesh;"
        ),
        cancellable = true
    )
    private void javaReforged$discardSupersededCompilation(
        SectionBufferBuilderPack buffers,
        CallbackInfoReturnable<SectionRenderDispatcher.RenderSection.SectionTask.SectionTaskResult> callback,
        @Local SectionCompiler.Results results
    ) {
        if (!TerrainTaskOwnership.isCurrent(this)) {
            results.release();
            callback.setReturnValue(
                SectionRenderDispatcher.RenderSection.SectionTask.SectionTaskResult.CANCELLED
            );
        }
    }

    @Redirect(method = "doTask", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;onSpinWait()V"))
    private void javaReforged$measureStagingRetry() {
        ExperimentalRenderMetrics.recordStagingRetry();
        Thread.onSpinWait();
    }
}
