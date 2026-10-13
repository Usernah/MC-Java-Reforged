package net.jr.client.components.widgets.item;

import net.jr.api.client.render.Draw;
import net.jr.api.client.resource.Asset;
import net.jr.client.components.navigation.UiAction;
import net.jr.client.components.widgets.JavaBaseWidget;
import net.jr.client.ui.container.slots.ItemSlotHighlightRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public class CreativeItemSlotElement extends JavaBaseWidget {
    private static final Asset BACKGROUND =
            Asset.MC("textures/gui/sprites/container/creative_inventory/item_slot.png");
    private final ItemStack stack;
    private final Consumer<ItemStack> selection;
    private final Consumer<ItemStack> focusedPrimarySelection;
    private final Consumer<ItemStack> focusedAlternateSelection;

    public CreativeItemSlotElement(
            int width,
            int height,
            ItemStack stack,
            Consumer<ItemStack> selection,
            Consumer<ItemStack> focusedPrimarySelection,
            Consumer<ItemStack> focusedAlternateSelection
    ) {
        super(0, 0, width, height, Component.empty());
        this.stack = stack == null ? ItemStack.EMPTY : stack;
        this.selection = selection == null ? ignored -> {} : selection;
        this.focusedPrimarySelection = focusedPrimarySelection == null ? ignored -> {} : focusedPrimarySelection;
        this.focusedAlternateSelection = focusedAlternateSelection == null ? ignored -> {} : focusedAlternateSelection;
    }

    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
        Draw.imageFromMeta(BACKGROUND, this.x(), this.y(), this.width(), this.height()).draw(graphics);
        drawItem(graphics, this.stack, this.x(), this.y(), this.width(), this.height());
        if (this.isFocused() || this.isHovered()) {
            ItemSlotHighlightRenderer.draw(graphics, this.getX(), this.getY(), this.getWidth(), this.getHeight());
        }
    }

    @Override public void onClick(double mouseX, double mouseY) { this.selection.accept(this.stack); }
    @Override public boolean handleUiAction(UiAction action) {
        if (!this.active) return false;
        if (action == UiAction.ACCEPT) {
            this.focusedPrimarySelection.accept(this.stack);
            return true;
        }
        if (action != UiAction.ALTERNATE) return false;
        this.focusedAlternateSelection.accept(this.stack);
        return true;
    }

    @Override
    public boolean prefersDirectFocusAction() {
        return true;
    }

    public ItemStack stack() {
        return this.stack;
    }

    static void drawItem(GuiGraphicsExtractor graphics, ItemStack stack, float x, float y, float width, float height) {
        if (stack.isEmpty()) return;
        int size = Math.max(1, Math.min(16, Math.round(Math.min(width, height) - 2.0F)));
        float scale = size / 16.0F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + (width - size) / 2.0F, y + (height - size) / 2.0F);
        graphics.pose().scale(scale, scale);
        graphics.item(stack, 0, 0);
        graphics.pose().popMatrix();
    }

}
