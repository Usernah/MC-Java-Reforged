package net.jr.mixin.runtime;

import com.mojang.blaze3d.platform.Window;
import net.jr.client.runtime.viewport.ViewportGuiDimensions;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ContextualBar.class)
public interface ContextualBarMixin {
    @Inject(
            method = "left",
            at = @At("HEAD"),
            cancellable = true
    )
    private void javaReforged$viewportLeft(
            Window window,
            CallbackInfoReturnable<Integer> callback
    ) {
        Integer width = ViewportGuiDimensions.activeWidthOrNull();

        if (width != null) {
            callback.setReturnValue((width - 182) / 2);
        }
    }

    @Inject(
            method = "top",
            at = @At("HEAD"),
            cancellable = true
    )
    private void javaReforged$viewportTop(
            Window window,
            CallbackInfoReturnable<Integer> callback
    ) {
        Integer height = ViewportGuiDimensions.activeHeightOrNull();

        if (height != null) {
            callback.setReturnValue(height - 29);
        }
    }
}
