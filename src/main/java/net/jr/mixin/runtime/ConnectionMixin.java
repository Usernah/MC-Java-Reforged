package net.jr.mixin.runtime;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.context.LocalClientExecution;
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
        Integer boundSlotId = ClientRuntime.INSTANCE.connections().slotOrNull(connection);
        if (boundSlotId != null) {
            slotId = boundSlotId;
        } else {
            Integer activeSlotId = SlotScope.idOrNull();
            slotId = activeSlotId != null ? activeSlotId : 0;
            ClientRuntime.INSTANCE.connections().bind(connection, slotId);
        }
        if (minecraft != null && minecraft.isSameThread()) {
            LocalClientExecution.runForConnection(connection, () -> splitTest$handlePacket(packet, listener));
            return;
        }

        try (SlotScope.Scheduling ignored = SlotScope.schedule(slotId)) {
            splitTest$handlePacket(packet, listener);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void splitTest$handlePacket(Packet packet, PacketListener listener) {
        packet.handle(listener);
    }
}
