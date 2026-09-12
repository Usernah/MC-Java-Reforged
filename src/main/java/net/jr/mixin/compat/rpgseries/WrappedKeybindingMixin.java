package net.jr.mixin.compat.rpgseries;

import net.jr.client.input.InputApi;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.spell_engine.client.input.WrappedKeybinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WrappedKeybinding.class, remap = false)
abstract class WrappedKeybindingMixin {
    @Shadow
    public KeyMapping original;

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void javaReforged$preferControllerBoundOriginal(
        Options options,
        CallbackInfoReturnable<WrappedKeybinding.Unwrapped> cir
    ) {
        if (InputApi.binding(this.original) != null) {
            cir.setReturnValue(new WrappedKeybinding.Unwrapped(this.original, null));
        }
    }
}
