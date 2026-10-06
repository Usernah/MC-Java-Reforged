package net.jr.client.runtime.input;

import net.jr.api.client.input.IInputAssignment;
import net.jr.api.client.input.IInputDeviceRegistry;
import net.jr.api.client.input.IMutableInputAssignment;
import net.jr.client.input.simulation.InputSimulation;
import net.minecraft.client.Minecraft;

/** Composition root for physical devices, assignment and local joining. */
public final class InputRuntime {
    private final InputDeviceRegistry devices = new InputDeviceRegistry();
    private final InputAssignment assignments = new InputAssignment(
        this.devices,
        () -> Minecraft.getInstance().level == null,
        InputSimulation::isConnected
    );
    private final JoinService join = new JoinService(this.devices, this.assignments);

    public IInputDeviceRegistry devices() {
        return this.devices;
    }

    public IInputAssignment assignments() {
        return this.assignments;
    }

    public JoinService join() {
        return this.join;
    }

    InputDeviceRegistry deviceRegistry() {
        return this.devices;
    }

    public IMutableInputAssignment mutableAssignments() {
        return this.assignments;
    }
}
