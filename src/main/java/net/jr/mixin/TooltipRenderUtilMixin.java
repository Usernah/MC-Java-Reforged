package net.jr.mixin;

import net.jr.client.ui.tooltip.TooltipBackgroundPass;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TooltipRenderUtil.class)
public abstract class TooltipRenderUtilMixin {
    @Inject(method = "extractTooltipBackground", at = @At("HEAD"), cancellable = true)
    private static void javaReforged$extractTooltipBackground(
        GuiGraphicsExtractor graphics,
        int x,
        int y,
        int width,
        int height,
        @Nullable Identifier style,
        CallbackInfo callback
    ) {
        if (TooltipBackgroundPass.extractWithPadding(graphics, x, y, width, height, 4, style)) {
            callback.cancel();
        }
    }
}
