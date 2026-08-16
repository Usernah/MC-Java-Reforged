package net.jr.mixin.runtime;

import net.jr.client.runtime.player.LocalPlayerPositionSync;
import net.jr.client.runtime.player.LocalPlayerPolicy;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerSSMixin {
    @Inject(method = "isControlledCamera", at = @At("HEAD"), cancellable = true)
    private void splitTest$isControlledCamera(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(LocalPlayerPolicy.isControlledCamera((LocalPlayer)(Object)this));
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void splitTest$reportPositionWhenChunkIsMissing(CallbackInfo ci) {
        LocalPlayerPositionSync.afterTick((LocalPlayer)(Object)this);
    }

    @Redirect(
        method = "tickDeath",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V")
    )
    private void splitTest$retainLocalCameraPlayerUntilRespawn(LocalPlayer player, Entity.RemovalReason removalReason) {
        if (!LocalPlayerPolicy.shouldRetainDeadClientPlayer(player)) {
            player.remove(removalReason);
        }
    }
}
