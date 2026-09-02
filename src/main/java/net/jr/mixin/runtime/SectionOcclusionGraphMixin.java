package net.jr.mixin.runtime;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.jr.client.runtime.context.LocalClientScope;
import net.jr.client.runtime.terrain.ClientWorkBackpressure;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SectionOcclusionGraph.class)
public abstract class SectionOcclusionGraphMixin {
    @Unique
    private int javaReforged$acquiredGraphSlot = -1;

    @Inject(method = "scheduleFullUpdate", at = @At("HEAD"), cancellable = true)
    private void javaReforged$admitFullGraphUpdate(CameraRenderState camera, CallbackInfo ci) {
        var client = LocalClientScope.currentClientOrNull();
        if (client == null) {
            return;
        }
        int slotId = client.slotId();
        if (!ClientWorkBackpressure.tryAcquireGraphRebuild(slotId)) {
            ci.cancel();
            return;
        }
        this.javaReforged$acquiredGraphSlot = slotId;
    }

    @WrapOperation(
        method = "scheduleFullUpdate",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/concurrent/CompletableFuture;runAsync(Ljava/lang/Runnable;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"
        )
    )
    private CompletableFuture<Void> javaReforged$releaseGraphPermitWhenComplete(
        Runnable task,
        Executor executor,
        Operation<CompletableFuture<Void>> original
    ) {
        int slotId = this.javaReforged$acquiredGraphSlot;
        this.javaReforged$acquiredGraphSlot = -1;
        CompletableFuture<Void> future = original.call(task, executor);
        if (slotId >= 0) {
            future.whenComplete((unused, error) -> ClientWorkBackpressure.releaseGraphRebuild(slotId));
        }
        return future;
    }
}
