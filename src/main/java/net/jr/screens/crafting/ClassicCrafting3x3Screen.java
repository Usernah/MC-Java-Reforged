package net.jr.screens.crafting;

import net.jr.api.client.ui.DslContainer;
import net.jr.api.client.ui.UiContainerFile;
import net.jr.client.ui.container.DslContainerSize;
import net.jr.client.ui.container.slots.CustomItemSlotLayout;
import net.jr.client.ui.container.slots.ItemSlotGrid;
import net.jr.client.ui.container.slots.VanillaItemSlotLayer;
import net.jr.registry.ModUi;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingMenu;

public final class ClassicCrafting3x3Screen extends AbstractContainerScreen<CraftingMenu>
        implements DslContainer, CustomItemSlotLayout {
    private VanillaItemSlotLayer itemSlotLayer;
    private ItemSlotGrid itemSlotGrid;

    public ClassicCrafting3x3Screen(CraftingMenu menu, Inventory inventory, Component title) {
        this(menu, inventory, title, DslContainerSize.resolve(ModUi.CLASSIC_CRAFTING_3X3, 176, 166));
    }

    private ClassicCrafting3x3Screen(
            CraftingMenu menu,
            Inventory inventory,
            Component title,
            DslContainerSize dimensions
    ) {
        super(menu, inventory, title, dimensions.width(), dimensions.height());
    }

    @Override
    public UiContainerFile dslFile() {
        return ModUi.CLASSIC_CRAFTING_3X3;
    }

    @Override
    protected void init() {
        super.init();
        this.dslData().exposeText("crafting.title", () -> this.title.getString());
        this.dslData().exposeText("crafting.inventory_title", () -> this.playerInventoryTitle.getString());
        this.itemSlotGrid = this.dslItemSlotGrid("menu");
        if (this.itemSlotGrid == null) {
            throw new IllegalStateException("Classic crafting DSL must declare item_slots id 'menu'");
        }
        this.itemSlotLayer = this.itemSlotGrid.itemSlotLayer();
    }

    @Override
    public ItemSlotGrid itemSlotGrid() {
        return this.itemSlotGrid;
    }

    @Override
    public VanillaItemSlotLayer itemSlotLayer() {
        return this.itemSlotLayer;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.extractDslBackground(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.extractDslContent(graphics, mouseX, mouseY, partialTick);
        this.itemSlotLayer.updateHovered(this.itemSlotGrid, mouseX, mouseY);
        this.itemSlotLayer.renderItemSlotMap(graphics, this.itemSlotGrid);
        this.itemSlotLayer.renderHoveredItemSlot(graphics, this.itemSlotGrid);
        this.itemSlotLayer.renderCarriedItem(graphics, mouseX, mouseY);
        this.itemSlotLayer.renderTooltip(graphics, this.itemSlotGrid, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        GuiEventListener child = this.getChildAt(event.x(), event.y()).orElse(null);
        if (child != null) {
            boolean handled = child.mouseClicked(event, doubleClick);
            if (handled && child.shouldTakeFocusAfterInteraction()) {
                this.setFocused(child);
                if (event.button() == 0) this.setDragging(true);
            }
            return true;
        }
        return this.itemSlotLayer.mouseClicked(this.itemSlotGrid, event.x(), event.y(), event.button(),
                true, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && this.isDragging()) {
            this.setDragging(false);
            GuiEventListener focused = this.getFocused();
            if (focused != null) return focused.mouseReleased(event);
        }
        return this.itemSlotLayer.mouseReleased(this.itemSlotGrid, event.x(), event.y(), event.button(),
                true, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        GuiEventListener focused = this.getFocused();
        if (focused != null && this.isDragging() && event.button() == 0) {
            return focused.mouseDragged(event, dragX, dragY);
        }
        return this.itemSlotLayer.mouseDragged(this.itemSlotGrid, event.x(), event.y(), event.button(), dragX, dragY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return this.itemSlotLayer.keyPressed(this.itemSlotGrid, event.key(), event.scancode())
                || super.keyPressed(event);
    }
}
