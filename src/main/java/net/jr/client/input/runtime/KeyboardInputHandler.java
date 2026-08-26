package net.jr.client.input.runtime;

import net.jr.client.input.InputApi;
import net.jr.client.input.simulation.InputSimulation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

public final class KeyboardInputHandler {
    private KeyboardInputHandler() {
    }

    public static boolean handle(long windowPointer, int action, KeyEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (windowPointer != minecraft.getWindow().handle()) {
            return false;
        }

        int key = event.key();
        int scanCode = event.scancode();
        if (InputSimulation.handleKey(windowPointer, key, scanCode, action, event.modifiers())) {
            if (InputSimulation.isActive()) {
                InputApi.markGamepadInput();
            }
            return true;
        }

        if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
            InputApi.markKeyboardMouseInput();
        }
        if (MappedActionProcessor.suppressConsumedScreenKey(key, scanCode, action)) {
            return true;
        }
        if (action != GLFW.GLFW_PRESS && action != GLFW.GLFW_REPEAT) {
            return false;
        }

        Screen screen = minecraft.gui.screen();
        if (screen == null || minecraft.gui.overlay() != null) {
            return false;
        }
        if (!MappedActionProcessor.handleScreenKey(screen, key, scanCode, action)) {
            return false;
        }

        MappedActionProcessor.rememberSuppressedScreenKey(key, scanCode);
        screen.afterKeyboardAction();
        return true;
    }
}
