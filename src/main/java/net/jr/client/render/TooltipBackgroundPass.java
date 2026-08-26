package net.jr.client.render;

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
        ImageElement background = new ImageElement(BACKGROUND, x - 3, y - 3, width + 6, height + 6, graphics);
        background.setUseMeta(true);
        background.draw();
        return true;
    }
}
