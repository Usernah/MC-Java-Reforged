package net.alnv.javareforged.mixin.SSM;

import net.alnv.javareforged.ClientRuntime.runtime.ClientDisconnects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenDisconnectMixin {
    @Inject(method = "onDisconnect", at = @At("HEAD"), cancellable = true)
    private void splitTest$disconnectOnlyCurrentLocalClient(CallbackInfo ci) {
        Minecraft minecraft = ((Screen)(Object)this).getMinecraft();
        if (minecraft != null && ClientDisconnects.disconnectCurrentFromPauseMenu(minecraft)) {
            ci.cancel();
        }
    }
}
