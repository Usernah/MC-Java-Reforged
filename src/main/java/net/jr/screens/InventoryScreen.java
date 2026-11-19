package net.jr.screens;

import net.jr.api.client.ui.DslContainer;
import net.jr.api.client.ui.UiContainerFile;
import net.jr.api.client.ui.UiDataValues;
import net.jr.client.ui.container.DslContainerSize;
import net.jr.client.ui.container.slots.CustomItemSlotLayout;
import net.jr.client.ui.container.slots.ItemSlotGrid;
import net.jr.client.ui.container.slots.VanillaItemSlotLayer;
import net.jr.registry.ModUi;
import net.jr.client.runtime.options.OptionsStateResolver;
import net.jr.client.runtime.state.CraftingMode;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;


public final class InventoryScreen extends AbstractContainerScreen<InventoryMenu>
        implements DslContainer, CustomItemSlotLayout {

    private VanillaItemSlotLayer itemSlotLayer;
    private ItemSlotGrid itemSlotGrid;
    private final EffectsInInventory effects;
    private final UiDataValues screenData;
    private final UiContainerFile containerFile;

    public InventoryScreen(InventoryMenu menu, Inventory inventory, Component title) {
        this(menu, inventory, title, resolveContainerFile());
    }

    private InventoryScreen(
            InventoryMenu menu,
            Inventory inventory,
            Component title,
            UiContainerFile containerFile
    ) {
        this(menu, inventory, title, containerFile, DslContainerSize.resolve(containerFile, 176, 166));
    }

    private InventoryScreen(
            InventoryMenu menu,
            Inventory inventory,
            Component title,
            UiContainerFile containerFile,
            DslContainerSize dimensions
    ) {
        super(menu, inventory, title, dimensions.width(), dimensions.height());
        this.containerFile = containerFile;
        this.effects = new EffectsInInventory(this);
        this.screenData = ModUi.SURIVAL_INVENTORY_DAT.readData();
    }

    @Override
    public UiContainerFile dslFile() {
        return this.containerFile;
    }

    @Override
    protected void init() {
        super.init();
        this.dslData().exposeText("inventory.title", () -> this.title.getString());
        this.dslData().exposeText(
                "inventory.inventory_title",
                () -> this.playerInventoryTitle.getString()
        );
        this.itemSlotGrid = this.dslItemSlotGrid("menu");
        if (this.itemSlotGrid == null) {
            throw new IllegalStateException(
                    "Inventory DSL must declare item_slots id 'menu'"
            );
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
    public void extractBackground(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        this.extractDslBackground(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.extractDslContent(graphics, mouseX, mouseY, partialTick);
        if (this.minecraft != null && this.minecraft.player != null) {
            net.minecraft.client.gui.screens.inventory.InventoryScreen.extractEntityInInventoryFollowsMouse(
                    graphics,
                    this.leftPos + this.screenData.integer("player_preview_left"),
                    this.topPos + this.screenData.integer("player_preview_top"),
                    this.leftPos + this.screenData.integer("player_preview_right"),
                    this.topPos + this.screenData.integer("player_preview_bottom"),
                    this.screenData.integer("player_preview_size"),
                    this.screenData.decimal("player_preview_scale"),
                    mouseX,
                    mouseY,
                    this.minecraft.player
            );
        }
        this.effects.extractRenderState(graphics, mouseX, mouseY);
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

    private static UiContainerFile resolveContainerFile() {
        return OptionsStateResolver.current().craftingMode().get() == CraftingMode.CLASSIC
                ? ModUi.CLASSIC_INVENTORY
                : ModUi.SURIVAL_INVENTORY;
    }
}
