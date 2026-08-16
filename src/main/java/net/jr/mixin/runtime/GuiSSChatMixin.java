package net.jr.mixin.runtime;

import net.jr.client.runtime.ui.LocalChatRouting;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.chat.ChatListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Gui.class)
public abstract class GuiSSChatMixin {
    @Shadow
    @Final
    private ChatListener chatListener;

    @Redirect(
        method = {
            "tick()V",
            "chatListener()Lnet/minecraft/client/multiplayer/chat/ChatListener;",
            "addSocialInteractionsToast()V"
        },
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Gui;chatListener:Lnet/minecraft/client/multiplayer/chat/ChatListener;")
    )
    private ChatListener javaReforged$useCurrentClientChatListener(Gui gui) {
        return LocalChatRouting.listener(this.chatListener);
    }
}
