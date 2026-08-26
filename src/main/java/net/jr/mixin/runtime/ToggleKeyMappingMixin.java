package net.jr.mixin.runtime;

import java.util.function.BooleanSupplier;
import net.jr.client.input.InputApi;
import net.jr.client.runtime.input.binding.KeyMappingRouter;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.ToggleKeyMapping;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ToggleKeyMapping.class)
public abstract class ToggleKeyMappingMixin {
    @Redirect(
            method = "isDown",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/ToggleKeyMapping;isDown:Z", opcode = Opcodes.GETFIELD)
    )
    private boolean javaReforged$readSlotToggleDown(ToggleKeyMapping keyMapping) {
        return InputApi.isDown((KeyMapping)(Object)keyMapping);
    }

    @Redirect(
            method = {
                    "shouldSetOnIngameFocus",
                    "setDown",
                    "isDown",
                    "release",
                    "shouldRestoreStateOnScreenClosed"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/function/BooleanSupplier;getAsBoolean()Z"
            )
    )
    private boolean javaReforged$readSlotToggleMode(BooleanSupplier vanillaMode) {
        return KeyMappingRouter.toggleMode((KeyMapping) (Object) this, vanillaMode);
    }
}
