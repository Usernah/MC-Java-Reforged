package net.jr.client.input.gamepad;

import net.jr.api.client.input.GamepadAxis;
import net.jr.api.client.input.ISlotInput;

import java.util.function.DoubleSupplier;
import net.jr.client.input.InputApi;
import net.jr.client.runtime.options.OptionsStateResolver;
import net.jr.client.runtime.state.OptionsState;
import net.jr.mixin.accessors.ClientInputAccessor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec2;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

public final class GamepadGameplayInputHandler {
    private static final float DEADZONE = 0.2F;
    private static final float CAMERA_INPUT_EPSILON = 0.0001F;
    private static final float SNEAK_MOVEMENT_SCALE = 0.3F;

    private GamepadGameplayInputHandler() {
    }

    public static void applyMovement(MovementInputUpdateEvent event, ISlotInput input) {
        float leftStickX = input.axis(GamepadAxis.LEFT_STICK_X, DEADZONE);
        float leftStickY = input.axis(GamepadAxis.LEFT_STICK_Y, DEADZONE);
        if (leftStickX == 0.0F && leftStickY == 0.0F) return;

        InputApi.markGamepadInput();
        OptionsState optionsState = OptionsStateResolver.current();
        float forwardImpulse = optionsState.invertControllerMovementY().get()
                ? leftStickY
                : -leftStickY;
        float leftImpulse = -leftStickX;
        if (event.getInput().keyPresses.shift()) {
            forwardImpulse *= SNEAK_MOVEMENT_SCALE;
            leftImpulse *= SNEAK_MOVEMENT_SCALE;
        }
        ((ClientInputAccessor) event.getInput()).javaReforged$setMoveVector(
            new Vec2(leftImpulse, forwardImpulse)
        );
    }

    public static boolean turnCamera(LocalPlayer player, ISlotInput input, DoubleSupplier frameScale) {
        float rightStickX = GamepadInputCurve.response(input.axis(GamepadAxis.RIGHT_STICK_X, DEADZONE));
        float rightStickY = GamepadInputCurve.response(input.axis(GamepadAxis.RIGHT_STICK_Y, DEADZONE));
        if (Math.abs(rightStickX) <= CAMERA_INPUT_EPSILON
            && Math.abs(rightStickY) <= CAMERA_INPUT_EPSILON) {
            return false;
        }

        InputApi.markGamepadInput();
        OptionsState optionsState = OptionsStateResolver.current();
        double sensitivity = player.isScoping()
                ? optionsState.controllerSpyglassSensitivity().get()
                : optionsState.controllerCameraSensitivity().get();
        if (optionsState.invertControllerCameraX().get()) {
            rightStickX = -rightStickX;
        }
        if (optionsState.invertControllerCameraY().get()) {
            rightStickY = -rightStickY;
        }
        double scale = frameScale.getAsDouble();
        player.turn(
            (float) (rightStickX * sensitivity * scale),
            (float) (rightStickY * sensitivity * scale)
        );
        return true;
    }
}
