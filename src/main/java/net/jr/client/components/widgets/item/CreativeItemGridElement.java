package net.jr.client.components.widgets.item;

import net.jr.client.components.widgets.JavaBaseWidget;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public class CreativeItemGridElement extends AbstractItemGridElement<ItemStack> {
    private Consumer<ItemStack> selection = ignored -> {};
    private Consumer<ItemStack> focusedPrimarySelection = ignored -> {};
    private Consumer<ItemStack> focusedAlternateSelection = ignored -> {};
    private final CreativeItemSlotStyle slotStyle;

    public CreativeItemGridElement(
            int x,
            int y,
            int width,
            int height,
            int visibleRows,
            int columns,
            CreativeItemSlotStyle slotStyle
    ) {
        super(x, y, width, height, visibleRows, columns);
        this.slotStyle = slotStyle;
    }

    public void onSelection(Consumer<ItemStack> selection) {
        this.selection = selection == null ? ignored -> {} : selection;
    }

    public void onFocusedPrimarySelection(Consumer<ItemStack> focusedPrimarySelection) {
        this.focusedPrimarySelection = focusedPrimarySelection == null ? ignored -> {} : focusedPrimarySelection;
    }

    public void onFocusedAlternateSelection(Consumer<ItemStack> focusedAlternateSelection) {
        this.focusedAlternateSelection = focusedAlternateSelection == null ? ignored -> {} : focusedAlternateSelection;
    }

    @Override protected JavaBaseWidget createCell(ItemStack entry, int index, int width, int height) {
        return new CreativeItemSlotElement(
                width,
                height,
                entry,
                this.selection,
                this.focusedPrimarySelection,
                this.focusedAlternateSelection,
                this.slotStyle
        );
    }

    public ItemStack hoveredStack(boolean includeFocused) {
        for (JavaBaseWidget child : this.childWidgets()) {
            if (child instanceof CreativeItemSlotElement slot && child.isHovered()) {
                return slot.stack();
            }
        }
        if (includeFocused && this.getFocused() instanceof CreativeItemSlotElement slot) {
            return slot.stack();
        }
        return ItemStack.EMPTY;
    }

    public ItemStack previewedStack(double mouseX, double mouseY, boolean includeFocused) {
        CreativeItemSlotElement slot = this.previewedSlot(mouseX, mouseY, includeFocused);
        return slot == null ? ItemStack.EMPTY : slot.stack();
    }

    public CreativeItemSlotElement previewedSlot(
            double mouseX,
            double mouseY,
            boolean includeFocused
    ) {
        JavaBaseWidget cell = this.previewedCellAt(mouseX, mouseY, includeFocused);
        return cell instanceof CreativeItemSlotElement slot ? slot : null;
    }
}
