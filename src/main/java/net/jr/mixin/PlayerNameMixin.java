package net.jr.mixin;

import net.jr.ClientRuntime.player.SplitPlayerName;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps split players' protocol-safe names separate from the names shown by the client UI. */
@Mixin(Player.class)
public abstract class PlayerNameMixin {
    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    private void javaReforged$useSplitDisplayName(CallbackInfoReturnable<Component> callback) {
        Player player = (Player) (Object) this;
        Component displayName = SplitPlayerName.visibleName(player.getGameProfile());
        if (displayName != null) {
            callback.setReturnValue(displayName.copy().withStyle(callback.getReturnValue().getStyle()));
        }
    }
}
