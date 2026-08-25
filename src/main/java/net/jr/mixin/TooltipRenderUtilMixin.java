package net.jr.mixin;

import net.jr.api.client.resource.Asset;
import net.jr.client.components.elements.ImageElement;
import net.minecraft.client.Minecraft;
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
    private static final Asset TOOLTIP_BACKGROUND = Asset.MC("textures/gui/tooltip/tooltip_background.png");

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
        if (style != null || TOOLTIP_BACKGROUND.find(Minecraft.getInstance().getResourceManager()).isEmpty()) {
            return;
        }

        ImageElement background = new ImageElement(TOOLTIP_BACKGROUND, x - 3, y - 3, width + 6, height + 6, graphics);
        background.setUseMeta(true);
        background.draw();
        callback.cancel();
    }
}
