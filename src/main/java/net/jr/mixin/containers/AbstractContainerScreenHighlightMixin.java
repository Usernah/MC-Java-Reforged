package net.jr.mixin.containers;

import net.jr.Java_reforged;
import net.jr.api.client.render.Draw;
import net.jr.api.client.resource.Asset;
import net.jr.mixin.controlhints.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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

        // Vanilla nos entrega x/y para un item de 16x16.
        // Lo desplazamos 2 px para que el de 20x20 siga centrado
        // exactamente en el cursor.
        graphics.pose().translate(
                x - offset,
                y - offset
        );

        graphics.pose().scale(scale, scale);

        graphics.item(stack, 0, 0);

        Font itemFont = IClientItemExtensions.of(stack)
                .getFont(
                        stack,
                        IClientItemExtensions.FontContext.ITEM_COUNT
                );

        graphics.itemDecorations(
                itemFont != null ? itemFont : Minecraft.getInstance().font,
                stack,
                0,
                0,
                text
        );

        graphics.pose().popMatrix();

        ci.cancel();
    }
}
