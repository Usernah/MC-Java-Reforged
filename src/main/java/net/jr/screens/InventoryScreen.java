package net.jr.screens;

import net.jr.api.client.render.Draw;
import net.jr.api.client.resource.Asset;
import net.jr.client.ui.container.slots.CustomSlotLayout;
import net.jr.client.ui.container.slots.SlotGrid;
import net.jr.client.ui.container.slots.SlotGroup;
import net.jr.client.ui.container.slots.SlotMaps;
import net.jr.client.ui.container.slots.VanillaSlotLayer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.item.crafting.display.StonecutterRecipeDisplay;


public final class InventoryScreen extends AbstractContainerScreen<InventoryMenu>
        implements CustomSlotLayout {
    private static final Asset BACKGROUND =
            Asset.MC("textures/gui/container/inventory.png");

    private VanillaSlotLayer slotLayer;
    private SlotGrid slotGrid;

    public InventoryScreen(InventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    protected void init() {
        super.init();
        this.slotLayer = new VanillaSlotLayer(this, this.menu, this.font, 18);
        this.slotGrid = this.createSlotGrid();
    }

    private SlotGrid createSlotGrid() {
        return new SlotGrid(SlotMaps.SURVIVAL_INVENTORY)
                .grid(
                        SlotGroup.PLAYER_ARMOR,
                        this.leftPos + 46,
                        this.topPos + 7,
                        1,
                        18,
                        18,
                        18,
                        18,
                        16
                )
                .point(
                        SlotGroup.PLAYER_OFFHAND,
                        0,
                        this.leftPos + 115,
                        this.topPos + 61,
                        18,
                        18,
                        16
                )
                .grid(
                        SlotGroup.PLAYER_INVENTORY,
                        this.leftPos + 7,
                        this.topPos + 83,
                        9,
                        18,
                        18,
                        18,
                        18,
                        16
                )
                .grid(
                        SlotGroup.PLAYER_HOTBAR,
                        this.leftPos + 7,
                        this.topPos + 141,
                        9,
                        18,
                        18,
                        18,
                        18,
                        16
                );
    }

    @Override
    public SlotGrid slotGrid() {
        return this.slotGrid;
    }

    @Override
    public VanillaSlotLayer slotLayer() {
        return this.slotLayer;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        Draw.imageFromMeta(BACKGROUND, this.leftPos, this.topPos, this.imageWidth, this.imageHeight)
                .draw(graphics);

        if (this.minecraft != null && this.minecraft.player != null) {
            net.minecraft.client.gui.screens.inventory.InventoryScreen.extractEntityInInventoryFollowsMouse(
                    graphics,
                    this.leftPos + 64,
                    this.topPos + 7,
                    this.leftPos + 113,
                    this.topPos + 77,
                    30,
                    0.0625F,
                    mouseX,
                    mouseY,
                    this.minecraft.player
            );
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        for (GuiEventListener child : this.children()) {
            if (child instanceof Renderable renderable) {
                renderable.extractRenderState(graphics, mouseX, mouseY, partialTick);
            }
        }
        this.slotLayer.updateHovered(this.slotGrid, mouseX, mouseY);
        this.slotLayer.renderSlotMap(graphics, this.slotGrid);
        this.slotLayer.renderHoveredSlot(graphics, this.slotGrid);
        this.slotLayer.renderCarriedItem(graphics, mouseX, mouseY);
        this.slotLayer.renderTooltip(graphics, this.slotGrid, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        GuiEventListener child = this.getChildAt(event.x(), event.y()).orElse(null);
        if (child != null) {
            boolean handled = child.mouseClicked(event, doubleClick);
            if (handled && child.shouldTakeFocusAfterInteraction()) {
                this.setFocused(child);
                if (event.button() == 0) {
                    this.setDragging(true);
                }
            }
            return true;
        }
        return this.slotLayer.mouseClicked(
                this.slotGrid,
                event.x(),
                event.y(),
                event.button(),
                true,
                this.leftPos,
                this.topPos,
                this.imageWidth,
                this.imageHeight
        );
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && this.isDragging()) {
            this.setDragging(false);
            GuiEventListener focused = this.getFocused();
            if (focused != null) {
                return focused.mouseReleased(event);
            }
        }
        return this.slotLayer.mouseReleased(
                this.slotGrid,
                event.x(),
                event.y(),
                event.button(),
                true,
                this.leftPos,
                this.topPos,
                this.imageWidth,
                this.imageHeight
        );
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        GuiEventListener focused = this.getFocused();
        if (focused != null && this.isDragging() && event.button() == 0) {
            return focused.mouseDragged(event, dragX, dragY);
        }
        return this.slotLayer.mouseDragged(
                this.slotGrid,
                event.x(),
                event.y(),
                event.button(),
                dragX,
                dragY
        );
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return this.slotLayer.keyPressed(this.slotGrid, event.key(), event.scancode())
                || super.keyPressed(event);
    }
}
