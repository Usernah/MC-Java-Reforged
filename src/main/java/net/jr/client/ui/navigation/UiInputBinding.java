package net.jr.client.ui.navigation;

import java.util.Objects;
import net.jr.client.input.InputApi;
import net.jr.api.client.input.GamepadDigitalInput;
import net.minecraft.client.KeyMapping;

public sealed interface UiInputBinding
        permits UiInputBinding.KeyMappingInput, UiInputBinding.GamepadInput {

    boolean isDown();

    boolean consumePress(boolean wasDown, boolean down);

    void discardPresses();

    default boolean requiresGamepad() {
        return false;
    }

    static UiInputBinding key(KeyMapping mapping) {
        return new KeyMappingInput(mapping);
    }

    static UiInputBinding gamepad(GamepadDigitalInput input) {
        return new GamepadInput(input);
    }

    record KeyMappingInput(KeyMapping mapping) implements UiInputBinding {
        public KeyMappingInput {
            Objects.requireNonNull(mapping, "mapping");
        }

        @Override
        public boolean isDown() {
            return this.mapping.isDown();
        }

        @Override
        public boolean consumePress(boolean wasDown, boolean down) {
            return this.mapping.consumeClick();
        }

        @Override
        public void discardPresses() {
            while (this.mapping.consumeClick()) {

            }
        }
    }

    record GamepadInput(GamepadDigitalInput input) implements UiInputBinding {
        public GamepadInput {
            Objects.requireNonNull(input, "input");
        }

        @Override
        public boolean isDown() {
            boolean down = InputApi.isPressed(this.input);
            if (down) {
                InputApi.markGamepadInput();
            }
            return down;
        }

        @Override
        public boolean consumePress(boolean wasDown, boolean down) {
            return down && !wasDown;
        }

        @Override
        public void discardPresses() {
        }

        @Override
        public boolean requiresGamepad() {
            return true;
        }
    }
}
