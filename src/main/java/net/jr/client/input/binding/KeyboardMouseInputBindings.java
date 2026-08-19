package net.jr.client.input.binding;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

public final class KeyboardMouseInputBindings {
    public static final int MOUSE_SCROLL_UP = 100;
    public static final int MOUSE_SCROLL_DOWN = 101;

    private KeyboardMouseInputBindings() {
    }

    public static InputConstants.Key mouseButton(int button) {
        return InputConstants.Type.MOUSE.getOrCreate(button);
    }

    public static InputConstants.Key mouseScrollUp() {
        return InputConstants.Type.MOUSE.getOrCreate(MOUSE_SCROLL_UP);
    }

    public static InputConstants.Key mouseScrollDown() {
        return InputConstants.Type.MOUSE.getOrCreate(MOUSE_SCROLL_DOWN);
    }

    public static InputConstants.Key mouseScroll(double scrollY) {
        return scrollY > 0.0D ? mouseScrollUp() : mouseScrollDown();
    }

    public static boolean matchesMouseButton(KeyMapping mapping, int button) {
        return !mapping.isUnbound()
            && mapping.getKey().equals(mouseButton(button))
            && modifierMatches(mapping.getKeyModifier());
    }

    public static boolean matchesMouseScroll(KeyMapping mapping, double scrollY) {
        if (scrollY == 0.0D || mapping.isUnbound()) {
            return false;
        }

        return mapping.getKey().equals(mouseScroll(scrollY))
            && modifierMatches(mapping.getKeyModifier());
    }

    private static boolean modifierMatches(KeyModifier modifier) {
        return switch (modifier) {
            case NONE -> true;
            case SHIFT -> isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT);
            case CONTROL -> isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) || isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL);
            case CONTROL_OR_COMMAND -> modifier.isActive(
                net.neoforged.neoforge.client.settings.KeyConflictContext.GUI
            );
            case ALT -> isKeyDown(GLFW.GLFW_KEY_LEFT_ALT) || isKeyDown(GLFW.GLFW_KEY_RIGHT_ALT);
        };
    }

    private static boolean isKeyDown(int key) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return false;
        }
        return GLFW.glfwGetKey(minecraft.getWindow().handle(), key) == GLFW.GLFW_PRESS;
    }
}

