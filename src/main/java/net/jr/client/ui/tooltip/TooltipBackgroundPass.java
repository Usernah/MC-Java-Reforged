package net.jr.client.ui.tooltip;

import net.jr.api.client.resource.Asset;
import net.jr.client.components.elements.ImageElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public final class TooltipBackgroundPass {
    private static final Asset BACKGROUND = Asset.MC("textures/gui/tooltip/tooltip_background.png");

    private TooltipBackgroundPass() {
    }

    public static boolean extractWithPadding(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            int padding,
            @Nullable Identifier style
    ) {

        return extract(graphics, x - padding, y - padding, width + (padding * 2), height + (padding * 2), style);
    }
    public static boolean extract(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            @Nullable Identifier style
    ) {
        if (style != null || BACKGROUND.find(Minecraft.getInstance().getResourceManager()).isEmpty()) {
            return false;
        }
        ImageElement background = new ImageElement(BACKGROUND, x , y, width, height, graphics);
        background.setUseMeta(true);
        background.draw();
        return true;
    }
}
