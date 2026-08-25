package net.jr.mixin.runtime;

import java.util.function.BooleanSupplier;
import net.jr.client.input.InputApi;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.state.OptionsState;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.ToggleKeyMapping;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ToggleKeyMapping.class)
public abstract class ToggleKeyMappingSlotStateMixin {
    @Redirect(
            method = "isDown",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/ToggleKeyMapping;isDown:Z", opcode = Opcodes.GETFIELD)
    )
    private boolean splitTest$readSlotToggleDown(ToggleKeyMapping keyMapping) {
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
    private boolean splitTest$readSlotToggleMode(BooleanSupplier vanillaMode) {
        KeyMapping keyMapping = (KeyMapping)(Object)this;
        OptionsState options = ClientRuntime.INSTANCE
                .slots()
                .slot(InputApi.currentSlotId())
                .optionsState();
        return switch (keyMapping.getName()) {
            case "key.sneak" -> options.toggleCrouch().get();
            case "key.sprint" -> options.toggleSprint().get();
            case "key.attack" -> options.toggleAttack().get();
            case "key.use" -> options.toggleUse().get();
            default -> vanillaMode.getAsBoolean();
        };
    }
}
