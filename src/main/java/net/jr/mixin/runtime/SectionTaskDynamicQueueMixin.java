package net.jr.mixin.runtime;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import net.jr.client.runtime.terrain.TerrainCompileBudget;
import net.jr.client.runtime.terrain.TerrainTaskPriority;
import net.jr.client.runtime.terrain.TerrainTaskQueueExtension;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.chunk.SectionTaskDynamicQueue;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SectionTaskDynamicQueue.class)
public abstract class SectionTaskDynamicQueueMixin implements TerrainTaskQueueExtension {
    @Shadow @Final
    private List<SectionRenderDispatcher.RenderSection.SectionTask> tasks;

    @Unique
    private volatile int javaReforged$queuedTaskCount;

    @Override
    @Unique
    public int javaReforged$queuedTaskCount() {
        return this.javaReforged$queuedTaskCount;
    }

    @ModifyConstant(method = {"<init>", "poll"}, constant = @Constant(intValue = 2))
    private int javaReforged$useCorrectionBurstQuota(int quota) {
        return TerrainCompileBudget.recompileQuota();
    }

    @Inject(method = {"add", "clear"}, at = @At("RETURN"))
    private void javaReforged$publishQueueMutation(CallbackInfo ci) {
        // These methods hold vanilla's queue monitor. Readers only need the last
        // completed mutation, so they never wait for a worker's priority scan.
        this.javaReforged$queuedTaskCount = this.tasks.size();
    }

    @WrapOperation(
        method = "poll",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/SectionTaskDynamicQueue;removeTaskByIndex(I)Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection$SectionTask;"
        )
    )
    private SectionRenderDispatcher.RenderSection.SectionTask javaReforged$reserveSharedWorkBudget(
        SectionTaskDynamicQueue queue,
        int taskIndex,
        Operation<SectionRenderDispatcher.RenderSection.SectionTask> original
    ) {
        if (taskIndex < 0) {
            return original.call(queue, taskIndex);
        }
        SectionRenderDispatcher.RenderSection.SectionTask task = this.tasks.get(taskIndex);
        // Reserve at the exact point vanilla removes a task. This prevents a
        // correction elsewhere in the queue from granting every task an unlimited
        // bypass, while leaving the selected task queued for the next frame if its
        // lane has spent its budget.
        if (!TerrainCompileBudget.tryAcquire(task.isRecompile())) {
            return null;
        }
        return original.call(queue, taskIndex);
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
        // poll already removes cancelled tasks while selecting work. Publish even
        // when it returns null (empty queue or exhausted compile budget). Do not
        // use ownership as a cancellation test: transparency sorts are untracked.
        this.javaReforged$queuedTaskCount = this.tasks.size();
        SectionRenderDispatcher.RenderSection.SectionTask task = callback.getReturnValue();
        if (task != null) {
            TerrainTaskPriority.recordSelection(task, task.getRenderOrigin());
        }
    }
}
