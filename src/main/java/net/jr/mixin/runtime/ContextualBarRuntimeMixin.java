package net.jr.mixin.runtime;


import com.mojang.blaze3d.platform.Window;
import net.jr.client.runtime.viewport.ViewportGuiScale;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ContextualBar.class)
public interface ContextualBarRuntimeMixin {
    @Inject(
            method = "left",
            at = @At("HEAD"),
            cancellable = true
    )
    private void splitTest$viewportLeft(
            Window window,
            CallbackInfoReturnable<Integer> callback
    ) {
        Integer width = ViewportGuiScale.activeGuiWidthOrNull();

        if (width != null) {
            callback.setReturnValue((width - 182) / 2);
        }
    }

    @Inject(
            method = "top",
            at = @At("HEAD"),
            cancellable = true
    )
    private void splitTest$viewportTop(
            Window window,
            CallbackInfoReturnable<Integer> callback
    ) {
        Integer height = ViewportGuiScale.activeGuiHeightOrNull();

        if (height != null) {
            callback.setReturnValue(height - 29);
        }
    }
}
