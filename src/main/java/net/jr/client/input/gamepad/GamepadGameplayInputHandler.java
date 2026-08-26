package net.jr.client.input.gamepad;

import java.util.function.DoubleSupplier;
import net.jr.client.input.InputApi;
import net.jr.mixin.accessors.ClientInputAccessor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec2;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

public final class GamepadGameplayInputHandler {
    private static final float DEADZONE = 0.2F;
    private static final float CAMERA_SENSITIVITY_X = 24.0F;
    private static final float CAMERA_SENSITIVITY_Y = 24.0F;
    private static final float CAMERA_INPUT_EPSILON = 0.0001F;
    private static final float SNEAK_MOVEMENT_SCALE = 0.3F;

    private GamepadGameplayInputHandler() {
    }

    public static void applyMovement(MovementInputUpdateEvent event) {
        float leftStickX = InputApi.axis(GamepadAxis.LEFT_STICK_X, DEADZONE);
        float leftStickY = InputApi.axis(GamepadAxis.LEFT_STICK_Y, DEADZONE);
        if (leftStickX == 0.0F && leftStickY == 0.0F) return;

        InputApi.markGamepadInput();
        float forwardImpulse = -leftStickY;
        float leftImpulse = -leftStickX;
        if (event.getInput().keyPresses.shift()) {
            forwardImpulse *= SNEAK_MOVEMENT_SCALE;
            leftImpulse *= SNEAK_MOVEMENT_SCALE;
        }
        ((ClientInputAccessor) event.getInput()).javaReforged$setMoveVector(
            new Vec2(leftImpulse, forwardImpulse)
        );
    }

    public static boolean turnCamera(LocalPlayer player, DoubleSupplier frameScale) {
        float rightStickX = GamepadInputCurve.response(InputApi.axis(GamepadAxis.RIGHT_STICK_X, DEADZONE));
        float rightStickY = GamepadInputCurve.response(InputApi.axis(GamepadAxis.RIGHT_STICK_Y, DEADZONE));
        if (Math.abs(rightStickX) <= CAMERA_INPUT_EPSILON
            && Math.abs(rightStickY) <= CAMERA_INPUT_EPSILON) {
            return false;
        }

        InputApi.markGamepadInput();
        double scale = frameScale.getAsDouble();
        player.turn(
            (float) (rightStickX * CAMERA_SENSITIVITY_X * scale),
            (float) (rightStickY * CAMERA_SENSITIVITY_Y * scale)
        );
        return true;
    }
}
