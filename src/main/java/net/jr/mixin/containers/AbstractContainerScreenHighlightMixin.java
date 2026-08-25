package net.jr.mixin.containers;

import net.jr.client.render.GuiGraphicsExtractorBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenHighlightMixin {
    @Inject(method = "extractSlotHighlightFront", at = @At("HEAD"), cancellable = true)
    private void javareforged$removeContainerSelectionFront(
        GuiGraphicsExtractor graphics,
        CallbackInfo callback
    ) {
        if (Minecraft.getInstance().gui.screen() instanceof ContainerScreen) {
            callback.cancel();
        }
    }

    @Inject(
            method = "extractFloatingItem",
            at = @At("HEAD"),
            cancellable = true
    )
    private void javareforged$extractLargeFloatingItem(
            GuiGraphicsExtractor graphics,
            ItemStack stack,
            int x,
            int y,
            @Nullable String text,
            CallbackInfo ci
    ) {
        float scale = 20.0F / 16.0F;
        float offset = (20.0F - 16.0F) / 2.0F;

        graphics.pose().pushMatrix();

        graphics.pose().translate(
                x - offset,
                y - offset
        );

        graphics.pose().scale(scale, scale);

        Font itemFont = IClientItemExtensions.of(stack)
                .getFont(
                        stack,
                        IClientItemExtensions.FontContext.ITEM_COUNT
                );

        ((GuiGraphicsExtractorBridge)graphics).javaReforged$renderItemStackAtSize(
                stack,
                0,
                0,
                0,
                20,
                false,
                itemFont != null ? itemFont : Minecraft.getInstance().font,
                text
        );

        graphics.pose().popMatrix();

        ci.cancel();
    }
}
