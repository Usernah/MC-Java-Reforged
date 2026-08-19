package net.jr.mixin.runtime;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.viewport.ViewportArea;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.alnv.reforged_api.API.utils.EffectManager", remap = false)
public abstract class EffectManagerSSMixin {
    @Inject(method = "currentContextId", at = @At("HEAD"), cancellable = true)
    private static void javareforged$currentClientEffectContext(CallbackInfoReturnable<Integer> cir) {
        Integer slotId = SlotScope.idOrNull();
        if (slotId != null) {
            cir.setReturnValue(slotId);
        }
    }

    @Inject(method = "currentPhysicalWidth", at = @At("HEAD"), cancellable = true)
    private static void javareforged$currentClientPhysicalWidth(Minecraft minecraft, CallbackInfoReturnable<Integer> cir) {
        ViewportArea viewport = currentViewportOrNull();
        if (viewport != null) {
            cir.setReturnValue(Math.max(1, viewport.glWidth()));
        }
    }

    @Inject(method = "currentPhysicalHeight", at = @At("HEAD"), cancellable = true)
    private static void javareforged$currentClientPhysicalHeight(Minecraft minecraft, CallbackInfoReturnable<Integer> cir) {
        ViewportArea viewport = currentViewportOrNull();
        if (viewport != null) {
            cir.setReturnValue(Math.max(1, viewport.glHeight()));
        }
    }

    @Inject(method = "currentGuiWidth", at = @At("HEAD"), cancellable = true)
    private static void javareforged$currentClientGuiWidth(Minecraft minecraft, CallbackInfoReturnable<Float> cir) {
        ViewportArea viewport = currentViewportOrNull();
        if (viewport != null) {
            cir.setReturnValue((float)Math.max(1, viewport.guiWidth()));
        }
    }

    @Inject(method = "currentGuiHeight", at = @At("HEAD"), cancellable = true)
    private static void javareforged$currentClientGuiHeight(Minecraft minecraft, CallbackInfoReturnable<Float> cir) {
        ViewportArea viewport = currentViewportOrNull();
        if (viewport != null) {
            cir.setReturnValue((float)Math.max(1, viewport.guiHeight()));
        }
    }

    private static ViewportArea currentViewportOrNull() {
        Integer slotId = SlotScope.idOrNull();
        return slotId != null ? ClientRuntime.INSTANCE.viewports().viewportOrNull(slotId) : null;
    }
}
