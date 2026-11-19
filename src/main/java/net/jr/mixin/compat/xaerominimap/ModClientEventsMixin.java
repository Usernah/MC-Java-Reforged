package net.jr.mixin.compat.xaerominimap;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.events.ModClientEvents;

@Mixin(value = ModClientEvents.class, remap = false)
abstract class ModClientEventsMixin {
    @Inject(method = "handleRenderModOverlay", at = @At("HEAD"), cancellable = true)
    private void javaReforged$renderEveryViewport(
        GuiGraphicsExtractor graphics,
        DeltaTracker deltaTracker,
        CallbackInfo callback
    ) {
        callback.cancel();
    }
}
