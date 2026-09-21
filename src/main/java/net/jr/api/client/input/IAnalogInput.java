package net.jr.api.client.input;

public interface IAnalogInput {
    float axis(GamepadAxis axis, float deadzone);

    float rawAxis(GamepadAxis axis);
}
