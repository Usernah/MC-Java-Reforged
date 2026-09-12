package net.jr.mixin.runtime;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
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
    @ModifyExpressionValue(method = "doTask", at = @At(value = "NEW", target = "net/minecraft/client/renderer/chunk/CompiledSectionMesh"))
    private CompiledSectionMesh javaReforged$carryRevisionToPublication(CompiledSectionMesh mesh) {
        TerrainTaskOwnership.attachMesh(this, mesh);
        return mesh;
    }

    @org.spongepowered.asm.mixin.Unique
    private long javaReforged$startedAt;

    @Inject(method = "doTask", at = @At("HEAD"))
    private void javaReforged$startTaskTimer(
        SectionBufferBuilderPack buffers,
        CallbackInfoReturnable<SectionRenderDispatcher.RenderSection.SectionTask.SectionTaskResult> callback
    ) {
        this.javaReforged$startedAt = System.nanoTime();
    }

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

    @Inject(method = "doTask", at = @At("RETURN"))
    private void javaReforged$recordTaskTime(
        SectionBufferBuilderPack buffers,
        CallbackInfoReturnable<SectionRenderDispatcher.RenderSection.SectionTask.SectionTaskResult> callback
    ) {
        long startedAt = this.javaReforged$startedAt;
        if (startedAt != 0L) {
            SectionRenderDispatcher.RenderSection.SectionTask task =
                (SectionRenderDispatcher.RenderSection.SectionTask)(Object)this;
            net.jr.client.runtime.terrain.TerrainCompileBudget.recordCompletedTask(
                task.isRecompile(),
                System.nanoTime() - startedAt
            );
            this.javaReforged$startedAt = 0L;
        }
    }

    @Redirect(method = "doTask", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;onSpinWait()V"))
    private void javaReforged$measureStagingRetry() {
        ExperimentalRenderMetrics.recordStagingRetry();
        Thread.onSpinWait();
    }
}
