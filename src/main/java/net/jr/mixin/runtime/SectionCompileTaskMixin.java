package net.jr.mixin.runtime;

import net.jr.client.runtime.render.experimental.ExperimentalRenderMetrics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$CompileTask")
public abstract class SectionCompileTaskMixin {
    @Redirect(method = "doTask", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;onSpinWait()V"))
    private void javaReforged$measureStagingRetry() {
        ExperimentalRenderMetrics.recordStagingRetry();
        Thread.onSpinWait();
    }
}
