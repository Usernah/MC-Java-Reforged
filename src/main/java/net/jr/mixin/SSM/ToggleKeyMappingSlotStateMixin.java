package net.jr.mixin.SSM;

import net.jr.client.input.InputApi;
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
}
