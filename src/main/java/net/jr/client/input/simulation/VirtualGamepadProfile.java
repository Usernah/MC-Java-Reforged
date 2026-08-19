package net.jr.client.input.simulation;

import java.util.EnumSet;
import java.util.Set;
import net.jr.client.input.gamepad.GamepadDigitalInput;

public enum VirtualGamepadProfile {
    XBOX(
        standardButtons(),
        extra(GamepadDigitalInput.MISC_1)
    ),
    XBOX_PRO(
        standardButtons(),
        extra(
            GamepadDigitalInput.MISC_1,
            GamepadDigitalInput.PADDLE_RIGHT_1,
            GamepadDigitalInput.PADDLE_LEFT_1,
            GamepadDigitalInput.PADDLE_RIGHT_2,
            GamepadDigitalInput.PADDLE_LEFT_2
        )
    ),
    PS4(
        standardButtons(),
        extra(
            GamepadDigitalInput.TOUCHPAD_BUTTON,
            GamepadDigitalInput.TOUCHPAD_LEFT_BUTTON,
            GamepadDigitalInput.TOUCHPAD_RIGHT_BUTTON
        )
    ),
    PS5(
        standardButtons(),
        extra(
            GamepadDigitalInput.TOUCHPAD_BUTTON,
            GamepadDigitalInput.TOUCHPAD_LEFT_BUTTON,
            GamepadDigitalInput.TOUCHPAD_RIGHT_BUTTON,
            GamepadDigitalInput.MISC_1
        )
    ),
    NS2_JOYCON_PAIR(
        standardButtons(),
        extra(GamepadDigitalInput.MISC_1)
    ),
    NS2_STANDARD(
        standardButtons(),
        extra(GamepadDigitalInput.MISC_1)
    ),
    NS2_PRO(
        standardButtons(),
        extra(GamepadDigitalInput.MISC_1, GamepadDigitalInput.PADDLE_LEFT_1, GamepadDigitalInput.PADDLE_RIGHT_1)
    ),
    STEAM_DECK(
        standardButtons(),
        extra(
            GamepadDigitalInput.BUTTON_GUIDE,
            GamepadDigitalInput.MISC_1,
            GamepadDigitalInput.PADDLE_RIGHT_1,
            GamepadDigitalInput.PADDLE_LEFT_1,
            GamepadDigitalInput.PADDLE_RIGHT_2,
            GamepadDigitalInput.PADDLE_LEFT_2
        )
    ),
    GENERIC(standardButtons(), extra());

    private final Set<GamepadDigitalInput> supportedInputs;

    VirtualGamepadProfile(Set<GamepadDigitalInput> baseInputs, Set<GamepadDigitalInput> extraInputs) {
        EnumSet<GamepadDigitalInput> inputs = EnumSet.copyOf(baseInputs);
        inputs.addAll(extraInputs);
        this.supportedInputs = Set.copyOf(inputs);
    }

    public boolean supports(GamepadDigitalInput input) {
        return this.supportedInputs.contains(input);
    }

    public Set<GamepadDigitalInput> supportedInputs() {
        return this.supportedInputs;
    }

    private static Set<GamepadDigitalInput> standardButtons() {
        return EnumSet.of(
            GamepadDigitalInput.BUTTON_DOWN,
            GamepadDigitalInput.BUTTON_RIGHT,
            GamepadDigitalInput.BUTTON_LEFT,
            GamepadDigitalInput.BUTTON_UP,
            GamepadDigitalInput.BUTTON_START,
            GamepadDigitalInput.BUTTON_SELECT,
            GamepadDigitalInput.BUTTON_GUIDE,
            GamepadDigitalInput.STICK_LEFT_BUTTON,
            GamepadDigitalInput.STICK_RIGHT_BUTTON,
            GamepadDigitalInput.BUMPER_LEFT,
            GamepadDigitalInput.BUMPER_RIGHT,
            GamepadDigitalInput.DPAD_UP,
            GamepadDigitalInput.DPAD_DOWN,
            GamepadDigitalInput.DPAD_LEFT,
            GamepadDigitalInput.DPAD_RIGHT,
            GamepadDigitalInput.TRIGGER_LEFT,
            GamepadDigitalInput.TRIGGER_RIGHT,
            GamepadDigitalInput.STICK_LEFT_MOVE_UP,
            GamepadDigitalInput.STICK_LEFT_MOVE_DOWN,
            GamepadDigitalInput.STICK_LEFT_MOVE_LEFT,
            GamepadDigitalInput.STICK_LEFT_MOVE_RIGHT,
            GamepadDigitalInput.STICK_RIGHT_MOVE_UP,
            GamepadDigitalInput.STICK_RIGHT_MOVE_DOWN,
            GamepadDigitalInput.STICK_RIGHT_MOVE_LEFT,
            GamepadDigitalInput.STICK_RIGHT_MOVE_RIGHT
        );
    }

    private static Set<GamepadDigitalInput> extra(GamepadDigitalInput... inputs) {
        EnumSet<GamepadDigitalInput> set = EnumSet.noneOf(GamepadDigitalInput.class);
        for (GamepadDigitalInput input : inputs) {
            set.add(input);
        }
        return set;
    }
}

