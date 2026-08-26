package net.jr.mixin;

import net.jr.client.runtime.player.SecondaryPlayerNameResolver;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    private void javaReforged$resolveSplitDisplayName(CallbackInfoReturnable<Component> cir) {
        cir.setReturnValue(SecondaryPlayerNameResolver.resolve((Player) (Object) this, cir.getReturnValue()));
    }
}
