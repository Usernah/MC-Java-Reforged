package net.jr.mixin.runtime;

import java.util.function.BooleanSupplier;
import net.jr.client.input.InputApi;
import net.jr.client.runtime.input.binding.MomentaryShiftInput;
import net.jr.client.runtime.input.binding.KeyMappingRouter;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.ToggleKeyMapping;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ToggleKeyMapping.class)
public abstract class ToggleKeyMappingMixin {
    @Inject(method = "setDown", at = @At("HEAD"), cancellable = true)
    private void javaReforged$keepMomentaryShiftOutOfToggleState(boolean down, CallbackInfo ci) {
        KeyMapping keyMapping = (KeyMapping)(Object)this;
        if ("key.sneak".equals(keyMapping.getName()) && MomentaryShiftInput.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "release", at = @At("HEAD"))
    private void javaReforged$releasePhysicalDown(CallbackInfo ci) {
        InputApi.setPhysicallyDown((KeyMapping)(Object)this, false);
    }

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
