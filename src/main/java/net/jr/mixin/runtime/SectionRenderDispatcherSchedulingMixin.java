package net.jr.mixin.runtime;

import net.jr.client.runtime.terrain.TerrainCompileBudget;
import net.minecraft.TracingExecutor;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Vanilla wakes a worker for every section as it is submitted. During split
 * scheduling that makes submission order into execution priority, so defer only
 * those wake-ups until every local viewport has contributed its requests.
 */
@Mixin(SectionRenderDispatcher.class)
public abstract class SectionRenderDispatcherSchedulingMixin {
    @Redirect(
        method = "schedule",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/TracingExecutor;execute(Ljava/lang/Runnable;)V"
        )
    )
    private void javaReforged$dispatchAfterSharedGather(TracingExecutor executor, Runnable task) {
        if (!TerrainCompileBudget.shouldDeferVanillaWake()) {
            executor.execute(task);
        }
    }
}
