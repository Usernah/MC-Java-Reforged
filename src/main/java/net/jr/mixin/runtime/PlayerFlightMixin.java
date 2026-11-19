package net.jr.mixin.runtime;

import net.jr.client.runtime.player.LegacyGameplayModes;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerFlightMixin {
    @Inject(method = "getFlyingSpeed", at = @At("HEAD"), cancellable = true)
    private void javaReforged$useLegacyBaseFlightSpeed(CallbackInfoReturnable<Float> cir) {
        if ((Object)this instanceof LocalPlayer player
                && player.getAbilities().flying
                && !player.isPassenger()
                && LegacyGameplayModes.usesLegacyFlight(player)) {
            cir.setReturnValue(player.getAbilities().getFlyingSpeed());
        }
    }
}
