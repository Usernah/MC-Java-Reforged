package net.jr.mixin.SSM;

import net.jr.ClientRuntime.runtime.ClientBoundary;
import net.jr.ClientRuntime.runtime.LocalPlayers;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Restores the local-client identity when Minecraft drains its 26.2 packet queue.
 *
 * <p>PacketProcessor stores the listener and packet directly, so an execution
 * context installed on the Netty thread cannot travel with the queued entry.
 * The listener's Connection is the stable owner of that entry and therefore
 * the authoritative place to recover the slot before any packet handler runs.</p>
 */
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
            && LocalPlayers.INSTANCE.connections().has(clientListener.getConnection())) {
            ClientBoundary.runForListener(clientListener, () -> packet.handle(listener));
            return;
        }

        packet.handle(listener);
    }
}
