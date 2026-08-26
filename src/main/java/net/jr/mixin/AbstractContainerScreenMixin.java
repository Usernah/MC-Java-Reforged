package net.jr.mixin;

import net.jr.client.ui.container.ContainerHighlightPolicy;
import net.jr.client.ui.container.slots.FloatingItemRenderPass;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Inject(method = "extractSlotHighlightFront", at = @At("HEAD"), cancellable = true)
    private void javaReforged$suppressContainerSelectionFront(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (ContainerHighlightPolicy.suppressFrontHighlight()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractFloatingItem", at = @At("HEAD"), cancellable = true)
    private void javaReforged$extractLargeFloatingItem(
            GuiGraphicsExtractor graphics,
            ItemStack stack,
            int x,
            int y,
            @Nullable String text,
            CallbackInfo ci
    ) {
        FloatingItemRenderPass.extract(graphics, stack, x, y, text);
        ci.cancel();
    }
}
