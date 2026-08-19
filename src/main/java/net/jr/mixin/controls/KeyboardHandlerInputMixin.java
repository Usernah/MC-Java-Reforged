package net.jr.mixin.controls;

import net.jr.client.input.InputApi;
import net.jr.client.input.runtime.MappedActionProcessor;
import net.jr.client.input.simulation.InputSimulation;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Routes real and simulated keyboard events through JR before vanilla handles
 * the active screen. This is required for slot focus and mapped container
 * actions; the tick-side controller path uses the same processor.
 */
@Mixin(KeyboardHandler.class)
public final class KeyboardHandlerInputMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void javaReforged$handleScreenMappings(
        long windowPointer, int action, KeyEvent event, CallbackInfo callback
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (windowPointer != minecraft.getWindow().handle()) return;
        int key = event.key();
        int scanCode = event.scancode();

        if (InputSimulation.handleKey(windowPointer, key, scanCode, action, event.modifiers())) {
            if (InputSimulation.isActive()) InputApi.markGamepadInput();
            callback.cancel();
            return;
        }

        if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
            InputApi.markKeyboardMouseInput();
        }

        if (MappedActionProcessor.suppressConsumedScreenKey(key, scanCode, action)) {
            callback.cancel();
            return;
        }

        if (action != GLFW.GLFW_PRESS && action != GLFW.GLFW_REPEAT) return;
        Screen screen = minecraft.gui.screen();
        if (screen == null || minecraft.gui.overlay() != null) return;

        if (MappedActionProcessor.handleScreenKey(screen, key, scanCode, action)) {
            MappedActionProcessor.rememberSuppressedScreenKey(key, scanCode);
            screen.afterKeyboardAction();
            callback.cancel();
        }
    }
}
