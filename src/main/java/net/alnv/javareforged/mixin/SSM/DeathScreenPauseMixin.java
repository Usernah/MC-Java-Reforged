package net.alnv.javareforged.mixin.SSM;

import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class DeathScreenPauseMixin {
    @Inject(method = "isPauseScreen", at = @At("HEAD"), cancellable = true)
    private void splitTest$deathScreenNeverPausesWorld(CallbackInfoReturnable<Boolean> cir) {
        if ((Object)this instanceof DeathScreen) {
            cir.setReturnValue(false);
        }
    }
}
