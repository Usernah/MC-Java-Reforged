package net.jr.mixin.runtime;

import net.jr.client.runtime.context.LocalClientExecution;
import net.minecraft.network.PacketProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Preserves the active local client for NeoForge work queued from a packet handler. */
@Mixin(PacketProcessor.class)
public abstract class PacketProcessorSSMixin {
    @ModifyVariable(
        method = "scheduleIfPossible(Ljava/lang/Runnable;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Runnable splitTest$carryClientIntoQueuedPayloadWork(Runnable task) {
        return LocalClientExecution.wrapScheduled(task);
    }
}
