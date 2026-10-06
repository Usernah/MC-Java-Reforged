package net.jr.mixin;

import net.jr.client.ui.cursor.ScreenMousePositionResolver;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @ModifyVariable(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int javaReforged$resolveMouseX(int mouseX) {
        return ScreenMousePositionResolver.x(mouseX);
    }

    @ModifyVariable(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int javaReforged$resolveMouseY(int mouseY) {
        return ScreenMousePositionResolver.y(mouseY);
    }

    @Inject(method = "extractTransparentBackground", at = @At("HEAD"),cancellable = true)
    private void javaReforged$TransparentBackground(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if ((Object) this instanceof AbstractContainerScreen<?>) {
            ci.cancel();
        }
    }
}
