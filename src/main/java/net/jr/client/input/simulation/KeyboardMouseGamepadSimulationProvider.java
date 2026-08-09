package net.jr.client.input.simulation;

import java.util.EnumMap;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.jr.Java_reforged;
import net.jr.client.input.gamepad.GamepadAxis;
import net.jr.client.input.gamepad.GamepadDigitalInput;
import net.jr.client.input.gamepad.RawGamepadInputPress;
import org.lwjgl.glfw.GLFW;

public final class KeyboardMouseGamepadSimulationProvider implements InputSimulationProvider {
    private static final float DIGITAL_AXIS_VALUE = 1.0F;

    private final KeyboardMouseVirtualMapping mapping = new KeyboardMouseVirtualMapping();
    private final EnumSet<GamepadDigitalInput> pressedInputs = EnumSet.noneOf(GamepadDigitalInput.class);
    private final EnumMap<GamepadAxis, Float> axes = new EnumMap<>(GamepadAxis.class);
    private boolean active;
    private VirtualGamepadProfile profile = VirtualGamepadProfile.STEAM_DECK;

    public KeyboardMouseGamepadSimulationProvider() {
        this.clearAxes();
    }

    public void setProfile(VirtualGamepadProfile profile) {
        this.profile = profile == null ? VirtualGamepadProfile.XBOX : profile;
        this.releaseInputs();
    }

    public VirtualGamepadProfile profile() {
        return this.profile;
    }

    @Override
    public boolean isActive() {
        return this.active;
    }

    @Override
    public boolean isConnected() {
        return this.active;
    }

    @Override
    public void update() {
        this.rebuildAxes();
    }

    @Override
    public boolean isPressed(GamepadDigitalInput input) {
        return this.active && this.profile.supports(input) && this.pressedInputs.contains(input);
    }

    @Override
    public float axis(GamepadAxis axis, float deadzone) {
        float value = this.rawAxis(axis);
        return Math.abs(value) < deadzone ? 0.0F : value;
    }

    @Override
    public float rawAxis(GamepadAxis axis) {
        return this.active ? this.axes.getOrDefault(axis, 0.0F) : 0.0F;
    }

    @Override
    public boolean handleKey(long windowPointer, int key, int scanCode, int action, int modifiers) {
        if (key == KeyboardMouseVirtualMapping.TOGGLE_KEY) {
            if (action == GLFW.GLFW_PRESS) {
                this.active = !this.active;
                this.releaseInputs();
                Java_reforged.LOGGER.info("Simulacion de gamepad {} con perfil {}.", this.active ? "activada" : "desactivada", this.profile);
            }
            return true;
        }

        if (!this.active) {
            return false;
        }

        GamepadDigitalInput input = this.mapping.key(key);
        if (input != null) {
            this.setInput(input, action != GLFW.GLFW_RELEASE);
        }
        return true;
    }

    @Override
    public boolean handleMouseMove(long windowPointer, double x, double y) {
        return this.active;
    }

    @Override
    public boolean handleMouseButton(long windowPointer, int button, int action, int modifiers) {
        if (!this.active) {
            return false;
        }

        GamepadDigitalInput input = this.mapping.mouseButton(button);
        if (input != null) {
            this.setInput(input, action != GLFW.GLFW_RELEASE);
        }
        return true;
    }

    @Override
    public boolean handleMouseScroll(long windowPointer, double xOffset, double yOffset) {
        return this.active;
    }

    @Override
    public void clearCalibrationPresses() {
    }

    @Nullable
    @Override
    public RawGamepadInputPress pollCalibrationPress() {
        return null;
    }

    @Override
    public boolean hasActiveCalibrationInput() {
        return this.active && !this.pressedInputs.isEmpty();
    }

    @Override
    public String debugStatus() {
        return "Input simulation: active=" + this.active + ", profile=" + this.profile + ", pressed=" + this.pressedInputs;
    }

    private void setInput(GamepadDigitalInput input, boolean down) {
        if (!this.profile.supports(input)) {
            return;
        }

        if (down) {
            this.pressedInputs.add(input);
        } else {
            this.pressedInputs.remove(input);
        }
        this.rebuildAxes();
    }

    private void releaseInputs() {
        this.pressedInputs.clear();
        this.clearAxes();
    }

    private void rebuildAxes() {
        this.clearAxes();
        this.axes.put(GamepadAxis.LEFT_STICK_X, axis(GamepadDigitalInput.STICK_LEFT_MOVE_LEFT, GamepadDigitalInput.STICK_LEFT_MOVE_RIGHT));
        this.axes.put(GamepadAxis.LEFT_STICK_Y, axis(GamepadDigitalInput.STICK_LEFT_MOVE_UP, GamepadDigitalInput.STICK_LEFT_MOVE_DOWN));
        this.axes.put(GamepadAxis.RIGHT_STICK_X, axis(GamepadDigitalInput.STICK_RIGHT_MOVE_LEFT, GamepadDigitalInput.STICK_RIGHT_MOVE_RIGHT));
        this.axes.put(GamepadAxis.RIGHT_STICK_Y, axis(GamepadDigitalInput.STICK_RIGHT_MOVE_UP, GamepadDigitalInput.STICK_RIGHT_MOVE_DOWN));
        this.axes.put(GamepadAxis.TRIGGER_LEFT, this.isPressed(GamepadDigitalInput.TRIGGER_LEFT) ? DIGITAL_AXIS_VALUE : 0.0F);
        this.axes.put(GamepadAxis.TRIGGER_RIGHT, this.isPressed(GamepadDigitalInput.TRIGGER_RIGHT) ? DIGITAL_AXIS_VALUE : 0.0F);
    }

    private float axis(GamepadDigitalInput negative, GamepadDigitalInput positive) {
        float value = 0.0F;
        if (this.isPressed(negative)) {
            value -= DIGITAL_AXIS_VALUE;
        }
        if (this.isPressed(positive)) {
            value += DIGITAL_AXIS_VALUE;
        }
        return value;
    }

    private void clearAxes() {
        for (GamepadAxis axis : GamepadAxis.values()) {
            this.axes.put(axis, 0.0F);
        }
    }
}

