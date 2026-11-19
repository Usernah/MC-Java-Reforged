package net.jr.mixin.runtime;

import com.mojang.blaze3d.platform.InputConstants;
import net.jr.client.input.InputApi;
import net.jr.client.runtime.input.binding.KeyMappingRouter;
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
public abstract class KeyMappingMixin {
    @Shadow
    @Final
    private static KeyMappingLookup MAP;

    @Inject(method = "set", at = @At("HEAD"))
    private static void javaReforged$trackPhysicalKeyState(
        InputConstants.Key key,
        boolean down,
        CallbackInfo ci
    ) {
        KeyMappingRouter.setPhysicalDownAll(MAP, key, down);
    }

    @Redirect(
        method = "setAll",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;setDown(Z)V")
    )
    private static void javaReforged$restorePhysicalKeyState(KeyMapping keyMapping, boolean down) {
        InputApi.setPhysicallyDown(keyMapping, down);
        keyMapping.setDown(down);
    }

    @Inject(method = "click", at = @At("HEAD"), cancellable = true)
    private static void javaReforged$clickSlotKeyMapping(InputConstants.Key key, CallbackInfo ci) {
        KeyMappingRouter.clickAll(MAP, key);
        ci.cancel();
    }

    @Redirect(
            method = "isDown",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/KeyMapping;isDown:Z", opcode = Opcodes.GETFIELD)
    )
    private boolean javaReforged$readSlotDown(KeyMapping keyMapping) {
        return InputApi.isDown(keyMapping);
    }

    @Inject(method = "consumeClick", at = @At("HEAD"), cancellable = true)
    private void javaReforged$consumeSlotClick(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(InputApi.consumeClick((KeyMapping)(Object)this));
    }

    @Inject(method = "release", at = @At("HEAD"), cancellable = true)
    private void javaReforged$releaseSlotKeyMapping(CallbackInfo ci) {
        InputApi.release((KeyMapping)(Object)this);
        ci.cancel();
    }

    @Redirect(
            method = "setDown",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/KeyMapping;isDown:Z", opcode = Opcodes.PUTFIELD)
    )
    private void javaReforged$writeSlotDown(KeyMapping keyMapping, boolean down) {
        InputApi.setDown(keyMapping, down);
    }
}
