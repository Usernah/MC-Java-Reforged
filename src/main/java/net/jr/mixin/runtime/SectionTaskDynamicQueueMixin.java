package net.jr.mixin.runtime;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import net.jr.client.runtime.terrain.TerrainCompileBudget;
import net.jr.client.runtime.terrain.TerrainTaskPriority;
import net.jr.client.runtime.terrain.TerrainTaskOwnership;
import net.jr.client.runtime.terrain.TerrainTaskQueueExtension;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.chunk.SectionTaskDynamicQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SectionTaskDynamicQueue.class)
public abstract class SectionTaskDynamicQueueMixin implements TerrainTaskQueueExtension {
    @Shadow @Final
    private List<SectionRenderDispatcher.RenderSection.SectionTask> tasks;

    @Override
    @Unique
    public synchronized int javaReforged$pruneUntrackedTasks() {
        this.tasks.removeIf(task -> !TerrainTaskOwnership.isTracked(task));
        return this.tasks.size();
    }

    @Inject(method = "poll", at = @At("HEAD"), cancellable = true)
    private void javaReforged$enforceCompileBudget(
        Vec3 cameraPos,
        CallbackInfoReturnable<SectionRenderDispatcher.RenderSection.SectionTask> callback
    ) {
        if (!this.tasks.isEmpty() && !TerrainCompileBudget.tryAcquirePermit()) {
            callback.setReturnValue(null);
        }
    }

    @ModifyExpressionValue(
        method = "poll",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/core/BlockPos;distToCenterSqr(Lnet/minecraft/core/Position;)D"
        )
    )
    private double javaReforged$useNearestReferencingCamera(
        double vanillaDistance,
        @Local SectionRenderDispatcher.RenderSection.SectionTask task
    ) {
        return TerrainTaskPriority.distanceSquared(task, task.getRenderOrigin(), vanillaDistance);
    }

    @Inject(method = "poll", at = @At("RETURN"))
    private void javaReforged$recordSelectedCamera(
        Vec3 cameraPos,
        CallbackInfoReturnable<SectionRenderDispatcher.RenderSection.SectionTask> callback
    ) {
        SectionRenderDispatcher.RenderSection.SectionTask task = callback.getReturnValue();
        if (task != null) {
            TerrainCompileBudget.recordDispatch();
            TerrainTaskPriority.recordSelection(task, task.getRenderOrigin());
        }
    }
}
