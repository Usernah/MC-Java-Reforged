package net.jr.screens.inventory;

import net.jr.api.client.render.Draw;
import net.jr.api.client.local.LocalClients;
import net.jr.api.client.resource.Asset;
import net.jr.api.client.ui.Cursor;
import net.jr.client.components.navigation.UiAction;
import net.jr.client.components.navigation.UiActionHandler;
import net.jr.client.components.elements.TextElement;
import net.jr.client.components.widgets.EditBoxElement;
import net.jr.client.components.widgets.TabBarElement;
import net.jr.client.components.widgets.TabElement;
import net.jr.client.components.widgets.item.CreativeItemGridElement;
import net.jr.client.components.widgets.item.CreativeItemSlotElement;
import net.jr.client.ui.navigation.UiInputBinding;
import net.jr.client.ui.cursor.CursorMagnetTarget;
import net.jr.client.input.gamepad.GamepadDigitalInput;
import net.jr.client.runtime.input.LocalGamepadInputHandler;
import net.jr.client.sound.action.InputActionSounds;
import net.jr.client.ui.container.slots.CustomSlotLayout;
import net.jr.client.ui.container.actions.CreativeCatalogActionHost;
import net.jr.client.ui.container.slots.SlotGrid;
import net.jr.client.ui.container.slots.SlotGroup;
import net.jr.client.ui.container.slots.SlotMaps;
import net.jr.client.ui.container.slots.VanillaSlotLayer;
import net.jr.client.ui.container.actions.ContainerSlotFocusController;
import net.jr.client.ui.navigation.FocusCursorVisibilityHost;
import net.jr.client.ui.navigation.UiInputModeController;
import net.jr.client.ui.navigation.tabs.TabNavigationController;
import net.jr.client.ui.navigation.tabs.TabNavigationHost;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CreativeInventoryScreen extends AbstractContainerScreen<InventoryMenu>
        implements UiActionHandler, CustomSlotLayout, TabNavigationHost, CreativeCatalogActionHost,
        FocusCursorVisibilityHost {
    private static final int TAB_WIDTH = 32;
    private static final int TAB_HEIGHT = 27;
    private static final int TAB_Y_OFFSET = -23;
    private static final int TAB_ICON_SIZE = 18;
    private static final int TAB_ICON_OFFSET_X = 7;
    private static final int TAB_ICON_OFFSET_Y = 4;

    private static final int ITEM_GRID_X = 10;
    private static final int ITEM_GRID_Y = 22;
    private static final int ITEM_COLUMNS = 12;
    private static final int ITEM_ROWS = 6;
    private static final int SLOT_FRAME_SIZE = 18;
    private static final int ITEM_RENDER_SIZE = 16;
    private static final int HOTBAR_X = 37;
    private static final int HOTBAR_Y = 137;
    private static final float TAB_TITLE_X = 249.0F / 2.0F;
    private static final int TAB_TITLE_Y = 7;
    private static final int SEARCH_BOX_X = 68;
    private static final int SEARCH_BOX_Y = 5;
    private static final int SEARCH_BOX_WIDTH = 114;
    private static final int SEARCH_BOX_HEIGHT = 13;

    private static final Asset BACKGROUND =
            Asset.MC("textures/gui/container/creative_inventory.png");
    private static final Asset TAB_LEFT =
            Asset.MC("textures/gui/sprites/container/creative_inventory/tab_left.png");
    private static final Asset TAB_LEFT_SELECTED =
            Asset.MC("textures/gui/sprites/container/creative_inventory/tab_left_selected.png");
    private static final Asset TAB_CENTER =
            Asset.MC("textures/gui/sprites/container/creative_inventory/tab_center.png");
    private static final Asset TAB_CENTER_SELECTED =
            Asset.MC("textures/gui/sprites/container/creative_inventory/tab_center_selected.png");
    private static final Asset TAB_RIGHT =
            Asset.MC("textures/gui/sprites/container/creative_inventory/tab_right.png");
    private static final Asset TAB_RIGHT_SELECTED =
            Asset.MC("textures/gui/sprites/container/creative_inventory/tab_right_selected.png");

    private static final Asset SCROLLER =
            Asset.MC("textures/gui/sprites/container/creative_inventory/scroller.png");
    private static final Asset SCROLLER_DISABLED =
            Asset.MC("textures/gui/sprites/container/creative_inventory/scroller_disabled.png");
    private static final Asset SEARCH_BOX =
            Asset.MC("textures/gui/sprites/widgets/text_edit_box.png");

    private final TabNavigationController tabNavigation = new TabNavigationController();
    private final Map<TabElement, CreativeCategory> categoryByTab = new IdentityHashMap<>();
    private final LocalPlayer player;
    private final int slotId;

    private VanillaSlotLayer slotLayer;
    private SlotGrid slotGrid;
    private CreativeItemGridElement itemGrid;
    private TabBarElement tabs;
    private TextElement tabTitleText;
    private EditBoxElement searchEditBox;
    private CreativeCategory selectedCategory = CreativeCategory.CONSTRUCTION_AND_COLORS;
    private FocusRegion focusRegion = FocusRegion.CATALOG;
    private int pendingCatalogReturnIndex = -1;

    public CreativeInventoryScreen(LocalPlayer player, Component title) {
        super(player.inventoryMenu, player.getInventory(), title, 249, 164);
        this.player = player;
        this.slotId = LocalClients.slotId(player);

        player.containerMenu = player.inventoryMenu;
    }

    @Override
    protected void init() {
        super.init();

        this.topPos += 12;

        this.rebuildCreativeTabContents();
        this.slotLayer = new VanillaSlotLayer(this, this.menu, this.font, SLOT_FRAME_SIZE);
        this.slotGrid = new SlotGrid(SlotMaps.SURVIVAL_INVENTORY)
                .grid(
                        SlotGroup.PLAYER_HOTBAR,
                        this.leftPos + HOTBAR_X,
                        this.topPos + HOTBAR_Y,
                        9,
                        SLOT_FRAME_SIZE,
                        SLOT_FRAME_SIZE,
                        SLOT_FRAME_SIZE,
                        SLOT_FRAME_SIZE,
                        ITEM_RENDER_SIZE
                );

        this.itemGrid = new CreativeItemGridElement(
                this.leftPos + ITEM_GRID_X,
                this.topPos + ITEM_GRID_Y,
                ITEM_COLUMNS * SLOT_FRAME_SIZE,
                ITEM_ROWS * SLOT_FRAME_SIZE,
                ITEM_ROWS,
                ITEM_COLUMNS
        );
        this.itemGrid.setCellSize(SLOT_FRAME_SIZE, SLOT_FRAME_SIZE);
        this.itemGrid.onSelection(this::selectCreativeItem);
        this.itemGrid.onFocusedPrimarySelection(stack -> this.moveCreativeItemToHotbar(stack, 1));
        this.itemGrid.onFocusedAlternateSelection(stack ->
                this.moveCreativeItemToHotbar(stack, stack.getMaxStackSize()));
        this.configureItemScrollbar();

        this.tabs = this.createTabs();
        this.createTabTitle();
        this.createSearchBox();
        this.tabNavigation.clear();
        this.tabNavigation.bind(
                this.tabs,
                UiInputBinding.gamepad(GamepadDigitalInput.BUMPER_LEFT),
                UiInputBinding.gamepad(GamepadDigitalInput.BUMPER_RIGHT),
                Component.translatable("hint.container.java_reforged.change_tab"),
                InputActionSounds::playFocus
        );

        this.addRenderableWidget(this.itemGrid);
        this.addRenderableWidget(this.tabs);
        this.addRenderableWidget(this.searchEditBox);
        this.reloadItems();
        this.focusFirstCatalogEntryAndMoveCursor();
    }

    private void rebuildCreativeTabContents() {
        if (this.player.connection == null) {
            return;
        }
        CreativeModeTabs.tryRebuildTabContents(
                this.player.connection.enabledFeatures(),
                this.player.canUseGameMasterBlocks(),
                this.player.level().registryAccess()
        );
    }

    private TabBarElement createTabs() {
        TabBarElement bar = new TabBarElement(
                this.leftPos,
                this.topPos + TAB_Y_OFFSET,
                TAB_WIDTH,
                TAB_HEIGHT
        );
        bar.setOrientation(TabBarElement.Orientation.HORIZONTAL);

        bar.setPadding(-1.0F);

        for (CreativeCategory category : CreativeCategory.values()) {
            TabElement tab = new TabElement(0, 0, TAB_WIDTH, TAB_HEIGHT);
            tab.setRenderer((graphics, widget, mouseX, mouseY, partialTick) ->
                    this.renderTab(graphics, (TabElement)widget, category.icon())
            );
            this.categoryByTab.put(tab, category);
            bar.addTab(tab);
        }
        bar.onSelectionChange(tab -> {
            CreativeCategory category = this.categoryByTab.get(tab);
            if (category != null) {
                boolean refocusCatalog = this.shouldRefocusCatalogAfterTabChange();
                this.selectedCategory = category;
                this.reloadItems();
                if (refocusCatalog) {
                    this.focusFirstCatalogEntryAndMoveCursor();
                } else if (this.getFocused() == this.itemGrid) {
                    this.setFocused(null);
                }
                this.setSearchBoxVisible(category == CreativeCategory.SEARCH);
            }
        });
        return bar;
    }

    private void renderTab(GuiGraphicsExtractor graphics, TabElement tab, ItemStack icon) {
        Asset background = switch (tab.positionRole()) {
            case FIRST -> tab.isSelected() ? TAB_LEFT_SELECTED : TAB_LEFT;
            case LAST -> tab.isSelected() ? TAB_RIGHT_SELECTED : TAB_RIGHT;
            case MIDDLE, SINGLE -> tab.isSelected() ? TAB_CENTER_SELECTED : TAB_CENTER;
        };
        Draw.imageFromMeta(background, tab.x(), tab.y(), TAB_WIDTH, TAB_HEIGHT).draw(graphics);

        graphics.pose().pushMatrix();
        graphics.pose().translate(tab.x() + TAB_ICON_OFFSET_X, tab.y() + TAB_ICON_OFFSET_Y);
        float iconScale = TAB_ICON_SIZE / 16.0F;
        graphics.pose().scale(iconScale, iconScale);
        graphics.item(icon, 0, 0);
        graphics.pose().popMatrix();
    }

    private void configureItemScrollbar() {
        this.itemGrid.setAutoPlaceScrollbar(false);
        this.itemGrid.setFixedScrollbarHandleSize(15.0F);
        this.itemGrid.scrollbar().setX(this.leftPos + 231);
        this.itemGrid.scrollbar().setY(this.topPos + 22);
        this.itemGrid.scrollbar().setSize(9, 110);
        this.itemGrid.scrollbar().setRenderer((graphics, ignored, mouseX, mouseY, partialTick) -> {
            boolean scrollable = this.itemGrid.hasScrollableOverflow();
            this.itemGrid.scrollbar().setActive(scrollable);
            Draw.image(
                    scrollable ? SCROLLER : SCROLLER_DISABLED,
                    Math.round(this.itemGrid.scrollbar().handleX()),
                    Math.round(this.itemGrid.scrollbar().handleY()),
                    9,
                    15
            ).draw(graphics);
        });
    }

    private void createTabTitle() {
        this.tabTitleText = new TextElement(
                this.selectedCategory.title(),
                this.leftPos + TAB_TITLE_X,
                this.topPos + TAB_TITLE_Y,
                null
        );
        this.tabTitleText.setAlign(Draw.TextAlign.CENTER);
        this.tabTitleText.setScale(1.0F);
        this.tabTitleText.setColor(0xFF404040);
    }

    private void createSearchBox() {
        this.searchEditBox = new EditBoxElement(
                this.leftPos + SEARCH_BOX_X,
                this.topPos + SEARCH_BOX_Y,
                SEARCH_BOX_WIDTH,
                SEARCH_BOX_HEIGHT,
                Component.translatable("creative_tab.java_reforged.search_placeholder")
        );
        this.searchEditBox.setFont(this.font);
        this.searchEditBox.setHint(
                Component.translatable("creative_tab.java_reforged.search_placeholder")
        );
        this.searchEditBox.setMaxLength(64);
        this.searchEditBox.setRenderer(this::renderSearchBox);
        this.searchEditBox.setResponder(ignored -> {
            if (this.selectedCategory == CreativeCategory.SEARCH) {
                this.reloadItems();
            }
        });
        this.setSearchBoxVisible(false);
    }

    private void renderSearchBox(
            GuiGraphicsExtractor graphics,
            AbstractWidget widget,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        EditBoxElement box = (EditBoxElement)widget;
        Draw.image(SEARCH_BOX, box.getX(), box.getY(), box.getWidth(), box.getHeight()).draw(graphics);

        boolean empty = box.value().isEmpty();
        String text = empty ? box.hint().getString() : box.visibleText(132);
        Draw.text(Component.literal(text), box.getX() + 4, box.getY() + 3)
                .scale(0.8F)
                .color(empty ? 0xFF808080 : 0xFFE0E0E0)
                .draw(graphics);

        if (box.isCursorVisible()) {
            int cursorWidth = this.font.width(box.value().substring(0, box.cursorPosition()));
            int cursorX = box.getX() + 4 + Math.min(105, Math.round(cursorWidth * 0.8F));
            Draw.text(Component.literal("|"), cursorX, box.getY() + 3)
                    .scale(0.8F)
                    .color(0xFFFFFFFF)
                    .draw(graphics);
        }
    }

    private void setSearchBoxVisible(boolean visible) {
        if (this.searchEditBox == null) {
            return;
        }
        this.searchEditBox.setVisible(visible);
        this.searchEditBox.active = visible;
        if (!visible) {
            if (this.getFocused() == this.searchEditBox) {
                this.setFocused(null);
            }
            this.searchEditBox.setFocused(false);
        }
    }

    private void reloadItems() {
        if (this.itemGrid == null) {
            return;
        }
        this.pendingCatalogReturnIndex = -1;
        this.itemGrid.scrollToImmediately(0.0D, 0.0D);
        this.itemGrid.clearEntryFocus();
        this.itemGrid.setEntries(this.collectItems(this.selectedCategory));
    }

    private List<ItemStack> collectItems(CreativeCategory category) {
        if (category == CreativeCategory.SEARCH) {
            String query = this.searchEditBox == null
                    ? ""
                    : this.searchEditBox.value().trim().toLowerCase(Locale.ROOT);
            return CreativeModeTabs.searchTab().getDisplayItems().stream()
                    .filter(stack -> this.matchesSearch(stack, query))
                    .toList();
        }

        List<ItemStack> items = new ArrayList<>();
        for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
            if (tab.getType() != CreativeModeTab.Type.CATEGORY) {
                continue;
            }
            Identifier id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            String path = id == null ? "" : id.getPath();
            for (ItemStack stack : tab.getDisplayItems()) {
                if (category.accepts(path, stack) && !containsEquivalent(items, stack)) {
                    items.add(stack);
                }
            }
        }
        return List.copyOf(items);
    }

    private boolean matchesSearch(ItemStack stack, String query) {
        if (query.isEmpty()) {
            return true;
        }
        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return name.contains(query)
                || (id != null && id.toString().toLowerCase(Locale.ROOT).contains(query));
    }

    private static boolean containsEquivalent(List<ItemStack> values, ItemStack candidate) {
        for (ItemStack value : values) {
            if (ItemStack.isSameItemSameComponents(value, candidate)) {
                return true;
            }
        }
        return false;
    }

    private void selectCreativeItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        if (!this.menu.getCarried().isEmpty()) {
            this.menu.setCarried(ItemStack.EMPTY);
            return;
        }
        this.menu.setCarried(stack.copyWithCount(stack.getMaxStackSize()));
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ContainerInput input) {
        if (this.minecraft == null || this.minecraft.player == null || this.minecraft.gameMode == null) {
            return;
        }

        if (this.isCreativeHotbarSlot(slot)) {
            if (!this.menu.getCarried().isEmpty() && input == ContainerInput.PICKUP) {
                this.replaceCreativeHotbarSlot(slot);
                return;
            }
            if (input == ContainerInput.QUICK_MOVE) {
                if (this.menu.getCarried().isEmpty()) {
                    this.clearCreativeHotbarSlot(slot);
                } else {
                    this.replaceCreativeHotbarSlot(slot);
                }
                return;
            }
        }

        List<ItemStack> before = this.snapshotCreativeHotbar();
        this.menu.clicked(slotId, mouseButton, input, this.minecraft.player);
        this.player.inventoryMenu.broadcastChanges();
        this.syncCreativeHotbarChanges(before);
    }

    @Override
    public boolean handleCreativeQuickMove() {
        return switch (this.creativeCatalogTarget()) {
            case CATALOG_ITEM -> this.moveCatalogItemToHotbar(this.actionCatalogItem());
            case HOTBAR_ITEM -> this.menu.getCarried().isEmpty()
                    ? this.clearCreativeHotbarSlot(this.actionHotbarSlot())
                    : this.replaceCreativeHotbarSlot(this.actionHotbarSlot());
            case NONE -> false;
        };
    }

    @Override
    public CreativeCatalogActionHost.Target creativeCatalogTarget() {
        CreativeItemSlotElement catalogItem = this.actionCatalogItem();
        if (catalogItem != null && !catalogItem.stack().isEmpty()) {
            return CreativeCatalogActionHost.Target.CATALOG_ITEM;
        }
        Slot hotbarSlot = this.actionHotbarSlot();
        return hotbarSlot != null && (hotbarSlot.hasItem() || !this.menu.getCarried().isEmpty())
                ? CreativeCatalogActionHost.Target.HOTBAR_ITEM
                : CreativeCatalogActionHost.Target.NONE;
    }

    private boolean moveCatalogItemToHotbar(CreativeItemSlotElement catalogItem) {
        return catalogItem != null && this.moveCreativeItemToHotbar(
                catalogItem.stack(),
                catalogItem.stack().getMaxStackSize()
        );
    }

    private boolean moveCreativeItemToHotbar(ItemStack catalogStack, int count) {
        if (catalogStack.isEmpty()) {
            return false;
        }

        boolean fromFocus = UiInputModeController.isFocusNavigationActive();

        if (!this.menu.getCarried().isEmpty()) {
            this.menu.setCarried(ItemStack.EMPTY);
            this.pendingCatalogReturnIndex = -1;
            return true;
        }

        ItemStack stack = catalogStack.copyWithCount(Math.min(count, catalogStack.getMaxStackSize()));
        Slot emptyHotbarSlot = this.firstEmptyCreativeHotbarSlot();
        if (emptyHotbarSlot == null) {
            this.menu.setCarried(stack);
            if (fromFocus) {
                this.pendingCatalogReturnIndex = this.itemGrid == null
                        ? -1
                        : this.itemGrid.focusedEntryIndex();
                this.focusHotbarSlotAndMoveCursor(this.firstCreativeHotbarSlot());
            } else {
                this.pendingCatalogReturnIndex = -1;
            }
            return true;
        }

        emptyHotbarSlot.setByPlayer(stack);
        this.player.inventoryMenu.broadcastChanges();
        this.sendCreativeHotbarSlot(emptyHotbarSlot);
        return true;
    }

    private boolean replaceCreativeHotbarSlot(Slot slot) {
        if (!this.isCreativeHotbarSlot(slot) || this.menu.getCarried().isEmpty()) {
            return false;
        }
        slot.setByPlayer(this.menu.getCarried().copy());
        this.menu.setCarried(ItemStack.EMPTY);
        this.player.inventoryMenu.broadcastChanges();
        this.sendCreativeHotbarSlot(slot);
        this.restorePendingCatalogFocus();
        return true;
    }

    private boolean clearCreativeHotbarSlot(Slot slot) {
        if (slot == null || !slot.hasItem()) {
            return false;
        }
        slot.setByPlayer(ItemStack.EMPTY);
        this.player.inventoryMenu.broadcastChanges();
        this.sendCreativeHotbarSlot(slot);
        return true;
    }

    private CreativeItemSlotElement focusedCatalogItem() {
        if (this.itemGrid == null || !(this.itemGrid.getFocused() instanceof CreativeItemSlotElement slot)) {
            return null;
        }
        return slot;
    }

    private CreativeItemSlotElement actionCatalogItem() {
        if (UiInputModeController.isFocusNavigationActive()) {
            return this.focusRegion == FocusRegion.CATALOG ? this.focusedCatalogItem() : null;
        }
        return this.itemGrid == null
                ? null
                : this.itemGrid.previewedSlot(
                        LocalGamepadInputHandler.cursorX(),
                        LocalGamepadInputHandler.cursorY(),
                        false
                );
    }

    private Slot focusedCreativeHotbarSlot() {
        if (this.focusRegion != FocusRegion.HOTBAR || this.slotLayer == null) {
            return null;
        }
        Slot slot = this.slotLayer.getFocusedSlot();
        return slot != null && this.slotGrid != null && this.slotGrid.has(slot.index) ? slot : null;
    }

    private Slot actionHotbarSlot() {
        if (UiInputModeController.isFocusNavigationActive()) {
            return this.focusedCreativeHotbarSlot();
        }
        return this.slotLayer == null || this.slotGrid == null
                ? null
                : this.slotLayer.getSlotAt(
                        this.slotGrid,
                        LocalGamepadInputHandler.cursorX(),
                        LocalGamepadInputHandler.cursorY()
                );
    }

    private Slot firstEmptyCreativeHotbarSlot() {
        if (this.slotGrid == null) {
            return null;
        }
        for (int index = 0; index < 9; index++) {
            Slot slot = this.menu.slots.get(36 + index);
            if (this.slotGrid.has(slot.index) && slot.isActive() && slot.getItem().isEmpty()) {
                return slot;
            }
        }
        return null;
    }

    private Slot firstCreativeHotbarSlot() {
        if (this.slotGrid == null) {
            return null;
        }
        for (int index = 0; index < 9; index++) {
            Slot slot = this.menu.slots.get(36 + index);
            if (this.slotGrid.has(slot.index) && slot.isActive()) {
                return slot;
            }
        }
        return null;
    }

    private boolean isCreativeHotbarSlot(Slot slot) {
        return slot != null && this.slotGrid != null && this.slotGrid.has(slot.index) && slot.isActive();
    }

    private void focusHotbarSlotAndMoveCursor(Slot slot) {
        if (!this.isCreativeHotbarSlot(slot)) {
            return;
        }
        this.enterHotbarFocus();
        this.slotLayer.setFocusedSlot(slot);
        var position = this.slotGrid.get(slot.index);
        Cursor.moveTo(
                this.slotId,
                position.x() + position.width() / 2.0D,
                position.y() + position.height() / 2.0D
        );
    }

    private void restorePendingCatalogFocus() {
        int returnIndex = this.pendingCatalogReturnIndex;
        this.pendingCatalogReturnIndex = -1;
        if (returnIndex < 0
                || !UiInputModeController.isFocusNavigationActive()
                || this.itemGrid == null) {
            return;
        }
        this.focusRegion = FocusRegion.CATALOG;
        if (this.slotLayer != null) {
            this.slotLayer.clearFocusedSlot();
        }
        this.setFocused(this.itemGrid);
        if (!this.itemGrid.focusEntry(returnIndex)) {
            return;
        }
        var rectangle = this.itemGrid.getFocused().getRectangle();
        Cursor.moveTo(
                this.slotId,
                rectangle.left() + rectangle.width() / 2.0D,
                rectangle.top() + rectangle.height() / 2.0D
        );
    }

    private List<ItemStack> snapshotCreativeHotbar() {
        List<ItemStack> stacks = new ArrayList<>(9);
        for (int index = 0; index < 9; index++) {
            stacks.add(this.menu.slots.get(36 + index).getItem().copy());
        }
        return stacks;
    }

    private void syncCreativeHotbarChanges(List<ItemStack> before) {
        for (int index = 0; index < 9; index++) {
            Slot slot = this.menu.slots.get(36 + index);
            ItemStack previous = index < before.size() ? before.get(index) : ItemStack.EMPTY;
            if (!ItemStack.matches(previous, slot.getItem())) {
                this.sendCreativeHotbarSlot(slot);
            }
        }
    }

    private void sendCreativeHotbarSlot(Slot slot) {
        if (slot == null || this.minecraft == null || this.minecraft.gameMode == null) {
            return;
        }

        this.minecraft.gameMode.handleCreativeModeItemAdd(slot.getItem(), slot.index);
    }

    @Override
    public SlotGrid slotGrid() {
        return this.slotGrid;
    }

    @Override
    public CursorMagnetTarget cursorMagnetTargetAt(double guiX, double guiY) {
        CursorMagnetTarget catalogTarget = this.itemGrid == null
                ? null
                : this.itemGrid.cursorMagnetTargetAt(guiX, guiY);
        return catalogTarget != null
                ? catalogTarget
                : CustomSlotLayout.super.cursorMagnetTargetAt(guiX, guiY);
    }

    @Override
    public VanillaSlotLayer slotLayer() {
        return this.slotLayer;
    }

    @Override
    public boolean allowsSlotFocusNavigation() {
        return this.focusRegion == FocusRegion.HOTBAR;
    }

    @Override
    public boolean seedFocusFromPointer(double guiX, double guiY) {
        CreativeItemSlotElement catalogItem = this.itemGrid == null
                ? null
                : this.itemGrid.previewedSlot(guiX, guiY, false);
        if (catalogItem != null) {
            this.focusRegion = FocusRegion.CATALOG;
            if (this.slotLayer != null) {
                this.slotLayer.clearFocusedSlot();
            }
            this.setFocused(this.itemGrid);
            this.itemGrid.setFocused(catalogItem);
            return true;
        }

        Slot hotbarSlot = this.slotLayer == null || this.slotGrid == null
                ? null
                : this.slotLayer.getSlotAt(this.slotGrid, guiX, guiY);
        if (hotbarSlot == null) {
            return this.assignFirstCatalogFocusAndMoveCursor();
        }

        this.enterHotbarFocus();
        this.slotLayer.setFocusedSlot(hotbarSlot);
        return true;
    }

    @Override
    public void clearFocusForPointer() {
        this.focusRegion = FocusRegion.NONE;
        if (this.itemGrid != null) {
            this.itemGrid.clearEntryFocus();
        }
        if (this.slotLayer != null) {
            this.slotLayer.clearFocusedSlot();
        }
    }

    @Override
    public boolean shouldShowCursorDuringFocus() {
        return this.focusRegion == FocusRegion.CATALOG;
    }

    @Override
    public boolean handleSlotNavigationBoundary(int keyCode) {
        if (this.focusRegion == FocusRegion.HOTBAR && keyCode == GLFW.GLFW_KEY_UP) {
            this.enterCatalogFocus(true);
            return true;
        }
        return false;
    }

    @Override
    public TabNavigationController tabNavigationController() {
        return this.tabNavigation;
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
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        this.tabTitleText.setText(this.selectedCategory.title());
        if (this.selectedCategory != CreativeCategory.SEARCH) {
            this.tabTitleText.draw(graphics);
        }
        this.itemGrid.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.tabs.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.searchEditBox.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.slotLayer.updateHovered(this.slotGrid, mouseX, mouseY);
        this.slotLayer.renderSlotMap(graphics, this.slotGrid);
        this.slotLayer.renderHoveredSlot(graphics, this.slotGrid);
        this.slotLayer.renderCarriedItem(graphics, mouseX, mouseY);
        this.slotLayer.renderTooltip(graphics, this.slotGrid, mouseX, mouseY);

        if (this.menu.getCarried().isEmpty()) {
            CreativeItemSlotElement slot = this.itemGrid.previewedSlot(
                    mouseX,
                    mouseY,
                    this.focusRegion == FocusRegion.CATALOG
            );
            if (slot != null && !slot.stack().isEmpty()) {
                boolean pointerOverSlot = slot.isMouseOver(mouseX, mouseY);
                int tooltipX = pointerOverSlot ? mouseX : slot.getX() + slot.getWidth() / 2;
                int tooltipY = pointerOverSlot ? mouseY : slot.getY() + slot.getHeight() / 2;
                graphics.setTooltipForNextFrame(this.font, slot.stack(), tooltipX, tooltipY);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.searchEditBox.mouseClicked(event, doubleClick)) {
            this.setFocused(this.searchEditBox);
            return true;
        }
        if (this.tabs.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            CreativeItemSlotElement catalogItem = this.itemGrid.previewedSlot(
                    event.x(),
                    event.y(),
                    false
            );
            if (catalogItem != null) {
                return this.moveCatalogItemToHotbar(catalogItem);
            }
        }
        if (this.itemGrid.mouseClicked(event, doubleClick)) {
            this.enterCatalogFocus(false);
            return true;
        }
        boolean handled = this.slotLayer.mouseClicked(
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
        if (handled) {
            this.enterHotbarFocus();
        }
        return handled;
    }

    @Override
    public void removed() {
        this.menu.setCarried(ItemStack.EMPTY);
        this.pendingCatalogReturnIndex = -1;
        super.removed();
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return this.searchEditBox.mouseReleased(event)
                || this.tabs.mouseReleased(event)
                || this.itemGrid.mouseReleased(event)
                || this.slotLayer.mouseReleased(
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
        return this.searchEditBox.mouseDragged(event, dragX, dragY)
                || this.itemGrid.mouseDragged(event, dragX, dragY)
                || this.slotLayer.mouseDragged(
                this.slotGrid,
                event.x(),
                event.y(),
                event.button(),
                dragX,
                dragY
        );
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return this.itemGrid.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
                || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.searchEditBox.keyPressed(event)) {
            return true;
        }
        UiAction navigation = switch (event.key()) {
            case GLFW.GLFW_KEY_LEFT -> UiAction.MOVE_LEFT;
            case GLFW.GLFW_KEY_RIGHT -> UiAction.MOVE_RIGHT;
            case GLFW.GLFW_KEY_UP -> UiAction.MOVE_UP;
            case GLFW.GLFW_KEY_DOWN -> UiAction.MOVE_DOWN;
            default -> null;
        };
        if (this.focusRegion == FocusRegion.CATALOG
                && navigation != null
                && this.handleUiAction(navigation)) {
            return true;
        }
        return this.slotLayer.keyPressed(this.slotGrid, event.key(), event.scancode())
                || super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return this.searchEditBox.charTyped(event) || super.charTyped(event);
    }

    @Override
    public boolean handleUiAction(UiAction action) {
        if (this.focusRegion != FocusRegion.CATALOG) {
            return false;
        }
        if (this.itemGrid.handleUiAction(action)) {
            return true;
        }
        if (action == UiAction.MOVE_DOWN) {
            this.enterHotbarFocus();
            return ContainerSlotFocusController.moveFocus(this, GLFW.GLFW_KEY_DOWN);
        }

        return action.isDirectional();
    }

    private void enterCatalogFocus(boolean focusLast) {
        this.focusRegion = FocusRegion.CATALOG;
        if (this.slotLayer != null) {
            this.slotLayer.clearFocusedSlot();
        }
        this.setFocused(this.itemGrid);
        if (this.itemGrid == null) {
            return;
        }
        if (focusLast) {
            this.itemGrid.focusLastEntry();
        } else if (this.itemGrid.getFocused() == null) {
            this.itemGrid.focusFirstEntry();
        } else {
            this.itemGrid.getFocused().setFocused(true);
        }
    }

    private boolean focusFirstCatalogEntryAndMoveCursor() {
        if (!this.assignFirstCatalogFocusAndMoveCursor()) {
            return false;
        }
        UiInputModeController.activateFocusNavigation();
        return true;
    }

    private boolean assignFirstCatalogFocusAndMoveCursor() {
        if (this.itemGrid == null || !this.itemGrid.focusFirstEntry()) {
            if (this.itemGrid != null) {
                this.itemGrid.clearEntryFocus();
            }
            if (this.getFocused() == this.itemGrid) {
                this.setFocused(null);
            }
            return false;
        }
        this.focusRegion = FocusRegion.CATALOG;
        if (this.slotLayer != null) {
            this.slotLayer.clearFocusedSlot();
        }
        this.setFocused(this.itemGrid);
        var rectangle = this.itemGrid.getFocused().getRectangle();
        Cursor.moveTo(
            this.slotId,
            rectangle.left() + rectangle.width() / 2.0D,
            rectangle.top() + rectangle.height() / 2.0D
        );
        return true;
    }

    private boolean shouldRefocusCatalogAfterTabChange() {
        if (this.itemGrid == null) {
            return false;
        }
        if (this.itemGrid.getFocused() != null) {
            return true;
        }
        double cursorX = Cursor.x(this.slotId);
        double cursorY = Cursor.y(this.slotId);
        return cursorX >= this.itemGrid.getX()
            && cursorY >= this.itemGrid.getY()
            && cursorX < this.itemGrid.getX() + this.itemGrid.getWidth()
            && cursorY < this.itemGrid.getY() + this.itemGrid.getHeight();
    }

    private void enterHotbarFocus() {
        this.focusRegion = FocusRegion.HOTBAR;
        if (this.itemGrid != null) {
            this.itemGrid.clearEntryFocus();
        }
        this.setFocused(null);
    }

    private enum CreativeCategory {
        CONSTRUCTION_AND_COLORS(Items.BRICKS),
        DECORATION_AND_FUNCTIONAL(Items.ITEM_FRAME),
        REDSTONE_AND_TRANSPORT(Items.REDSTONE),
        EQUIPMENT_AND_COMBAT(Items.DIAMOND_SWORD),
        FOOD_AND_DRINKS(Items.COOKED_BEEF),
        INGREDIENTS_AND_RESOURCES(Items.IRON_INGOT),
        SPAWN_EGGS(Items.PIG_SPAWN_EGG),
        SEARCH(Items.COMPASS);

        private final ItemStack icon;

        CreativeCategory(net.minecraft.world.item.Item icon) {
            this.icon = new ItemStack(icon);
        }

        private ItemStack icon() {
            return this.icon;
        }

        private Component title() {
            return Component.translatable(
                    "creative_tab.java_reforged." + this.name().toLowerCase(Locale.ROOT)
            );
        }

        private boolean accepts(String tabPath, ItemStack stack) {
            return switch (this) {
                case CONSTRUCTION_AND_COLORS ->
                        tabPath.equals("building_blocks")
                                || tabPath.equals("colored_blocks")
                                || (tabPath.equals("natural_blocks") && isSolidBlock(stack));
                case DECORATION_AND_FUNCTIONAL ->
                        tabPath.equals("functional_blocks")
                                || (tabPath.equals("natural_blocks") && !isSolidBlock(stack));
                case REDSTONE_AND_TRANSPORT -> tabPath.equals("redstone_blocks");
                case EQUIPMENT_AND_COMBAT ->
                        tabPath.equals("tools_and_utilities") || tabPath.equals("combat");
                case FOOD_AND_DRINKS -> tabPath.equals("food_and_drinks");
                case INGREDIENTS_AND_RESOURCES ->
                        tabPath.equals("ingredients") || isUnmappedCreativeCategory(tabPath);
                case SPAWN_EGGS -> tabPath.equals("spawn_eggs");
                case SEARCH -> false;
            };
        }

        private static boolean isSolidBlock(ItemStack stack) {
            return stack.getItem() instanceof BlockItem blockItem
                    && blockItem.getBlock().defaultBlockState().isSolid();
        }

        private static boolean isUnmappedCreativeCategory(String tabPath) {
            return !tabPath.equals("building_blocks")
                    && !tabPath.equals("colored_blocks")
                    && !tabPath.equals("natural_blocks")
                    && !tabPath.equals("functional_blocks")
                    && !tabPath.equals("redstone_blocks")
                    && !tabPath.equals("tools_and_utilities")
                    && !tabPath.equals("combat")
                    && !tabPath.equals("food_and_drinks")
                    && !tabPath.equals("ingredients")
                    && !tabPath.equals("spawn_eggs");
        }
    }

    private enum FocusRegion {
        NONE,
        CATALOG,
        HOTBAR
    }
}
