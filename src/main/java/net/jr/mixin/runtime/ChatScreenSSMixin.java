package net.jr.mixin.runtime;

import net.jr.client.runtime.ui.LocalChatRouter;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChatScreen.class)
public abstract class ChatScreenSSMixin {
    @Redirect(
        method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getBackgroundColor(I)I")
    )
    private int javaReforged$drawConfiguredInputBackground(Options options, int color) {
        return LocalChatRouter.backgroundColor(options.getBackgroundColor(color));
    }
}
