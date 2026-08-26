package net.jr.mixin.runtime;

import net.jr.client.runtime.session.LocalClientDisconnectHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin {
    @Redirect(
        method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;disconnectFromWorld(Lnet/minecraft/network/chat/Component;)V"
        )
    )
    private void javaReforged$disconnectOnlyCurrentLocalClient(Minecraft minecraft, Component message) {
        if (!LocalClientDisconnectHandler.disconnectCurrentFromPauseMenu(minecraft)) {
            minecraft.disconnectFromWorld(message);
        }
    }
}
