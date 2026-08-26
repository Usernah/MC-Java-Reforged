package net.jr.mixin.runtime;

import net.jr.client.runtime.network.PacketExecutionRouter;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.network.PacketProcessor$ListenerAndPacket")
public abstract class ListenerAndPacketMixin {
    @Redirect(
        method = "handle",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/Packet;handle(Lnet/minecraft/network/PacketListener;)V"
        )
    )
    private void javaReforged$handleQueuedPacketInListenerSlot(Packet<?> packet, PacketListener listener) {
        PacketExecutionRouter.handleQueued(packet, listener);
    }
}
