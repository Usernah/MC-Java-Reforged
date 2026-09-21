package net.jr.client.input.sdl;

import net.jr.client.input.gamepad.GamepadCalibrationRegistry;
import net.jr.api.client.input.GamepadDigitalInput;
import net.jr.api.client.input.GamepadIdentity;
import net.jr.client.input.gamepad.RawGamepadInput;
import net.minecraft.client.Minecraft;

final class SdlGamepadInputMapper {
    private static final int TOUCHPAD_SIDE_NONE = 0;
    private static final int TOUCHPAD_SIDE_LEFT = 1;
    private static final int TOUCHPAD_SIDE_RIGHT = 2;

    private SdlGamepadInputMapper() {
    }

    static boolean isPressed(
        SdlGamepadSnapshot snapshot,
        GamepadDigitalInput input,
        float triggerThreshold,
        boolean splitTouchpad
    ) {
        if (splitTouchpad && input == GamepadDigitalInput.TOUCHPAD_BUTTON) {
            return snapshot.button(SdlGamepad.BUTTON_TOUCHPAD)
                && snapshot.touchpadPressSide() == TOUCHPAD_SIDE_NONE;
        }
        if (!splitTouchpad
            && (input == GamepadDigitalInput.TOUCHPAD_LEFT_BUTTON
                || input == GamepadDigitalInput.TOUCHPAD_RIGHT_BUTTON)) {
            return false;
        }

        RawGamepadInput calibratedInput = calibratedInput(snapshot.identity(), input);
        if (calibratedInput != null) return snapshot.rawInput(calibratedInput);

        return switch (input) {
            case BUTTON_DOWN -> snapshot.button(SdlGamepad.BUTTON_A);
            case BUTTON_RIGHT -> snapshot.button(SdlGamepad.BUTTON_B);
            case BUTTON_LEFT -> snapshot.button(SdlGamepad.BUTTON_X);
            case BUTTON_UP -> snapshot.button(SdlGamepad.BUTTON_Y);
            case BUTTON_START -> snapshot.button(SdlGamepad.BUTTON_START);
            case BUTTON_SELECT -> snapshot.button(SdlGamepad.BUTTON_BACK);
            case BUTTON_GUIDE -> snapshot.button(SdlGamepad.BUTTON_GUIDE);
            case BUTTON_SHARE, MISC_1 -> snapshot.button(SdlGamepad.BUTTON_MISC1);
            case MISC_2 -> snapshot.button(SdlGamepad.BUTTON_MISC2);
            case MISC_3 -> snapshot.button(SdlGamepad.BUTTON_MISC3);
            case MISC_4 -> snapshot.button(SdlGamepad.BUTTON_MISC4);
            case MISC_5 -> snapshot.button(SdlGamepad.BUTTON_MISC5);
            case MISC_6 -> snapshot.button(SdlGamepad.BUTTON_MISC6);
            case TOUCHPAD_BUTTON -> snapshot.button(SdlGamepad.BUTTON_TOUCHPAD);
            case TOUCHPAD_LEFT_BUTTON -> snapshot.touchpadPressSide() == TOUCHPAD_SIDE_LEFT;
            case TOUCHPAD_RIGHT_BUTTON -> snapshot.touchpadPressSide() == TOUCHPAD_SIDE_RIGHT;
            case STICK_LEFT_BUTTON -> snapshot.button(SdlGamepad.BUTTON_LEFTSTICK);
            case STICK_RIGHT_BUTTON -> snapshot.button(SdlGamepad.BUTTON_RIGHTSTICK);
            case BUMPER_LEFT -> snapshot.button(SdlGamepad.BUTTON_LEFTSHOULDER);
            case BUMPER_RIGHT -> snapshot.button(SdlGamepad.BUTTON_RIGHTSHOULDER);
            case PADDLE_RIGHT_1 -> snapshot.button(SdlGamepad.BUTTON_RIGHT_PADDLE1);
            case PADDLE_LEFT_1 -> snapshot.button(SdlGamepad.BUTTON_LEFT_PADDLE1);
            case PADDLE_RIGHT_2 -> snapshot.button(SdlGamepad.BUTTON_RIGHT_PADDLE2);
            case PADDLE_LEFT_2 -> snapshot.button(SdlGamepad.BUTTON_LEFT_PADDLE2);
            case DPAD_UP -> snapshot.button(SdlGamepad.BUTTON_DPAD_UP);
            case DPAD_DOWN -> snapshot.button(SdlGamepad.BUTTON_DPAD_DOWN);
            case DPAD_LEFT -> snapshot.button(SdlGamepad.BUTTON_DPAD_LEFT);
            case DPAD_RIGHT -> snapshot.button(SdlGamepad.BUTTON_DPAD_RIGHT);
            case TRIGGER_LEFT -> axis(snapshot, SdlGamepad.AXIS_TRIGGERLEFT, triggerThreshold) > 0.5F;
            case TRIGGER_RIGHT -> axis(snapshot, SdlGamepad.AXIS_TRIGGERRIGHT, triggerThreshold) > 0.5F;
            case STICK_LEFT_MOVE_UP -> direction(snapshot, SdlGamepad.AXIS_LEFTY, triggerThreshold, -1);
            case STICK_LEFT_MOVE_DOWN -> direction(snapshot, SdlGamepad.AXIS_LEFTY, triggerThreshold, 1);
            case STICK_LEFT_MOVE_LEFT -> direction(snapshot, SdlGamepad.AXIS_LEFTX, triggerThreshold, -1);
            case STICK_LEFT_MOVE_RIGHT -> direction(snapshot, SdlGamepad.AXIS_LEFTX, triggerThreshold, 1);
            case STICK_RIGHT_MOVE_UP -> direction(snapshot, SdlGamepad.AXIS_RIGHTY, triggerThreshold, -1);
            case STICK_RIGHT_MOVE_DOWN -> direction(snapshot, SdlGamepad.AXIS_RIGHTY, triggerThreshold, 1);
            case STICK_RIGHT_MOVE_LEFT -> direction(snapshot, SdlGamepad.AXIS_RIGHTX, triggerThreshold, -1);
            case STICK_RIGHT_MOVE_RIGHT -> direction(snapshot, SdlGamepad.AXIS_RIGHTX, triggerThreshold, 1);
        };
    }

    private static float axis(SdlGamepadSnapshot snapshot, int axis, float deadzone) {
        float value = snapshot.axis(axis);
        return Math.abs(value) < deadzone ? 0.0F : value;
    }

    private static boolean direction(SdlGamepadSnapshot snapshot, int axis, float deadzone, int direction) {
        float value = axis(snapshot, axis, deadzone);
        return direction < 0 ? value < -0.5F : value > 0.5F;
    }

    private static RawGamepadInput calibratedInput(GamepadIdentity identity, GamepadDigitalInput input) {
        Minecraft minecraft = Minecraft.getInstance();
        if (identity == null || minecraft == null) return null;
        GamepadCalibrationRegistry registry = GamepadCalibrationRegistry.get();
        registry.ensureLoaded(minecraft);
        return registry.getInput(identity, input);
    }
}
