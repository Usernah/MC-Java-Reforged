package net.jr.mixin.runtime;

import net.jr.client.runtime.ui.LocalChatRouter;
import net.jr.experimental_features.gamepad_keyboard.GamepadKeyboardScreen;
import net.minecraft.client.Options;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.lwjgl.glfw.GLFW;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin implements GamepadKeyboardScreen {
    @Override
    public boolean gamepadKeyboardActivateFocusedInputOnPrimary() {
        return true;
    }

    @Override
    public boolean gamepadKeyboardSubmit() {
        return ((ChatScreen)(Object)this).keyPressed(new KeyEvent(GLFW.GLFW_KEY_ENTER, 0, 0));
    }

    @Redirect(
        method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getBackgroundColor(I)I")
    )
    private int javaReforged$drawConfiguredInputBackground(Options options, int color) {
        return LocalChatRouter.backgroundColor(options.getBackgroundColor(color));
    }
}
