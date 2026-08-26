package net.jr.client.ui.container.slots;

import net.jr.client.render.GuiGraphicsExtractorBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

public final class FloatingItemRenderPass {
    private static final int ITEM_SIZE = 20;
    private static final int VANILLA_ITEM_SIZE = 16;

    private FloatingItemRenderPass() {
    }

    public static void extract(
            GuiGraphicsExtractor graphics,
            ItemStack stack,
            int x,
            int y,
            @Nullable String text
    ) {
        float scale = (float) ITEM_SIZE / VANILLA_ITEM_SIZE;
        float offset = (ITEM_SIZE - VANILLA_ITEM_SIZE) / 2.0F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x - offset, y - offset);
        graphics.pose().scale(scale, scale);

        Font itemFont = IClientItemExtensions.of(stack)
                .getFont(stack, IClientItemExtensions.FontContext.ITEM_COUNT);
        ((GuiGraphicsExtractorBridge) graphics).javaReforged$renderItemStackAtSize(
                stack,
                0,
                0,
                0,
                ITEM_SIZE,
                false,
                itemFont != null ? itemFont : Minecraft.getInstance().font,
                text
        );
        graphics.pose().popMatrix();
    }
}
