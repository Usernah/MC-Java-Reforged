package net.jr.mixin.compat.bettercombat;

import net.bettercombat.client.ClientNetwork;
import net.jr.client.runtime.context.InstanceExecution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = ClientNetwork.class, remap = false)
abstract class ClientNetworkMixin {
    @ModifyArg(
        method = {"handleAttackAnimation", "handleAttackSound"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;execute(Ljava/lang/Runnable;)V"
        ),
        index = 0
    )
    private static Runnable javaReforged$captureLocalClient(Runnable action) {
        return InstanceExecution.capture(action);
    }
}
