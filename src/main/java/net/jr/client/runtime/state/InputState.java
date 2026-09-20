package net.jr.client.runtime.state;

import java.util.IdentityHashMap;
import java.util.Map;
import net.jr.client.input.mode.InputMode;
import net.jr.client.runtime.input.binding.KeyMappingState;
import net.jr.client.ui.overlay.ScreenOverlayManager;
import net.jr.experimental_features.gamepad_keyboard.GamepadKeyboardState;
import net.minecraft.client.KeyMapping;

public final class InputState {
    private final Map<KeyMapping, KeyMappingState> keyMappings = new IdentityHashMap<>();
    private final UiNavigationState uiNavigation = new UiNavigationState();
    private final ControlHintAnimationState controlHintAnimations = new ControlHintAnimationState();
    private final ScreenOverlayManager screenOverlays = new ScreenOverlayManager();
    private final GamepadKeyboardState gamepadKeyboard = new GamepadKeyboardState();
    private InputMode mode = InputMode.KEYBOARD_MOUSE;
    private long lastKeyboardMouseInputNanos;
    private long lastGamepadInputNanos;

    public KeyMappingState state(KeyMapping keyMapping) {
        return this.keyMappings.computeIfAbsent(keyMapping, unused -> new KeyMappingState());
    }

    public UiNavigationState uiNavigation() {
        return this.uiNavigation;
    }

    public ControlHintAnimationState controlHintAnimations() {
        return this.controlHintAnimations;
    }

    public GamepadKeyboardState gamepadKeyboard() {
        return this.gamepadKeyboard;
    }

    public ScreenOverlayManager screenOverlays() {
        return this.screenOverlays;
    }

    public InputMode mode() {
        return this.mode;
    }

    public long lastKeyboardMouseInputNanos() {
        return this.lastKeyboardMouseInputNanos;
    }

    public long lastGamepadInputNanos() {
        return this.lastGamepadInputNanos;
    }

    public void markKeyboardMouseInput() {
        this.lastKeyboardMouseInputNanos = System.nanoTime();
        this.mode = InputMode.KEYBOARD_MOUSE;
    }

    public void markGamepadInput() {
        this.lastGamepadInputNanos = System.nanoTime();
        this.mode = InputMode.GAMEPAD;
    }

    public void clear() {
        this.keyMappings.clear();
        this.mode = InputMode.KEYBOARD_MOUSE;
        this.lastKeyboardMouseInputNanos = 0L;
        this.lastGamepadInputNanos = 0L;
        this.uiNavigation.clear();
        this.controlHintAnimations.clear();
        this.screenOverlays.clear();
        this.gamepadKeyboard.clear();
    }
}
