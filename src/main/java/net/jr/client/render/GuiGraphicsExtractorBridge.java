package net.jr.client.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface GuiGraphicsExtractorBridge {

    void javaReforged$enablePreciseScissor(
            double left,
            double top,
            double right,
            double bottom
    );

    @Nullable
    ScreenRectangle javaReforged$currentScissor();

    void javaReforged$renderItemAtSize(
            ItemStack stack,
            int x,
            int y,
            int seed,
            int size,
            boolean fakeItem
    );

    void javaReforged$renderItemStackAtSize(
            ItemStack stack,
            int x,
            int y,
            int seed,
            int size,
            boolean fakeItem,
            @Nullable Font decorationFont,
            @Nullable String countText
    );
}
