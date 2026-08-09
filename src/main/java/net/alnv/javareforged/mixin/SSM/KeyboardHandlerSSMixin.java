package net.alnv.javareforged.mixin.SSM;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.alnv.javareforged.ClientRuntime.input.ScreenInput;
import net.alnv.javareforged.ClientRuntime.runtime.ClientBoundary;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;

@Mixin(value = KeyboardHandler.class, priority = 2000)
public abstract class KeyboardHandlerSSMixin {
    @WrapMethod(method = "keyPress(JIIII)V")
    private void splitTest$keyPressForPrimaryClient(long windowPointer, int key, int scanCode, int action, int modifiers, Operation<Void> original) {
        ClientBoundary.runPrimary(Minecraft.getInstance(), () -> original.call(windowPointer, key, scanCode, action, modifiers));
    }

    @WrapMethod(method = "charTyped(JII)V")
    private void splitTest$charTypedForPrimaryClient(long windowPointer, int codePoint, int modifiers, Operation<Void> original) {
        ClientBoundary.runPrimary(Minecraft.getInstance(), () -> original.call(windowPointer, codePoint, modifiers));
    }

    @Redirect(
            method = {"keyPress", "charTyped"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;")
    )
    @Nullable
    private Screen splitTest$readTargetScreen(Minecraft minecraft) {
        return ScreenInput.screen();
    }

    @Redirect(
            method = {"keyPress", "charTyped"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"
            )
    )
    private void splitTest$runScreenEventForTargetSlot(Runnable event, String errorTitle, String screenName) {
        ScreenInput.runEvent(event, errorTitle, screenName);
    }
}
