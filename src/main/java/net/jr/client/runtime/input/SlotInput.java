package net.jr.client.runtime.input;

import java.util.List;
import net.jr.api.client.input.GamepadAxis;
import net.jr.api.client.input.GamepadDigitalInput;
import net.jr.api.client.input.IGamepad;
import net.jr.api.client.input.IKeyboardMouse;
import net.jr.api.client.input.ISlotInput;
import net.jr.api.client.input.IScreenInput;
import net.jr.api.client.input.InputMode;
import net.jr.api.client.systemui.SystemUI;
import net.jr.client.input.simulation.InputSimulation;
import net.jr.client.runtime.ClientRuntime;


public final class SlotInput implements ISlotInput {
    private final int slotId;
    private final IScreenInput screen;

    public SlotInput(int slotId) {
        this.slotId = slotId;
        this.screen = new SlotScreenInput(slotId);
    }

    @Override
    public int slotId() {
        return this.slotId;
    }

    @Override
    public InputMode mode() {
        return ClientRuntime.INSTANCE.slots().slot(this.slotId).inputState().mode();
    }

    @Override
    public List<IGamepad> gamepads() {
        return ClientRuntime.INSTANCE.input().assignments().gamepadsForSlot(this.slotId);
    }

    @Override
    public IKeyboardMouse keyboardMouse() {
        return this.slotId == InputAssignment.KEYBOARD_MOUSE_SLOT
            ? ClientRuntime.INSTANCE.input().devices().keyboardMouse() : null;
    }

    @Override
    public IScreenInput screen() {
        return this.screen;
    }

    @Override
    public boolean hasGamepad() {
        return ClientRuntime.INSTANCE.input().assignments().hasGamepadForSlot(this.slotId);
    }

    @Override
    public boolean hasDisconnectedGamepad() {
        return ClientRuntime.INSTANCE.input().assignments().hasDisconnectedGamepadForSlot(this.slotId);
    }

    @Override
    public boolean pressed(GamepadDigitalInput input) {
        if (InputSimulation.isActive()) {
            return InputSimulation.isPressed(input);
        }
        for (IGamepad gamepad : this.gamepads()) {
            if (!GamepadJoinTicker.suppressForJoinChord(gamepad, input)
                && gamepad.input().pressed(input)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public float axis(GamepadAxis axis, float deadzone) {
        if (SystemUI.blocksSlot(this.slotId)) {
            return 0.0F;
        }
        if (InputSimulation.isActive()) {
            return InputSimulation.axis(axis, deadzone);
        }
        float strongest = 0.0F;
        for (IGamepad gamepad : this.gamepads()) {
            float value = gamepad.input().axis(axis, deadzone);
            if (Math.abs(value) > Math.abs(strongest)) {
                strongest = value;
            }
        }
        return strongest;
    }

    @Override
    public float rawAxis(GamepadAxis axis) {
        if (SystemUI.blocksSlot(this.slotId)) {
            return 0.0F;
        }
        if (InputSimulation.isActive()) {
            return InputSimulation.rawAxis(axis);
        }
        float strongest = 0.0F;
        for (IGamepad gamepad : this.gamepads()) {
            float value = gamepad.input().rawAxis(axis);
            if (Math.abs(value) > Math.abs(strongest)) {
                strongest = value;
            }
        }
        return strongest;
    }
}
