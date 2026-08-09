package net.jr.mixin.containers;

import net.jr.Java_reforged;
import net.jr.api.client.render.Draw;
import net.jr.api.client.resource.Asset;
import net.jr.mixin.controlhints.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenHighlightMixin {
    @Unique
    private static final Asset JAVAREFORGED$CONTAINER_SELECTION_TEXTURE =
        Asset.NamespaceAndPatch(
            Java_reforged.MODID,
            "textures/gui/containers/inventory/slot_selection.png"
        );
    @Unique
    private static final float JAVAREFORGED$CONTAINER_SELECTION_OFFSET = -1.0F;
    @Unique
    private static final float JAVAREFORGED$CONTAINER_SELECTION_WIDTH = 24.0F;
    @Unique
    private static final float JAVAREFORGED$CONTAINER_SELECTION_HEIGHT = 24.0F;

    @Inject(method = "extractSlotHighlightBack", at = @At("HEAD"), cancellable = true)
    private void javareforged$renderContainerSelectionBack(
        GuiGraphicsExtractor graphics,
        CallbackInfo callback
    ) {
        if (!(Minecraft.getInstance().gui.screen() instanceof ContainerScreen)) {
            return;
        }

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        var hoveredSlot = ((AbstractContainerScreenAccessor) screen).javareforged$getHoveredSlot();
        if (hoveredSlot == null || !hoveredSlot.isHighlightable()) {
            callback.cancel();
            return;
        }

        Draw.imageFromMeta(
            JAVAREFORGED$CONTAINER_SELECTION_TEXTURE,
            hoveredSlot.x + JAVAREFORGED$CONTAINER_SELECTION_OFFSET,
            hoveredSlot.y + JAVAREFORGED$CONTAINER_SELECTION_OFFSET,
            JAVAREFORGED$CONTAINER_SELECTION_WIDTH,
            JAVAREFORGED$CONTAINER_SELECTION_HEIGHT
        ).uvSize(24.0F, 24.0F).atlasSize(24.0F, 24.0F).draw(graphics);
        callback.cancel();
    }

    @Inject(method = "extractSlotHighlightFront", at = @At("HEAD"), cancellable = true)
    private void javareforged$removeContainerSelectionFront(
        GuiGraphicsExtractor graphics,
        CallbackInfo callback
    ) {
        if (Minecraft.getInstance().gui.screen() instanceof ContainerScreen) {
            callback.cancel();
        }
    }
}
