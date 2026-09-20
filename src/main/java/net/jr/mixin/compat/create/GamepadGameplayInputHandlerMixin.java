package net.jr.mixin.compat.create;

import net.jr.client.compat.create.CreateRuntimeState;
import net.jr.client.input.gamepad.GamepadGameplayInputHandler;
import net.jr.mixin.accessors.ClientInputAccessor;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
        value = GamepadGameplayInputHandler.class,
        remap = false
)
abstract class GamepadGameplayInputHandlerMixin {

    @Inject(
            method = "applyMovement",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void javaReforged$clearPlayerMovementWhileDrivingCreate(
            MovementInputUpdateEvent event,
            CallbackInfo ci
    ) {
        if (CreateRuntimeState.isTrainControlsActive()) {
            Input current = event.getInput().keyPresses;
            event.getInput().keyPresses = new Input(
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    current.sprint()
            );
            ((ClientInputAccessor) event.getInput()).javaReforged$setMoveVector(Vec2.ZERO);
            ci.cancel();
        }
    }
}
