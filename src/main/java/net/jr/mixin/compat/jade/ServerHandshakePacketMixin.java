package net.jr.mixin.compat.jade;

import net.jr.client.compat.jade.JadeRuntimeState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import snownee.jade.network.ClientPayloadContext;
import snownee.jade.network.ServerHandshakePacket;

@Mixin(value = ServerHandshakePacket.class, remap = false)
abstract class ServerHandshakePacketMixin {
    @Redirect(
        method = "handle",
        at = @At(
            value = "INVOKE",
            target = "Lsnownee/jade/network/ClientPayloadContext;execute(Ljava/lang/Runnable;)V"
        )
    )
    private static void javaReforged$handleCurrentClient(
        ClientPayloadContext context,
        Runnable action
    ) {
        context.execute(JadeRuntimeState.wrap(action));
    }
}
