package net.jr.client.ui.hint.glyph;

import javax.annotation.Nullable;
import net.jr.client.input.gamepad.GamepadDigitalInput;

public enum ControllerGlyph {
    BUTTON_DOWN('\uE000', "button_down"),
    BUTTON_RIGHT('\uE001', "button_right"),
    BUTTON_LEFT('\uE002', "button_left"),
    BUTTON_UP('\uE003', "button_up"),
    BUMPER_LEFT('\uE004', "bumper_left"),
    BUMPER_RIGHT('\uE005', "bumper_right"),
    TRIGGER_LEFT('\uE006', "trigger_left"),
    TRIGGER_RIGHT('\uE007', "trigger_right"),
    BUTTON_SELECT('\uE008', "button_select"),
    BUTTON_START('\uE009', "button_start"),
    BUTTON_GUIDE('\uE026', "button_guide"),
    DPAD('\uE012', "dpad"),
    DPAD_UP('\uE00A', "dpad_up"),
    DPAD_DOWN('\uE00B', "dpad_down"),
    DPAD_LEFT('\uE00C', "dpad_left"),
    DPAD_RIGHT('\uE00D', "dpad_right"),
    STICK_LEFT('\uE00E', "stick_left"),
    STICK_RIGHT('\uE00F', "stick_right"),
    STICK_LEFT_BUTTON('\uE010', "stick_left_button"),
    STICK_RIGHT_BUTTON('\uE011', "stick_right_button"),
    STICK_LEFT_MOVE_UP('\uE013', "stick_left_move_up"),
    STICK_LEFT_MOVE_DOWN('\uE014', "stick_left_move_down"),
    STICK_LEFT_MOVE_LEFT('\uE015', "stick_left_move_left"),
    STICK_LEFT_MOVE_RIGHT('\uE016', "stick_left_move_right"),
    STICK_RIGHT_MOVE_UP('\uE017', "stick_right_move_up"),
    STICK_RIGHT_MOVE_DOWN('\uE018', "stick_right_move_down"),
    STICK_RIGHT_MOVE_LEFT('\uE019', "stick_right_move_left"),
    STICK_RIGHT_MOVE_RIGHT('\uE01A', "stick_right_move_right"),
    TOUCHPAD('\uE01B', "touchpad"),
    TOUCHPAD_BUTTON('\uE029', "touchpad_button"),
    TOUCHPAD_LEFT_BUTTON('\uE027', "touchpad_left_button"),
    TOUCHPAD_RIGHT_BUTTON('\uE028', "touchpad_right_button"),
    MISC_1('\uE01C', "misc_1"),
    MISC_2('\uE01D', "misc_2"),
    MISC_3('\uE01E', "misc_3"),
    MISC_4('\uE01F', "misc_4"),
    MISC_5('\uE020', "misc_5"),
    MISC_6('\uE021', "misc_6"),
    PADDLE_RIGHT_1('\uE022', "paddle_right_1"),
    PADDLE_LEFT_1('\uE023', "paddle_left_1"),
    PADDLE_RIGHT_2('\uE024', "paddle_right_2"),
    PADDLE_LEFT_2('\uE025', "paddle_left_2");

    private final char character;
    private final String fileName;

    ControllerGlyph(char character, String fileName) {
        this.character = character;
        this.fileName = fileName;
    }

    public char character() {
        return character;
    }

    public String text() {
        return Character.toString(character);
    }

    public String fileName() {
        return fileName;
    }

    @Nullable
    public static ControllerGlyph forInput(GamepadDigitalInput input) {
        return switch (input) {
            case BUTTON_DOWN -> BUTTON_DOWN;
            case BUTTON_RIGHT -> BUTTON_RIGHT;
            case BUTTON_LEFT -> BUTTON_LEFT;
            case BUTTON_UP -> BUTTON_UP;
            case BUTTON_START -> BUTTON_START;
            case BUTTON_SELECT -> BUTTON_SELECT;
            case BUTTON_GUIDE -> BUTTON_GUIDE;
            case BUTTON_SHARE, MISC_1 -> MISC_1;
            case MISC_2 -> MISC_2;
            case MISC_3 -> MISC_3;
            case MISC_4 -> MISC_4;
            case MISC_5 -> MISC_5;
            case MISC_6 -> MISC_6;
            case TOUCHPAD_BUTTON -> TOUCHPAD_BUTTON;
            case TOUCHPAD_LEFT_BUTTON -> TOUCHPAD_LEFT_BUTTON;
            case TOUCHPAD_RIGHT_BUTTON -> TOUCHPAD_RIGHT_BUTTON;
            case STICK_LEFT_BUTTON -> STICK_LEFT_BUTTON;
            case STICK_RIGHT_BUTTON -> STICK_RIGHT_BUTTON;
            case BUMPER_LEFT -> BUMPER_LEFT;
            case BUMPER_RIGHT -> BUMPER_RIGHT;
            case PADDLE_RIGHT_1 -> PADDLE_RIGHT_1;
            case PADDLE_LEFT_1 -> PADDLE_LEFT_1;
            case PADDLE_RIGHT_2 -> PADDLE_RIGHT_2;
            case PADDLE_LEFT_2 -> PADDLE_LEFT_2;
            case DPAD_UP -> DPAD_UP;
            case DPAD_DOWN -> DPAD_DOWN;
            case DPAD_LEFT -> DPAD_LEFT;
            case DPAD_RIGHT -> DPAD_RIGHT;
            case TRIGGER_LEFT -> TRIGGER_LEFT;
            case TRIGGER_RIGHT -> TRIGGER_RIGHT;
            case STICK_LEFT_MOVE_UP -> STICK_LEFT_MOVE_UP;
            case STICK_LEFT_MOVE_DOWN -> STICK_LEFT_MOVE_DOWN;
            case STICK_LEFT_MOVE_LEFT -> STICK_LEFT_MOVE_LEFT;
            case STICK_LEFT_MOVE_RIGHT -> STICK_LEFT_MOVE_RIGHT;
            case STICK_RIGHT_MOVE_UP -> STICK_RIGHT_MOVE_UP;
            case STICK_RIGHT_MOVE_DOWN -> STICK_RIGHT_MOVE_DOWN;
            case STICK_RIGHT_MOVE_LEFT -> STICK_RIGHT_MOVE_LEFT;
            case STICK_RIGHT_MOVE_RIGHT -> STICK_RIGHT_MOVE_RIGHT;
        };
    }
}
