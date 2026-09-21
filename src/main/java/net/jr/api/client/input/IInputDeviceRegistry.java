package net.jr.api.client.input;

import java.util.List;

public interface IInputDeviceRegistry {
    List<IInputDevice> devices();

    List<IGamepad> gamepads();

    IKeyboardMouse keyboardMouse();
}
