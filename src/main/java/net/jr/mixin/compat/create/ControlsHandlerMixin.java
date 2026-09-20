package net.jr.mixin.compat.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.jr.client.input.InputApi;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(
        targets = "com.simibubi.create.content.contraptions.actors.trainControls.ControlsHandler",
        remap = false
)
abstract class ControlsHandlerMixin {

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target =
                            "Lcom/simibubi/create/foundation/utility/ControlsUtil;"
                                    + "isActuallyPressed("
                                    + "Lnet/minecraft/client/KeyMapping;"
                                    + ")Z"
            )
    )
    private static boolean javaReforged$readRegisteredControl(
            KeyMapping mapping,
            Operation<Boolean> original
    ) {
        return original.call(mapping) || InputApi.isBindingApplied(mapping);
    }
}
