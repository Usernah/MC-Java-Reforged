package net.jr.mixin.runtime;

import net.jr.client.runtime.network.PacketExecutionRouter;
import net.minecraft.network.PacketProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PacketProcessor.class)
public abstract class PacketProcessorMixin {
    @ModifyVariable(
        method = "scheduleIfPossible(Ljava/lang/Runnable;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Runnable javaReforged$carryClientIntoQueuedPayloadWork(Runnable task) {
        return PacketExecutionRouter.wrapScheduled(task);
    }
}
