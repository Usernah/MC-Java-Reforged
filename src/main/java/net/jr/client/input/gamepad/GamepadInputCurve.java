package net.jr.client.input.gamepad;

public final class GamepadInputCurve {
    private GamepadInputCurve() {
    }

    public static float response(float input) {
        float magnitude = Math.abs(input);
        return Math.copySign(magnitude * magnitude, input);
    }
}
