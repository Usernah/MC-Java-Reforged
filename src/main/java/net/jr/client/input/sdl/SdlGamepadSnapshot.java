package net.jr.client.input.sdl;

import dev.isxander.sdl3java.api.gamepad.SDL_GamepadButton;
import java.util.Arrays;
import net.jr.client.input.gamepad.GamepadIdentity;
import net.jr.client.input.gamepad.RawGamepadInput;

record SdlGamepadSnapshot(
    boolean connected,
    float[] axes,
    boolean[] buttons,
    boolean[] rawButtons,
    int[] rawHats,
    int touchpadPressSide,
    GamepadIdentity identity,
    long readNanos
) {
    private static final int AXIS_COUNT = 6;
    private static final float RAW_AXIS_DIGITAL_THRESHOLD = 0.55F;

    static SdlGamepadSnapshot disconnected() {
        return new SdlGamepadSnapshot(
            false,
            new float[AXIS_COUNT],
            new boolean[SDL_GamepadButton.SDL_GAMEPAD_BUTTON_COUNT],
            new boolean[0],
            new int[0],
            0,
            null,
            0L
        );
    }

    float axis(int axis) {
        return axis >= 0 && axis < this.axes.length ? this.axes[axis] : 0.0F;
    }

    boolean button(int button) {
        return button >= 0 && button < this.buttons.length && this.buttons[button];
    }

    boolean rawInput(RawGamepadInput input) {
        return switch (input.type()) {
            case "controller_button" -> this.button(input.index());
            case "button" -> input.index() >= 0
                && input.index() < this.rawButtons.length
                && this.rawButtons[input.index()];
            case "hat" -> input.index() >= 0
                && input.index() < this.rawHats.length
                && (this.rawHats[input.index()] & input.value()) == input.value();
            case "axis" -> axisDirection(input.index()) == input.value();
            default -> false;
        };
    }

    boolean anyCalibrationInputActive() {
        for (boolean button : this.buttons) {
            if (button) return true;
        }
        for (boolean button : this.rawButtons) {
            if (button) return true;
        }
        for (int hat : this.rawHats) {
            if (hat != 0) return true;
        }
        for (int axis = 0; axis < this.axes.length; axis++) {
            if (axisDirection(axis) != 0) return true;
        }
        return false;
    }

    private int axisDirection(int axis) {
        float value = axis(axis);
        if (value >= RAW_AXIS_DIGITAL_THRESHOLD) return 1;
        if (value <= -RAW_AXIS_DIGITAL_THRESHOLD) return -1;
        return 0;
    }

    @Override
    public String toString() {
        return "SdlGamepadSnapshot{"
            + "connected=" + this.connected
            + ", axes=" + Arrays.toString(this.axes)
            + ", buttons=" + Arrays.toString(this.buttons)
            + ", rawButtons=" + Arrays.toString(this.rawButtons)
            + ", rawHats=" + Arrays.toString(this.rawHats)
            + ", touchpadPressSide=" + this.touchpadPressSide
            + ", identity=" + this.identity
            + ", readNanos=" + this.readNanos
            + '}';
    }
}
