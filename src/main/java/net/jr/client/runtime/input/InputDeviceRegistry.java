package net.jr.client.runtime.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.jr.api.client.input.IGamepad;
import net.jr.api.client.input.IInputDevice;
import net.jr.api.client.input.IInputDeviceRegistry;
import net.jr.api.client.input.IKeyboardMouse;
import net.jr.client.input.sdl.SdlGamepad;

public final class InputDeviceRegistry implements ManagedInputDeviceRegistry {
    private final Map<Long, SdlGamepadDevice> sdlDevices = new HashMap<>();
    private final IKeyboardMouse keyboardMouse = new KeyboardMouseDevice();

    @Override
    public List<IInputDevice> devices() {
        List<IInputDevice> result = new ArrayList<>();
        result.add(this.keyboardMouse);
        result.addAll(this.gamepads());
        return List.copyOf(result);
    }

    @Override
    public List<IGamepad> gamepads() {
        return SdlGamepad.deviceIds().stream().map(this::sdlDevice).map(device -> (IGamepad) device).toList();
    }

    @Override
    public IKeyboardMouse keyboardMouse() {
        return this.keyboardMouse;
    }

    SdlGamepadDevice sdlDevice(long deviceId) {
        return this.sdlDevices.computeIfAbsent(deviceId, SdlGamepadDevice::new);
    }

    @Override
    public void evictDisconnectedExcept(Set<IGamepad> retained) {
        this.sdlDevices.values().removeIf(device -> !device.connected() && !retained.contains(device));
    }
}
