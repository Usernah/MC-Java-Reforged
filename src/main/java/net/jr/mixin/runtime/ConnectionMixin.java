package net.jr.mixin.runtime;

import net.jr.client.runtime.network.PacketExecutionRouter;
import net.jr.client.runtime.network.ConnectionSlotRegistry;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class ConnectionMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void javaReforged$bindConnectionBeforeActivation(PacketFlow receiving, CallbackInfo ci) {
        ConnectionSlotRegistry.bindConstructed((Connection)(Object)this);
    }

    @Redirect(
        method = "channelRead0",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V")
    )
    private void javaReforged$routePacketToSlot(Packet<?> packet, PacketListener listener) {
        PacketExecutionRouter.handleIncoming((Connection) (Object) this, packet, listener);
    }

    @Redirect(
        method = "handleDisconnection",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/PacketListener;onDisconnect(Lnet/minecraft/network/DisconnectionDetails;)V"
        )
    )
    private void javaReforged$routeDisconnectToConnectionOwner(
        PacketListener listener,
        DisconnectionDetails details
    ) {
        PacketExecutionRouter.handleDisconnect((Connection)(Object)this, listener, details);
    }
}
