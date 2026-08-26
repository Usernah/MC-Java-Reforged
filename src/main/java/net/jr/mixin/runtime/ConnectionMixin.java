package net.jr.mixin.runtime;

import net.jr.client.runtime.network.PacketExecutionRouter;
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
    private void javaReforged$routePacketToSlot(Packet<?> packet, PacketListener listener) {
        PacketExecutionRouter.handleIncoming((Connection) (Object) this, packet, listener);
    }
}
