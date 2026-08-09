package net.jr.client.input.gamepad;

import java.util.Locale;
import javax.annotation.Nullable;

public enum GamepadDigitalInput {
    BUTTON_DOWN,
    BUTTON_RIGHT,
    BUTTON_LEFT,
    BUTTON_UP,
    BUTTON_START,
    BUTTON_SELECT,
    BUTTON_GUIDE,
    BUTTON_SHARE,
    MISC_1,
    MISC_2,
    MISC_3,
    MISC_4,
    MISC_5,
    MISC_6,
    TOUCHPAD_BUTTON,
    TOUCHPAD_LEFT_BUTTON,
    TOUCHPAD_RIGHT_BUTTON,
    STICK_LEFT_BUTTON,
    STICK_RIGHT_BUTTON,
    BUMPER_LEFT,
    BUMPER_RIGHT,
    PADDLE_RIGHT_1,
    PADDLE_LEFT_1,
    PADDLE_RIGHT_2,
    PADDLE_LEFT_2,
    DPAD_UP,
    DPAD_DOWN,
    DPAD_LEFT,
    DPAD_RIGHT,
    TRIGGER_LEFT,
    TRIGGER_RIGHT,
    STICK_LEFT_MOVE_UP,
    STICK_LEFT_MOVE_DOWN,
    STICK_LEFT_MOVE_LEFT,
    STICK_LEFT_MOVE_RIGHT,
    STICK_RIGHT_MOVE_UP,
    STICK_RIGHT_MOVE_DOWN,
    STICK_RIGHT_MOVE_LEFT,
    STICK_RIGHT_MOVE_RIGHT;

    private final String serializedName;

    GamepadDigitalInput() {
        this.serializedName = name().toUpperCase(Locale.ROOT);
    }

    public String serializedName() {
        return serializedName;
    }

    @Nullable
    public static GamepadDigitalInput fromSerializedName(String name) {
        // Compatibility with bindings written before touchpad base/click glyphs
        // became separate concepts.
        if ("TOUCHPAD".equalsIgnoreCase(name)) {
            return TOUCHPAD_BUTTON;
        }
        if ("TOUCHPAD_LEFT".equalsIgnoreCase(name)) {
            return TOUCHPAD_LEFT_BUTTON;
        }
        if ("TOUCHPAD_RIGHT".equalsIgnoreCase(name)) {
            return TOUCHPAD_RIGHT_BUTTON;
        }
        for (GamepadDigitalInput input : values()) {
            if (input.serializedName.equalsIgnoreCase(name)) {
                return input;
            }
        }
        return null;
    }
}

