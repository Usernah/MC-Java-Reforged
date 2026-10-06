package net.jr.mixin.runtime;

import net.jr.client.runtime.player.PositionSync;
import net.jr.client.runtime.player.PlayerPolicy;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Inject(method = "isControlledCamera", at = @At("HEAD"), cancellable = true)
    private void javaReforged$isControlledCamera(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(PlayerPolicy.isControlledCamera((LocalPlayer)(Object)this));
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void javaReforged$reportPositionWhenChunkIsMissing(CallbackInfo ci) {
        PositionSync.afterTick((LocalPlayer)(Object)this);
    }

    @Redirect(
        method = "tickDeath",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V")
    )
    private void javaReforged$retainLocalCameraPlayerUntilRespawn(LocalPlayer player, Entity.RemovalReason removalReason) {
        if (!PlayerPolicy.shouldRetainDeadClientPlayer(player)) {
            player.remove(removalReason);
        }
    }
}
