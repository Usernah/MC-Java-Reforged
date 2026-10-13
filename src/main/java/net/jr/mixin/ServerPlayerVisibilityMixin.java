package net.jr.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerVisibilityMixin {
    @Inject(method = "broadcastToPlayer", at = @At("HEAD"), cancellable = true)
    private void javaReforged$hideExpiredDeadAvatar(
        ServerPlayer viewer,
        CallbackInfoReturnable<Boolean> cir
    ) {
        ServerPlayer player = (ServerPlayer)(Object)this;
        if (player.deathTime >= 20 && player.isDeadOrDying()) {
            cir.setReturnValue(false);
        }
    }
}
