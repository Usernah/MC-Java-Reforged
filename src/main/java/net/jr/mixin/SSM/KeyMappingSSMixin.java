package net.jr.mixin.SSM;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.jr.client.input.InputApi;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyMappingLookup;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public abstract class KeyMappingSSMixin {
    @Shadow
    @Final
    private static KeyMappingLookup MAP;

    @Inject(method = "click", at = @At("HEAD"), cancellable = true)
    private static void splitTest$clickSlotKeyMapping(InputConstants.Key key, CallbackInfo ci) {
        List<KeyMapping> keyMappings = MAP.getAll(key);
        for (KeyMapping keyMapping : keyMappings) {
            if (keyMapping != null) {
                InputApi.click(keyMapping);
            }
        }
        ci.cancel();
    }

    @Redirect(
            method = "isDown",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/KeyMapping;isDown:Z", opcode = Opcodes.GETFIELD)
    )
    private boolean splitTest$readSlotDown(KeyMapping keyMapping) {
        return InputApi.isDown(keyMapping);
    }

    @Inject(method = "consumeClick", at = @At("HEAD"), cancellable = true)
    private void splitTest$consumeSlotClick(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(InputApi.consumeClick((KeyMapping)(Object)this));
    }

    @Inject(method = "release", at = @At("HEAD"), cancellable = true)
    private void splitTest$releaseSlotKeyMapping(CallbackInfo ci) {
        InputApi.release((KeyMapping)(Object)this);
        ci.cancel();
    }

    @Redirect(
            method = "setDown",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/KeyMapping;isDown:Z", opcode = Opcodes.PUTFIELD)
    )
    private void splitTest$writeSlotDown(KeyMapping keyMapping, boolean down) {
        InputApi.setDown(keyMapping, down);
    }
}
