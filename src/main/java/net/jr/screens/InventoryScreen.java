package net.jr.screens;

import net.jr.api.client.render.Draw;
import net.jr.api.client.resource.Asset;
import net.jr.client.ui.container.slots.CustomItemSlotLayout;
import net.jr.client.ui.container.slots.ItemSlotGrid;
import net.jr.client.ui.container.slots.ItemSlotGroup;
import net.jr.client.ui.container.slots.ItemSlotMaps;
import net.jr.client.ui.container.slots.VanillaItemSlotLayer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
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
        implements CustomItemSlotLayout {
    private static final Asset BACKGROUND =
            Asset.MC("textures/gui/container/inventory.png");

    private VanillaItemSlotLayer itemSlotLayer;
    private ItemSlotGrid itemSlotGrid;
    private final EffectsInInventory effects;

    public InventoryScreen(InventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.effects = new EffectsInInventory(this);
    }

    @Override
    protected void init() {
        super.init();
        this.itemSlotLayer = new VanillaItemSlotLayer(this, this.menu, this.font, 18);
        this.itemSlotGrid = this.createItemSlotGrid();
    }

    private ItemSlotGrid createItemSlotGrid() {
        return new ItemSlotGrid(ItemSlotMaps.SURVIVAL_INVENTORY)
                .grid(
                        ItemSlotGroup.PLAYER_ARMOR,
                        this.leftPos + 46,
                        this.topPos + 7,
                        1,
                        18,
                        18,
                        16
                )
                .point(
                        ItemSlotGroup.PLAYER_OFFHAND,
                        0,
                        this.leftPos + 115,
                        this.topPos + 61,
                        18,
                        18,
                        16
                )
                .grid(
                        ItemSlotGroup.PLAYER_INVENTORY,
                        this.leftPos + 7,
                        this.topPos + 83,
                        9,
                        18,
                        18,
                        16
                )
                .grid(
                        ItemSlotGroup.PLAYER_HOTBAR,
                        this.leftPos + 7,
                        this.topPos + 141,
                        9,
                        18,
                        18,
                        16
                );
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        for (GuiEventListener child : this.children()) {
            if (child instanceof Renderable renderable) {
                renderable.extractRenderState(graphics, mouseX, mouseY, partialTick);
            }
        }
        this.effects.extractRenderState(graphics,mouseX,mouseY);
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
                if (event.button() == 0) {
                    this.setDragging(true);
                }
            }
            return true;
        }
        return this.itemSlotLayer.mouseClicked(
                this.itemSlotGrid,
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
        return this.itemSlotLayer.mouseReleased(
                this.itemSlotGrid,
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
        return this.itemSlotLayer.mouseDragged(
                this.itemSlotGrid,
                event.x(),
                event.y(),
                event.button(),
                dragX,
                dragY
        );
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return this.itemSlotLayer.keyPressed(this.itemSlotGrid, event.key(), event.scancode())
                || super.keyPressed(event);
    }
}
