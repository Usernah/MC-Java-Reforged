package net.jr.client.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class GuiItemExtractionHandler {
    private GuiItemExtractionHandler() {
    }

    public static void renderAtSize(
            GuiGraphicsExtractor graphics,
            ItemStack stack,
            int x,
            int y,
            int seed,
            int size,
            boolean fakeItem
    ) {
        ItemRenderResolution.drawAtSize(size, () -> {
            if (fakeItem) {
                graphics.fakeItem(stack, x, y, seed);
            } else {
                graphics.item(stack, x, y, seed);
            }
        });
    }

    public static void renderStackAtSize(
            GuiGraphicsExtractor graphics,
            ItemStack stack,
            int x,
            int y,
            int seed,
            int size,
            boolean fakeItem,
            @Nullable Font decorationFont,
            @Nullable String countText
    ) {
        renderAtSize(graphics, stack, x, y, seed, size, fakeItem);
        if (decorationFont != null) {
            graphics.itemDecorations(decorationFont, stack, x, y, countText);
        }
    }
}
