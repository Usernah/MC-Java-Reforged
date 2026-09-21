package net.jr.api.client.input;

public interface IGamepadInput extends IDigitalInput, IAnalogInput {
    boolean calibrationInputActive();
}
