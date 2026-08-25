package net.jr.mixin.runtime;

import net.jr.client.runtime.context.LocalClientExecution;
import net.jr.client.runtime.ClientRuntime;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.network.PacketProcessor$ListenerAndPacket")
public abstract class PacketProcessorListenerAndPacketMixin {
    @Redirect(
        method = "handle",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/Packet;handle(Lnet/minecraft/network/PacketListener;)V"
        )
    )
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void splitTest$handleQueuedPacketInListenerSlot(Packet packet, PacketListener listener) {
        if (listener instanceof ClientPacketListener clientListener
            && ClientRuntime.INSTANCE.connections().has(clientListener.getConnection())) {
            LocalClientExecution.runForListener(clientListener, () -> packet.handle(listener));
            return;
        }

        packet.handle(listener);
    }
}
