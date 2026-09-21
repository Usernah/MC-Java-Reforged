package net.jr.mixin.minigame;

import net.jr.experimental_features.minigame.client.BattleClientState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class HudMixin {

    @Inject(method = {"extractContextualInfoBar","extractContextualInfoBarBackground","extractExperienceLevel"}, at = @At("HEAD"), cancellable = true)
    private void disabledContextualbar(GuiGraphicsExtractor grapchis, DeltaTracker deltaTracker,CallbackInfo ci) {
        if (BattleClientState.isBattle()) {
            ci.cancel();
        }
    }

}
