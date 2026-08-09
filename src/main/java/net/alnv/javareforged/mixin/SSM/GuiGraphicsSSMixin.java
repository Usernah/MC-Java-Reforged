package net.alnv.javareforged.mixin.SSM;

import net.alnv.javareforged.ClientRuntime.runtime.HudPass;
import net.alnv.javareforged.ClientRuntime.runtime.ScreenScale;
import net.alnv.javareforged.ClientRuntime.runtime.ScreenScissors;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsSSMixin {
    @Redirect(
            method = "applyScissor",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;enableScissor(IIII)V")
    )
    private void splitTest$enableViewportRelativeScissor(int x, int y, int width, int height) {
        ScreenScissors.enable(x, y, width, height);
    }

    @Redirect(
            method = "applyScissor",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;disableScissor()V")
    )
    private void splitTest$restoreViewportScissor() {
        ScreenScissors.disableOrRestoreViewport();
    }

    @Inject(method = "guiWidth", at = @At("HEAD"), cancellable = true)
    private void splitTest$useViewportGuiWidth(CallbackInfoReturnable<Integer> callback) {
        Integer screenWidth = ScreenScale.activeGuiWidthOrNull();
        if (screenWidth != null) {
            callback.setReturnValue(screenWidth);
            return;
        }
        Integer width = HudPass.guiWidthOrNull();
        if (width != null) {
            callback.setReturnValue(width);
        }
    }

    @Inject(method = "guiHeight", at = @At("HEAD"), cancellable = true)
    private void splitTest$useViewportGuiHeight(CallbackInfoReturnable<Integer> callback) {
        Integer screenHeight = ScreenScale.activeGuiHeightOrNull();
        if (screenHeight != null) {
            callback.setReturnValue(screenHeight);
            return;
        }
        Integer height = HudPass.guiHeightOrNull();
        if (height != null) {
            callback.setReturnValue(height);
        }
    }
}
