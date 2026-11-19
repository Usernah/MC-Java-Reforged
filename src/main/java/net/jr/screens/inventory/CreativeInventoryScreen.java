package net.jr.screens.inventory;

import net.jr.api.client.render.Draw;
import net.jr.api.client.resource.Asset;
import net.jr.api.client.ui.Cursor;
import net.jr.api.client.ui.DslContainer;
import net.jr.api.client.ui.UiContainerFile;
import net.jr.api.client.ui.UiDataValues;
import net.jr.client.components.navigation.UiAction;
import net.jr.client.components.navigation.UiActionHandler;
import net.jr.client.components.navigation.TabNavigator;
import net.jr.client.components.widgets.EditBoxElement;
import net.jr.client.components.widgets.TabBarElement;
import net.jr.client.components.widgets.TabElement;
import net.jr.client.components.widgets.item.CreativeItemGridElement;
import net.jr.client.components.widgets.item.CreativeItemSlotElement;
import net.jr.client.components.widgets.item.CreativeItemSlotStyle;
import net.jr.client.ui.navigation.UiInputBinding;
import net.jr.client.ui.cursor.CursorMagnetTarget;
import net.jr.api.client.input.GamepadDigitalInput;
import net.jr.client.runtime.input.GamepadInputHandler;
import net.jr.client.ui.container.slots.CustomItemSlotLayout;
import net.jr.client.ui.container.DslContainerSize;
import net.jr.client.ui.container.actions.CreativeCatalogActionHost;
import net.jr.client.ui.container.slots.ItemSlotGrid;
import net.jr.client.ui.container.slots.VanillaItemSlotLayer;
import net.jr.client.ui.container.actions.ContainerItemSlotFocusController;
import net.jr.client.ui.navigation.FocusCursorVisibilityHost;
import net.jr.client.ui.navigation.UiInputModeController;
import net.jr.client.ui.navigation.tabs.TabNavigationController;
import net.jr.client.ui.navigation.tabs.TabNavigationHost;
import net.jr.experimental_features.gamepad_keyboard.GamepadTextInputFocus;
import net.jr.registry.ModUi;
import net.jr.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
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
import net.neoforged.neoforge.common.CreativeModeTabRegistry;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CreativeInventoryScreen extends AbstractContainerScreen<InventoryMenu>
        implements DslContainer, UiActionHandler, CustomItemSlotLayout, TabNavigationHost, CreativeCatalogActionHost,
        FocusCursorVisibilityHost {
    private final TabNavigationController tabNavigation = new TabNavigationController();
    private final TabNavigator tabPageNavigation = new TabNavigator() {
        @Override
        public boolean selectPrevious() {
            return CreativeInventoryScreen.this.changeTabPage(-1);
        }

        @Override
        public boolean selectNext() {
            return CreativeInventoryScreen.this.changeTabPage(1);
        }
    };
    private final Map<TabElement, CreativeTab> creativeTabByWidget = new IdentityHashMap<>();
    private final List<List<CreativeTab>> tabPages = new ArrayList<>();
    private final LocalPlayer player;
    private final UiDataValues screenData;

    private VanillaItemSlotLayer itemSlotLayer;
    private ItemSlotGrid itemSlotGrid;
    private CreativeItemGridElement itemGrid;
    private boolean slotPointerActive;
    private TabBarElement tabs;
    private Button previousPageButton;
    private Button nextPageButton;
    private EditBoxElement searchEditBox;
    private CreativeTab selectedCreativeTab = CreativeTab.category(CreativeCategory.CONSTRUCTION_AND_COLORS);
    private int tabPageIndex;
    private FocusRegion focusRegion = FocusRegion.CATALOG;
    private int pendingCatalogReturnIndex = -1;

    public CreativeInventoryScreen(LocalPlayer player, Component title) {
        this(player, title, DslContainerSize.resolve(ModUi.CREATIVE_INVENTORY, 249, 164));
    }

    private CreativeInventoryScreen(
            LocalPlayer player,
            Component title,
            DslContainerSize dimensions
    ) {
        super(player.inventoryMenu, player.getInventory(), title, dimensions.width(), dimensions.height());
        this.player = player;
        this.screenData = ModUi.CREATIVE_INVENTORY_DAT.readData();

        player.containerMenu = player.inventoryMenu;
    }

    @Override
    public UiContainerFile dslFile() {
        return ModUi.CREATIVE_INVENTORY;
    }

    @Override
    protected void init() {
        super.init();

        this.topPos += 12;

        this.rebuildCreativeTabContents();
        this.rebuildTabPages();
        this.exposeDslData();
        this.itemSlotGrid = this.dslItemSlotGrid("menu");
        if (this.itemSlotGrid == null) {
            throw new IllegalStateException("Creative inventory DSL must declare item_slots id 'menu'");
        }
        this.itemSlotLayer = this.itemSlotGrid.itemSlotLayer();

        this.itemGrid = new CreativeItemGridElement(
                this.leftPos + this.screenData.integer("item_grid_x"),
                this.topPos + this.screenData.integer("item_grid_y"),
                this.screenData.integer("item_columns") * this.screenData.integer("item_cell_size"),
                this.screenData.integer("item_rows") * this.screenData.integer("item_cell_size"),
                this.screenData.integer("item_rows"),
                this.screenData.integer("item_columns"),
                new CreativeItemSlotStyle(
                        this.screenData.asset("item_slot_asset"),
                        this.screenData.integer("item_padding")
                )
        );
        this.itemGrid.setCellSize(
                this.screenData.integer("item_cell_size"),
                this.screenData.integer("item_cell_size")
        );
        this.itemGrid.onSelection(this::selectCreativeItem);
        this.itemGrid.onFocusedPrimarySelection(stack -> this.moveCreativeItemToHotbar(stack, 1));
        this.itemGrid.onFocusedAlternateSelection(stack ->
                this.moveCreativeItemToHotbar(stack, stack.getMaxStackSize()));
        this.configureItemScrollbar();

        this.tabs = this.createTabs();
        this.createPageButtons();
        this.createSearchBox();
        this.tabNavigation.clear();
        this.tabNavigation.bind(
                this.tabs,
                UiInputBinding.gamepad(GamepadDigitalInput.BUMPER_LEFT),
                UiInputBinding.gamepad(GamepadDigitalInput.BUMPER_RIGHT),
                Component.translatable("hint.container.java_reforged.change_tab"),
                CreativeInventoryScreen::playTabSelectionSound
        );
        this.tabNavigation.bind(
                this.tabPageNavigation,
                UiInputBinding.gamepad(GamepadDigitalInput.TRIGGER_LEFT),
                UiInputBinding.gamepad(GamepadDigitalInput.TRIGGER_RIGHT),
                Component.translatable("hint.container.java_reforged.change_tab_page"),
                CreativeInventoryScreen::playTabSelectionSound
        );

        this.addRenderableWidget(this.itemGrid);
        this.addRenderableWidget(this.tabs);
        this.addRenderableWidget(this.previousPageButton);
        this.addRenderableWidget(this.nextPageButton);
        this.addRenderableWidget(this.searchEditBox);
        this.reloadItems();
        this.focusFirstCatalogEntryAndMoveCursor();
    }

    private void exposeDslData() {
        this.dslData().exposeText("creative.tab_title", () -> this.selectedCreativeTab.title().getString());
        this.dslData().exposeText("creative.page_label", () ->
                (this.tabPageIndex + 1) + " / " + this.tabPages.size());
        this.dslData().exposeNumber("creative.tab_color", () -> this.selectedCreativeTab.labelColor());
        this.dslData().exposeBoolean("creative.show_tab_title", () -> !this.selectedCreativeTab.hasSearchBar());
        this.dslData().exposeBoolean("creative.show_page_label", () -> this.tabPages.size() > 1);
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
                this.topPos + this.screenData.integer("tab_y_offset"),
                this.screenData.integer("tab_width"),
                this.screenData.integer("tab_height")
        );
        bar.setOrientation(TabBarElement.Orientation.HORIZONTAL);
        bar.setPositionRoleCapacity(Math.max(1, this.screenData.integer("tabs_per_page")));

        bar.setPadding(this.screenData.decimal("tab_padding"));
        bar.onSelectionChange(tab -> {
            CreativeTab creativeTab = this.creativeTabByWidget.get(tab);
            if (creativeTab != null) {
                this.prepareCreativeTabChange();
                this.selectedCreativeTab = creativeTab;
                this.reloadItems();
                this.focusCatalogEntryUnderCursor();
                this.setSearchBoxVisible(creativeTab.hasSearchBar());
            }
        });
        this.populateTabs(bar);
        return bar;
    }

    private void rebuildTabPages() {
        this.tabPages.clear();
        List<CreativeTab> vanillaTabs = new ArrayList<>(List.of(CreativeCategory.values()).stream()
                .filter(category -> category != CreativeCategory.SEARCH)
                .map(CreativeTab::category)
                .toList());
        vanillaTabs.add(CreativeTab.category(CreativeCategory.SEARCH));
        this.tabPages.add(List.copyOf(vanillaTabs));

        List<CreativeTab> modTabs = CreativeModeTabRegistry.getSortedCreativeModeTabs().stream()
                .filter(CreativeModeTab::shouldDisplay)
                .filter(CreativeModeTab::hasAnyItems)
                .filter(CreativeInventoryScreen::isModCreativeTab)
                .map(CreativeTab::registered)
                .toList();
        int modTabsPerPage = Math.max(1, this.screenData.integer("tabs_per_page") - 1);
        for (int start = 0; start < modTabs.size(); start += modTabsPerPage) {
            int end = Math.min(start + modTabsPerPage, modTabs.size());
            List<CreativeTab> page = new ArrayList<>(modTabs.subList(start, end));
            page.add(CreativeTab.category(CreativeCategory.SEARCH));
            this.tabPages.add(List.copyOf(page));
        }

        int selectedPage = this.findPage(this.selectedCreativeTab);
        if (this.selectedCreativeTab.category() == CreativeCategory.SEARCH) {
            this.tabPageIndex = Math.min(this.tabPageIndex, this.tabPages.size() - 1);
        } else if (selectedPage >= 0) {
            this.tabPageIndex = selectedPage;
        } else {
            this.tabPageIndex = Math.min(this.tabPageIndex, this.tabPages.size() - 1);
        }
    }

    private static boolean isModCreativeTab(CreativeModeTab tab) {
        Identifier id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
        return id != null && !"minecraft".equals(id.getNamespace());
    }

    private int findPage(CreativeTab creativeTab) {
        for (int page = 0; page < this.tabPages.size(); page++) {
            if (this.tabPages.get(page).contains(creativeTab)) {
                return page;
            }
        }
        return -1;
    }

    private void populateTabs(TabBarElement bar) {
        bar.clearTabs();
        this.creativeTabByWidget.clear();
        List<CreativeTab> page = this.tabPages.get(this.tabPageIndex);
        CreativeTab preferredTab = page.contains(this.selectedCreativeTab)
                ? this.selectedCreativeTab
                : page.getFirst();
        for (CreativeTab creativeTab : page) {
            TabElement tab = new TabElement(
                    0,
                    0,
                    this.screenData.integer("tab_width"),
                    this.screenData.integer("tab_height")
            );
            tab.setRenderer((graphics, widget, mouseX, mouseY, partialTick) ->
                    this.renderTab(graphics, (TabElement)widget, creativeTab.icon())
            );
            this.creativeTabByWidget.put(tab, creativeTab);
            bar.addTab(tab);
        }
        bar.select(page.indexOf(preferredTab));
    }

    private void createPageButtons() {
        int buttonSize = this.screenData.integer("page_button_size");
        int y = this.topPos + this.screenData.integer("page_button_y_offset");
        this.previousPageButton = Button.builder(Component.literal("<"), ignored -> this.changeTabPage(-1))
                .pos(this.leftPos, y)
                .size(buttonSize, buttonSize)
                .build();
        this.nextPageButton = Button.builder(Component.literal(">"), ignored -> this.changeTabPage(1))
                .pos(this.leftPos + this.imageWidth - buttonSize, y)
                .size(buttonSize, buttonSize)
                .build();
        this.updatePageButtons();
    }

    private boolean changeTabPage(int delta) {
        int nextPage = Math.max(0, Math.min(this.tabPageIndex + delta, this.tabPages.size() - 1));
        if (nextPage == this.tabPageIndex) {
            return false;
        }
        this.tabPageIndex = nextPage;
        this.populateTabs(this.tabs);
        this.updatePageButtons();
        return true;
    }

    private void updatePageButtons() {
        boolean hasMultiplePages = this.tabPages.size() > 1;
        this.previousPageButton.visible = hasMultiplePages;
        this.nextPageButton.visible = hasMultiplePages;
        this.previousPageButton.active = hasMultiplePages && this.tabPageIndex > 0;
        this.nextPageButton.active = hasMultiplePages && this.tabPageIndex + 1 < this.tabPages.size();
    }

    private void renderTab(GuiGraphicsExtractor graphics, TabElement tab, ItemStack icon) {
        boolean selected = tab.isSelected();
        Asset background = switch (tab.positionRole()) {
            case SINGLE, FIRST -> this.screenData.asset(
                    selected ? "tab_left_selected_asset" : "tab_left_asset");
            case MIDDLE -> this.screenData.asset(
                    selected ? "tab_center_selected_asset" : "tab_center_asset");
            case LAST -> this.screenData.asset(
                    selected ? "tab_right_selected_asset" : "tab_right_asset");
        };
        Draw.imageFromMeta(
                background,
                tab.x(),
                tab.y(),
                this.screenData.integer("tab_width"),
                this.screenData.integer("tab_height")
        ).draw(graphics);

        graphics.pose().pushMatrix();
        graphics.pose().translate(
                tab.x() + this.screenData.integer("tab_icon_x"),
                tab.y() + this.screenData.integer("tab_icon_y")
        );
        float iconScale = this.screenData.integer("tab_icon_size") / 16.0F;
        graphics.pose().scale(iconScale, iconScale);
        graphics.item(icon, 0, 0);
        graphics.pose().popMatrix();
    }

    private void configureItemScrollbar() {
        this.itemGrid.setAutoPlaceScrollbar(false);
        this.itemGrid.setFixedScrollbarHandleSize(this.screenData.decimal("scrollbar_handle_size"));
        this.itemGrid.scrollbar().setX(this.leftPos + this.screenData.integer("scrollbar_x"));
        this.itemGrid.scrollbar().setY(this.topPos + this.screenData.integer("scrollbar_y"));
        this.itemGrid.scrollbar().setSize(
                this.screenData.integer("scrollbar_width"),
                this.screenData.integer("scrollbar_height")
        );
        this.itemGrid.scrollbar().setRenderer((graphics, ignored, mouseX, mouseY, partialTick) -> {
            boolean scrollable = this.itemGrid.hasScrollableOverflow();
            this.itemGrid.scrollbar().setActive(scrollable);
            Draw.image(
                    this.screenData.asset(scrollable ? "scrollbar_asset" : "scrollbar_disabled_asset"),
                    Math.round(this.itemGrid.scrollbar().handleX()),
                    Math.round(this.itemGrid.scrollbar().handleY()),
                    this.screenData.integer("scrollbar_handle_width"),
                    this.screenData.integer("scrollbar_handle_height")
            ).draw(graphics);
        });
    }

    private void createSearchBox() {
        this.searchEditBox = new EditBoxElement(
                this.leftPos + this.screenData.integer("search_x"),
                this.topPos + this.screenData.integer("search_y"),
                this.screenData.integer("search_width"),
                this.screenData.integer("search_height"),
                Component.translatable("creative_tab.java_reforged.search_placeholder")
        );
        this.searchEditBox.setFont(this.font);
        this.searchEditBox.setHint(
                Component.translatable("creative_tab.java_reforged.search_placeholder")
        );
        this.searchEditBox.setMaxLength(this.screenData.integer("search_max_length"));
        this.searchEditBox.setRenderer(this::renderSearchBox);
        this.searchEditBox.setResponder(ignored -> {
            if (this.selectedCreativeTab.hasSearchBar()) {
                this.reloadItems();
            }
        });
        this.setSearchBoxVisible(this.selectedCreativeTab.hasSearchBar());
    }

    private void renderSearchBox(
            GuiGraphicsExtractor graphics,
            AbstractWidget widget,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        EditBoxElement box = (EditBoxElement)widget;
        Draw.image(
                this.screenData.asset("search_asset"),
                box.getX(),
                box.getY(),
                box.getWidth(),
                box.getHeight()
        ).draw(graphics);

        boolean empty = box.value().isEmpty();
        int textX = this.screenData.integer("search_text_x");
        int textY = this.screenData.integer("search_text_y");
        float textScale = this.screenData.decimal("search_text_scale");
        String text = empty
                ? box.hint().getString()
                : box.visibleText(this.screenData.integer("search_visible_text_width"));
        Draw.text(Component.literal(text), box.getX() + textX, box.getY() + textY)
                .scale(textScale)
                .color(empty
                        ? this.screenData.color("search_empty_color")
                        : this.screenData.color("search_text_color"))
                .draw(graphics);

        if (box.isCursorVisible()) {
            int cursorWidth = this.font.width(box.value().substring(0, box.cursorPosition()));
            int cursorX = box.getX() + textX + Math.min(
                    this.screenData.integer("search_cursor_max_x"),
                    Math.round(cursorWidth * textScale)
            );
            Draw.text(
                    Component.literal(this.screenData.string("search_cursor_text")),
                    cursorX,
                    box.getY() + textY
            )
                    .scale(textScale)
                    .color(this.screenData.color("search_cursor_color"))
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
        this.itemGrid.setEntries(this.collectItems(this.selectedCreativeTab));
    }

    private List<ItemStack> collectItems(CreativeTab selectedTab) {
        if (selectedTab.registeredTab() != null) {
            return this.filterItems(selectedTab.registeredTab().getDisplayItems(), selectedTab.hasSearchBar());
        }

        CreativeCategory category = selectedTab.category();
        if (category == CreativeCategory.SEARCH) {
            return this.filterItems(CreativeModeTabs.searchTab().getDisplayItems(), true);
        }

        List<ItemStack> items = new ArrayList<>();
        for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
            if (tab.getType() != CreativeModeTab.Type.CATEGORY || isModCreativeTab(tab)) {
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

    private List<ItemStack> filterItems(Iterable<ItemStack> source, boolean applySearch) {
        if (applySearch) {
            String query = this.searchEditBox == null
                    ? ""
                    : this.searchEditBox.value().trim().toLowerCase(Locale.ROOT);
            List<ItemStack> items = new ArrayList<>();
            for (ItemStack stack : source) {
                if (this.matchesSearch(stack, query)) {
                    items.add(stack);
                }
            }
            return List.copyOf(items);
        }
        List<ItemStack> items = new ArrayList<>();
        source.forEach(items::add);
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
                        GamepadInputHandler.cursorX(),
                        GamepadInputHandler.cursorY(),
                        false
                );
    }

    private Slot focusedCreativeHotbarSlot() {
        if (this.focusRegion != FocusRegion.HOTBAR || this.itemSlotLayer == null) {
            return null;
        }
        Slot slot = this.itemSlotLayer.getFocusedItemSlot();
        return slot != null && this.itemSlotGrid != null && this.itemSlotGrid.has(slot.index) ? slot : null;
    }

    private Slot actionHotbarSlot() {
        if (UiInputModeController.isFocusNavigationActive()) {
            return this.focusedCreativeHotbarSlot();
        }
        return this.itemSlotLayer == null || this.itemSlotGrid == null
                ? null
                : this.itemSlotLayer.getItemSlotAt(
                        this.itemSlotGrid,
                        GamepadInputHandler.cursorX(),
                        GamepadInputHandler.cursorY()
                );
    }

    private Slot firstEmptyCreativeHotbarSlot() {
        if (this.itemSlotGrid == null) {
            return null;
        }
        for (int index = 0; index < 9; index++) {
            Slot slot = this.menu.slots.get(36 + index);
            if (this.itemSlotGrid.has(slot.index) && slot.isActive() && slot.getItem().isEmpty()) {
                return slot;
            }
        }
        return null;
    }

    private Slot firstCreativeHotbarSlot() {
        if (this.itemSlotGrid == null) {
            return null;
        }
        for (int index = 0; index < 9; index++) {
            Slot slot = this.menu.slots.get(36 + index);
            if (this.itemSlotGrid.has(slot.index) && slot.isActive()) {
                return slot;
            }
        }
        return null;
    }

    private boolean isCreativeHotbarSlot(Slot slot) {
        return slot != null && this.itemSlotGrid != null && this.itemSlotGrid.has(slot.index) && slot.isActive();
    }

    private void focusHotbarSlotAndMoveCursor(Slot slot) {
        if (!this.isCreativeHotbarSlot(slot)) {
            return;
        }
        this.enterHotbarFocus();
        this.itemSlotLayer.setFocusedItemSlot(slot);
        var position = this.itemSlotGrid.get(slot.index);
        Cursor.moveTo(
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
        if (this.itemSlotLayer != null) {
            this.itemSlotLayer.clearFocusedItemSlot();
        }
        this.setFocused(this.itemGrid);
        if (!this.itemGrid.focusEntry(returnIndex)) {
            return;
        }
        var rectangle = this.itemGrid.getFocused().getRectangle();
        Cursor.moveTo(
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
    public ItemSlotGrid itemSlotGrid() {
        return this.itemSlotGrid;
    }

    @Override
    public CursorMagnetTarget cursorMagnetTargetAt(double guiX, double guiY) {
        CursorMagnetTarget catalogTarget = this.itemGrid == null
                ? null
                : this.itemGrid.cursorMagnetTargetAt(guiX, guiY);
        return catalogTarget != null
                ? catalogTarget
                : CustomItemSlotLayout.super.cursorMagnetTargetAt(guiX, guiY);
    }

    @Override
    public VanillaItemSlotLayer itemSlotLayer() {
        return this.itemSlotLayer;
    }

    @Override
    public boolean allowsItemSlotFocusNavigation() {
        return this.focusRegion == FocusRegion.HOTBAR;
    }

    @Override
    public boolean seedFocusFromPointer(double guiX, double guiY) {
        CreativeItemSlotElement catalogItem = this.itemGrid == null
                ? null
                : this.itemGrid.previewedSlot(guiX, guiY, false);
        if (catalogItem != null) {
            this.focusRegion = FocusRegion.CATALOG;
            if (this.itemSlotLayer != null) {
                this.itemSlotLayer.clearFocusedItemSlot();
            }
            this.setFocused(this.itemGrid);
            this.itemGrid.setFocused(catalogItem);
            return true;
        }

        Slot hotbarSlot = this.itemSlotLayer == null || this.itemSlotGrid == null
                ? null
                : this.itemSlotLayer.getItemSlotAt(this.itemSlotGrid, guiX, guiY);
        if (hotbarSlot == null) {
            return this.assignFirstCatalogFocusAndMoveCursor();
        }

        this.enterHotbarFocus();
        this.itemSlotLayer.setFocusedItemSlot(hotbarSlot);
        return true;
    }

    @Override
    public void clearFocusForPointer() {
        this.focusRegion = FocusRegion.NONE;
        if (this.itemGrid != null) {
            this.itemGrid.clearEntryFocus();
        }
        if (this.itemSlotLayer != null) {
            this.itemSlotLayer.clearFocusedItemSlot();
        }
    }

    @Override
    public boolean shouldShowCursorDuringFocus() {
        return this.focusRegion == FocusRegion.CATALOG;
    }

    @Override
    public boolean handleItemSlotNavigationBoundary(int keyCode) {
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
        this.extractDslBackground(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        this.extractDslContent(graphics, mouseX, mouseY, partialTick);
        this.itemGrid.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.tabs.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.previousPageButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.nextPageButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.searchEditBox.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.itemSlotLayer.updateHovered(this.itemSlotGrid, mouseX, mouseY);
        this.itemSlotLayer.renderItemSlotMap(graphics, this.itemSlotGrid);
        this.itemSlotLayer.renderHoveredItemSlot(graphics, this.itemSlotGrid);
        this.itemSlotLayer.renderCarriedItem(graphics, mouseX, mouseY);
        this.itemSlotLayer.renderTooltip(graphics, this.itemSlotGrid, mouseX, mouseY);

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
        if (this.previousPageButton.mouseClicked(event, doubleClick)
                || this.nextPageButton.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (this.searchEditBox.mouseClicked(event, doubleClick)) {
            this.focusSearchBox();
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
            if (this.itemGrid.shouldTakeFocusAfterInteraction()) {
                this.enterCatalogFocus(false);
            }
            return true;
        }
        boolean handled = this.itemSlotLayer.mouseClicked(
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
        if (handled) {
            this.slotPointerActive = true;
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
        if (this.slotPointerActive) {
            this.slotPointerActive = false;
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
        return this.previousPageButton.mouseReleased(event)
                || this.nextPageButton.mouseReleased(event)
                || this.searchEditBox.mouseReleased(event)
                || this.tabs.mouseReleased(event)
                || this.itemGrid.mouseReleased(event)
                || this.itemSlotLayer.mouseReleased(
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
        if (this.slotPointerActive) {
            return this.itemSlotLayer.mouseDragged(
                    this.itemSlotGrid,
                    event.x(),
                    event.y(),
                    event.button(),
                    dragX,
                    dragY
            );
        }
        return this.searchEditBox.mouseDragged(event, dragX, dragY)
                || this.itemGrid.mouseDragged(event, dragX, dragY)
                || this.itemSlotLayer.mouseDragged(
                this.itemSlotGrid,
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
        return this.itemSlotLayer.keyPressed(this.itemSlotGrid, event.key(), event.scancode())
                || super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return this.searchEditBox.charTyped(event) || super.charTyped(event);
    }

    @Override
    public boolean handleUiAction(UiAction action) {
        if (GamepadTextInputFocus.handleFocusedNavigation(
                this,
                this.searchEditBox,
                action,
                this::assignFirstCatalogFocusAndMoveCursor)) {
            return true;
        }
        if (this.focusRegion != FocusRegion.CATALOG) {
            return false;
        }
        if (this.itemGrid.handleUiAction(action)) {
            return true;
        }
        if (GamepadTextInputFocus.focusFromContentBoundary(
                this,
                this.searchEditBox,
                action,
                this::prepareSearchFocus)) {
            return true;
        }
        if (action == UiAction.MOVE_DOWN) {
            this.enterHotbarFocus();
            return ContainerItemSlotFocusController.moveFocus(this, GLFW.GLFW_KEY_DOWN);
        }

        return action.isDirectional();
    }

    private boolean focusSearchBox() {
        return GamepadTextInputFocus.focus(this, this.searchEditBox, this::prepareSearchFocus);
    }

    private void prepareSearchFocus() {
        this.focusRegion = FocusRegion.SEARCH;
        if (this.itemGrid != null) {
            this.itemGrid.clearEntryFocus();
        }
        if (this.itemSlotLayer != null) {
            this.itemSlotLayer.clearFocusedItemSlot();
        }
    }

    private void enterCatalogFocus(boolean focusLast) {
        this.focusRegion = FocusRegion.CATALOG;
        if (this.itemSlotLayer != null) {
            this.itemSlotLayer.clearFocusedItemSlot();
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
        if (this.itemSlotLayer != null) {
            this.itemSlotLayer.clearFocusedItemSlot();
        }
        this.setFocused(this.itemGrid);
        var rectangle = this.itemGrid.getFocused().getRectangle();
        Cursor.moveTo(
            rectangle.left() + rectangle.width() / 2.0D,
            rectangle.top() + rectangle.height() / 2.0D
        );
        return true;
    }

    private void prepareCreativeTabChange() {
        if (this.itemGrid == null) {
            return;
        }
        this.itemGrid.clearEntryFocus();
        if (this.itemSlotLayer != null) {
            this.itemSlotLayer.clearFocusedItemSlot();
        }
        this.setFocused(null);
        this.focusRegion = FocusRegion.NONE;
    }

    private void focusCatalogEntryUnderCursor() {
        if (this.itemGrid == null) {
            return;
        }
        CreativeItemSlotElement slot = this.itemGrid.previewedSlot(
                Cursor.x(),
                Cursor.y(),
                false
        );
        if (slot == null) {
            return;
        }
        this.focusRegion = FocusRegion.CATALOG;
        this.itemGrid.setFocused(slot);
        this.setFocused(this.itemGrid);
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
                case INGREDIENTS_AND_RESOURCES ->
                        tabPath.equals("food_and_drinks")
                                || tabPath.equals("ingredients")
                                || isUnmappedCreativeCategory(tabPath);
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

    private record CreativeTab(
            CreativeCategory category,
            CreativeModeTab registeredTab
    ) {
        private static CreativeTab category(CreativeCategory category) {
            return new CreativeTab(category, null);
        }

        private static CreativeTab registered(CreativeModeTab tab) {
            return new CreativeTab(null, tab);
        }

        private ItemStack icon() {
            return this.registeredTab != null ? this.registeredTab.getIconItem() : this.category.icon();
        }

        private Component title() {
            return this.registeredTab != null ? this.registeredTab.getDisplayName() : this.category.title();
        }

        private boolean hasSearchBar() {
            return this.registeredTab != null
                    ? this.registeredTab.hasSearchBar()
                    : this.category == CreativeCategory.SEARCH;
        }

        private int labelColor() {
            return this.registeredTab != null ? this.registeredTab.getLabelColor() : 0xFF404040;
        }
    }

    private enum FocusRegion {
        NONE,
        SEARCH,
        CATALOG,
        HOTBAR
    }

    private static void playTabSelectionSound() {
        Minecraft.getInstance().getSoundManager().play(
            SimpleSoundInstance.forUI(ModSounds.UI_HOVER.get(), 1.0F, 1.0F)
        );
    }
}
