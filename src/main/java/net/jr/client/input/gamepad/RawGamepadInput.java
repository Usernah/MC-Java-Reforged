package net.jr.client.input.gamepad;

public record RawGamepadInput(String type, int index, int value) {
    public static RawGamepadInput button(int index) {
        return new RawGamepadInput("button", index, 1);
    }

    public static RawGamepadInput gamepadButton(int index) {
        return new RawGamepadInput("controller_button", index, 1);
    }

    public static RawGamepadInput hat(int index, int value) {
        return new RawGamepadInput("hat", index, value);
    }

    public static RawGamepadInput axis(int index, int direction) {
        return new RawGamepadInput("axis", index, direction >= 0 ? 1 : -1);
    }

    public String displayName() {
        return switch (this.type) {
            case "controller_button" -> "controller button " + this.index;
            case "hat" -> "hat " + this.index + " value " + this.value;
            case "axis" -> "axis " + this.index + (this.value > 0 ? "+" : "-");
            default -> "button " + this.index;
        };
    }
}

