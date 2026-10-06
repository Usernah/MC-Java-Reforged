package net.jr.mixin.minigame;

import net.jr.experimental_features.minigame.client.BattleClientState;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerExplosion.class)
public class ServerLevelMixin {

    @Inject(method = "interactsWithBlocks", at = @At("HEAD"), cancellable = true)
    private void overrideBlockInteraction(CallbackInfoReturnable<Boolean> cir) {
        if (!BattleClientState.isBattle()) {
            return;
        }
        cir.setReturnValue(false);
    }
}
