package net.jr.api.client.input;

import javax.annotation.Nullable;

public interface IGamepad extends IInputDevice {
    @Nullable GamepadIdentity identity();

    IGamepadInput input();

    IGamepadProfile profile();
}
