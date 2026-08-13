package net.jr.mixin.SSM;

import net.jr.ClientRuntime.runtime.LocalPlayers;
import net.jr.ClientRuntime.runtime.ActiveSlot;
import net.jr.ClientRuntime.runtime.ClientBoundary;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Connection.class)
public abstract class ConnectionMixin {
    @Redirect(
        method = "channelRead0",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V")
    )
    private void splitTest$routePacketToSlot(Packet<?> packet, PacketListener listener) {
        Connection connection = (Connection)(Object)this;
        Minecraft minecraft = Minecraft.getInstance();
        int slotId;
        Integer boundSlotId = LocalPlayers.INSTANCE.connections().slotOrNull(connection);
        if (boundSlotId != null) {
            slotId = boundSlotId;
        } else {
            Integer activeSlotId = ActiveSlot.idOrNull();
            slotId = activeSlotId != null ? activeSlotId : 0;
            LocalPlayers.INSTANCE.connections().bind(connection, slotId);
        }
        if (minecraft != null && minecraft.isSameThread()) {
            ClientBoundary.runForConnection(connection, () -> splitTest$handlePacket(packet, listener));
            return;
        }

        try (ActiveSlot.Scheduling ignored = ActiveSlot.schedule(slotId)) {
            splitTest$handlePacket(packet, listener);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void splitTest$handlePacket(Packet packet, PacketListener listener) {
        packet.handle(listener);
    }
}
