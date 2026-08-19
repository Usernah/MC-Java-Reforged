package net.jr.client.input.simulation;

import java.util.HashMap;
import java.util.Map;
import net.jr.client.input.gamepad.GamepadDigitalInput;
import org.lwjgl.glfw.GLFW;

final class KeyboardMouseVirtualMapping {
    static final int TOGGLE_KEY = GLFW.GLFW_KEY_F9;

    private final Map<Integer, GamepadDigitalInput> keys = new HashMap<>();
    private final Map<Integer, GamepadDigitalInput> mouseButtons = new HashMap<>();

    KeyboardMouseVirtualMapping() {
        this.keys.put(GLFW.GLFW_KEY_SPACE, GamepadDigitalInput.BUTTON_DOWN);
        this.keys.put(GLFW.GLFW_KEY_Q, GamepadDigitalInput.BUTTON_RIGHT);
        this.keys.put(GLFW.GLFW_KEY_C, GamepadDigitalInput.BUTTON_LEFT);
        this.keys.put(GLFW.GLFW_KEY_E, GamepadDigitalInput.BUTTON_UP);

        this.keys.put(GLFW.GLFW_KEY_ENTER, GamepadDigitalInput.BUTTON_START);
        this.keys.put(GLFW.GLFW_KEY_BACKSPACE, GamepadDigitalInput.BUTTON_SELECT);
        this.keys.put(GLFW.GLFW_KEY_G, GamepadDigitalInput.BUTTON_GUIDE);
        this.keys.put(GLFW.GLFW_KEY_M, GamepadDigitalInput.MISC_1);
        this.keys.put(GLFW.GLFW_KEY_TAB, GamepadDigitalInput.TOUCHPAD_BUTTON);

        this.keys.put(GLFW.GLFW_KEY_1, GamepadDigitalInput.BUMPER_LEFT);
        this.keys.put(GLFW.GLFW_KEY_2, GamepadDigitalInput.BUMPER_RIGHT);
        this.keys.put(GLFW.GLFW_MOUSE_BUTTON_RIGHT, GamepadDigitalInput.TRIGGER_LEFT);
        this.keys.put(GLFW.GLFW_MOUSE_BUTTON_LEFT, GamepadDigitalInput.TRIGGER_RIGHT);

        this.keys.put(GLFW.GLFW_KEY_W, GamepadDigitalInput.STICK_LEFT_MOVE_UP);
        this.keys.put(GLFW.GLFW_KEY_S, GamepadDigitalInput.STICK_LEFT_MOVE_DOWN);
        this.keys.put(GLFW.GLFW_KEY_A, GamepadDigitalInput.STICK_LEFT_MOVE_LEFT);
        this.keys.put(GLFW.GLFW_KEY_D, GamepadDigitalInput.STICK_LEFT_MOVE_RIGHT);

        this.keys.put(GLFW.GLFW_KEY_UP, GamepadDigitalInput.STICK_RIGHT_MOVE_UP);
        this.keys.put(GLFW.GLFW_KEY_DOWN, GamepadDigitalInput.STICK_RIGHT_MOVE_DOWN);
        this.keys.put(GLFW.GLFW_KEY_LEFT, GamepadDigitalInput.STICK_RIGHT_MOVE_LEFT);
        this.keys.put(GLFW.GLFW_KEY_RIGHT, GamepadDigitalInput.STICK_RIGHT_MOVE_RIGHT);

        this.keys.put(GLFW.GLFW_KEY_KP_8, GamepadDigitalInput.DPAD_UP);
        this.keys.put(GLFW.GLFW_KEY_KP_2, GamepadDigitalInput.DPAD_DOWN);
        this.keys.put(GLFW.GLFW_KEY_KP_4, GamepadDigitalInput.DPAD_LEFT);
        this.keys.put(GLFW.GLFW_KEY_KP_6, GamepadDigitalInput.DPAD_RIGHT);

        this.keys.put(GLFW.GLFW_KEY_F1, GamepadDigitalInput.PADDLE_LEFT_1);
        this.keys.put(GLFW.GLFW_KEY_F2, GamepadDigitalInput.PADDLE_RIGHT_1);
        this.keys.put(GLFW.GLFW_KEY_F3, GamepadDigitalInput.PADDLE_LEFT_2);
        this.keys.put(GLFW.GLFW_KEY_F4, GamepadDigitalInput.PADDLE_RIGHT_2);
        this.keys.put(GLFW.GLFW_KEY_5, GamepadDigitalInput.MISC_2);
        this.keys.put(GLFW.GLFW_KEY_6, GamepadDigitalInput.MISC_3);
        this.keys.put(GLFW.GLFW_KEY_7, GamepadDigitalInput.MISC_4);
        this.keys.put(GLFW.GLFW_KEY_8, GamepadDigitalInput.MISC_5);
        this.keys.put(GLFW.GLFW_KEY_9, GamepadDigitalInput.MISC_6);

        this.mouseButtons.put(GLFW.GLFW_MOUSE_BUTTON_LEFT, GamepadDigitalInput.TRIGGER_RIGHT);
        this.mouseButtons.put(GLFW.GLFW_MOUSE_BUTTON_RIGHT, GamepadDigitalInput.TRIGGER_LEFT);
        this.mouseButtons.put(GLFW.GLFW_MOUSE_BUTTON_MIDDLE, GamepadDigitalInput.STICK_RIGHT_BUTTON);
        this.mouseButtons.put(GLFW.GLFW_MOUSE_BUTTON_4, GamepadDigitalInput.BUMPER_LEFT);
        this.mouseButtons.put(GLFW.GLFW_MOUSE_BUTTON_5, GamepadDigitalInput.BUMPER_RIGHT);
    }

    GamepadDigitalInput key(int key) {
        return this.keys.get(key);
    }

    GamepadDigitalInput mouseButton(int button) {
        return this.mouseButtons.get(button);
    }
}

