package net.jr.screens.crafting;

import net.jr.api.client.render.Draw;
import net.jr.api.client.resource.Asset;
import net.jr.client.ui.presentation.UiPresentation;
import net.jr.api.client.ui.Cursor;
import net.jr.api.client.ui.DslContainer;
import net.jr.api.client.ui.UiContainerFile;
import net.jr.api.client.ui.UiDataFile;
import net.jr.client.components.navigation.UiAction;
import net.jr.client.components.navigation.UiActionHandler;
import net.jr.client.components.navigation.TabNavigator;
import net.jr.client.components.widgets.EditBoxElement;
import net.jr.client.components.widgets.TabBarElement;
import net.jr.client.components.widgets.TabElement;
import net.jr.client.components.widgets.recipe.RecipeEntryView;
import net.jr.client.components.widgets.recipe.RecipeGridElement;
import net.jr.client.components.widgets.recipe.RecipeSlotElement;
import net.jr.client.components.widgets.recipe.RecipeSlotStyle;
import net.jr.client.components.widgets.recipe.RecipeVariantView;
import net.jr.client.ui.navigation.UiInputBinding;
import net.jr.client.ui.navigation.UiInputModeController;
import net.jr.client.ui.cursor.CursorMagnetTarget;
import net.jr.api.client.input.GamepadDigitalInput;
import net.jr.client.ui.container.actions.ContainerItemSlotFocusController;
import net.jr.client.ui.container.DslContainerSize;
import net.jr.client.ui.container.slots.ItemSlotGrid;
import net.jr.client.ui.container.slots.ItemSlotGroup;
import net.jr.client.ui.container.slots.VanillaItemSlotLayer;
import net.jr.client.ui.container.slots.CustomItemSlotLayout;
import net.jr.client.ui.navigation.FocusCursorVisibilityHost;
import net.jr.client.ui.navigation.tabs.TabNavigationController;
import net.jr.client.ui.navigation.tabs.TabNavigationHost;
import net.jr.client.ui.hint.provider.RecipeControlHintHost;
import net.jr.experimental_features.gamepad_keyboard.GamepadTextInputFocus;
import net.jr.mixin.accessors.AbstractContainerScreenAccessor;
import net.jr.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import org.lwjgl.glfw.GLFW;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractCraftingScreen<M extends AbstractContainerMenu>
        extends AbstractContainerScreen<M>
        implements DslContainer, UiActionHandler, CustomItemSlotLayout, TabNavigationHost, FocusCursorVisibilityHost,
        RecipeControlHintHost, RecipeUpdateListener {

    private static final int MIN_MOD_CRAFTING_RECIPES = 5;

    private final UiContainerFile dslFile;
    private final CraftingScreenData screenData;
    private final int craftingColumns;
    private final int craftingRows;
    private final CraftingRecipePreviewRenderer recipePreviewRenderer;
    private final TabNavigationController tabNavigation =
            new TabNavigationController();
    private CraftingTabGroup craftingTabGroup;
    private FocusRegion focusRegion = FocusRegion.RECIPES;
    private boolean pendingDirectCraft;
    private int pendingCraftStateId = -1;
    private String currentResultTitle = "";

    protected VanillaItemSlotLayer itemSlotLayer;
    protected ItemSlotGrid itemSlotGrid;
    protected RecipeGridElement recipeGrid;
    protected TabBarElement recipeTabs;
    private Button previousPageButton;
    private Button nextPageButton;
    private EditBoxElement searchEditBox;

    protected AbstractCraftingScreen(
            M menu,
            Inventory inventory,
            Component title,
            int slotId,
            UiContainerFile dslFile,
            UiDataFile dataFile,
            int fallbackWidth,
            int fallbackHeight,
            int craftingColumns,
            int craftingRows,
            ItemSlotGroup craftingGridGroup,
            ItemSlotGroup craftingResultGroup
    ) {
        this(menu, inventory, title, slotId, dslFile, dataFile, craftingColumns, craftingRows,
                craftingGridGroup, craftingResultGroup,
                DslContainerSize.resolve(dslFile, fallbackWidth, fallbackHeight));
    }

    private AbstractCraftingScreen(
            M menu,
            Inventory inventory,
            Component title,
            int slotId,
            UiContainerFile dslFile,
            UiDataFile dataFile,
            int craftingColumns,
            int craftingRows,
            ItemSlotGroup craftingGridGroup,
            ItemSlotGroup craftingResultGroup,
            DslContainerSize dimensions
    ) {
        super(menu, inventory, title, dimensions.width(), dimensions.height());
        this.dslFile = dslFile;
        this.screenData = new CraftingScreenData(dataFile);
        this.craftingColumns = craftingColumns;
        this.craftingRows = craftingRows;
        this.recipePreviewRenderer = new CraftingRecipePreviewRenderer(
                craftingColumns,
                craftingRows,
                craftingGridGroup,
                craftingResultGroup,
                new CraftingPreviewStyle(
                        this.screenData.color("preview_missing_background"),
                        this.screenData.color("preview_missing_tint"),
                        this.screenData.decimal("preview_missing_item_alpha"),
                        this.screenData.integer("preview_background_inset"),
                        this.screenData.integer("preview_cycle_ticks"),
                        this.screenData.asset("preview_missing_overlay_asset"),
                        this.screenData.integer("preview_missing_overlay_x"),
                        this.screenData.integer("preview_missing_overlay_y"),
                        this.screenData.integer("preview_missing_overlay_width"),
                        this.screenData.integer("preview_missing_overlay_height")
                )
        );
    }

    @Override
    public final UiContainerFile dslFile() {
        return this.dslFile;
    }

    @Override
    protected void init() {
        super.init();
        this.rebuildCreativeTabContents();
        this.exposeDslData();
        this.itemSlotGrid = this.dslItemSlotGrid("menu");
        if (this.itemSlotGrid == null) {
            throw new IllegalStateException("Crafting container DSL must declare item_slots id 'menu'");
        }
        this.itemSlotLayer = this.itemSlotGrid.itemSlotLayer();

        this.recipeGrid = new RecipeGridElement(
                this.leftPos + this.screenData.integer("recipe_x"),
                this.topPos + this.screenData.integer("recipe_y"),
                this.screenData.integer("recipe_columns") * this.screenData.integer("recipe_cell_size"),
                this.screenData.integer("recipe_rows") * this.screenData.integer("recipe_cell_size"),
                this.screenData.integer("recipe_rows"),
                this.screenData.integer("recipe_columns"),
                new RecipeSlotStyle(
                        this.screenData.asset("recipe_slot_available_asset"),
                        this.screenData.asset("recipe_slot_unavailable_asset"),
                        this.screenData.asset("recipe_variant_available_asset"),
                        this.screenData.asset("recipe_variant_unavailable_asset"),
                        this.screenData.asset("recipe_variants_closed_asset"),
                        this.screenData.asset("recipe_variants_open_asset"),
                        this.screenData.integer("recipe_variant_indicator_size"),
                        this.screenData.integer("recipe_variant_indicator_inset"),
                        this.screenData.integer("recipe_item_max_size"),
                        this.screenData.integer("recipe_item_padding"),
                        this.screenData.decimal("recipe_unavailable_item_alpha")
                )
        );
        this.configureRecipeGrid(this.recipeGrid);
        this.recipeGrid.onCraft(this::craftRecipe);

        this.craftingTabGroup = new CraftingTabGroup();
        this.recipeTabs = this.craftingTabGroup.tabs();
        this.createPageButtons();
        this.createSearchBox();
        this.tabNavigation.clear();
        this.tabNavigation.bind(
                this.recipeTabs,
                UiInputBinding.gamepad(GamepadDigitalInput.BUMPER_LEFT),
                UiInputBinding.gamepad(GamepadDigitalInput.BUMPER_RIGHT),
                Component.translatable("hint.container.java_reforged.change_tab"),
                AbstractCraftingScreen::playTabSelectionSound
        );
        this.tabNavigation.bind(
                this.craftingTabGroup,
                UiInputBinding.gamepad(GamepadDigitalInput.TRIGGER_LEFT),
                UiInputBinding.gamepad(GamepadDigitalInput.TRIGGER_RIGHT),
                Component.translatable("hint.container.java_reforged.change_tab_page"),
                AbstractCraftingScreen::playTabSelectionSound
        );

        this.addRenderableWidget(this.recipeGrid);
        this.addRenderableWidget(this.recipeTabs);
        this.addRenderableWidget(this.previousPageButton);
        this.addRenderableWidget(this.nextPageButton);
        this.addRenderableWidget(this.searchEditBox);
        this.reloadRecipes();
        this.focusFirstRecipeEntryAndMoveCursor();
    }

    private void exposeDslData() {
        this.dslData().exposeText("crafting.inventory_title", () -> this.playerInventoryTitle.getString());
        this.dslData().exposeText("crafting.title", () -> this.playerInventoryTitle.getString());
        this.dslData().exposeText("crafting.tab_title", () -> this.craftingTabGroup == null
                ? "" : this.craftingTabGroup.selectedTitle().getString());
        this.dslData().exposeText("crafting.page_label", () -> this.craftingTabGroup == null
                ? "" : (this.craftingTabGroup.pageIndex() + 1) + " / " + this.craftingTabGroup.pageCount());
        this.dslData().exposeText("crafting.result_title", () -> this.currentResultTitle);
        this.dslData().exposeText("crafting.search_query", () -> this.searchEditBox == null
                ? "" : this.searchEditBox.value());
        this.dslData().exposeText("crafting.focus_region", () -> this.focusRegion.name()
                .toLowerCase(java.util.Locale.ROOT));
        this.dslData().exposeText("crafting.selected_category", () -> {
            CraftingCategory category = this.craftingTabGroup == null
                    ? null : this.craftingTabGroup.selectedCategory();
            return category == null ? "" : category.name().toLowerCase(java.util.Locale.ROOT);
        });
        this.dslData().exposeText("crafting.selected_mod", () -> {
            ModCraftingTab mod = this.craftingTabGroup == null
                    ? null : this.craftingTabGroup.selectedMod();
            return mod == null ? "" : mod.namespace;
        });
        this.dslData().exposeBoolean("crafting.search_selected", () -> this.craftingTabGroup != null
                && this.craftingTabGroup.isSearchSelected());
        this.dslData().exposeBoolean("crafting.show_tab_title", () -> this.craftingTabGroup != null
                && !this.craftingTabGroup.isSearchSelected());
        this.dslData().exposeBoolean("crafting.show_page_label", () -> this.craftingTabGroup != null
                && this.craftingTabGroup.pageCount() > 1);
        this.dslData().exposeBoolean("crafting.has_result_title", () -> !this.currentResultTitle.isEmpty());
        this.dslData().exposeBoolean("crafting.has_targeted_recipe", this::hasTargetedRecipe);
        this.dslData().exposeBoolean("crafting.targeted_recipe_craftable", this::isTargetedRecipeCraftable);
        this.dslData().exposeBoolean("crafting.recipes_focused", () -> this.focusRegion == FocusRegion.RECIPES);
        this.dslData().exposeBoolean("crafting.inventory_focused", () -> this.focusRegion == FocusRegion.INVENTORY);
        this.dslData().exposeBoolean("crafting.search_focused", () -> this.focusRegion == FocusRegion.SEARCH);
        this.dslData().exposeBoolean("crafting.has_previous_page", () -> this.craftingTabGroup != null
                && this.craftingTabGroup.pageIndex() > 0);
        this.dslData().exposeBoolean("crafting.has_next_page", () -> this.craftingTabGroup != null
                && this.craftingTabGroup.pageIndex() + 1 < this.craftingTabGroup.pageCount());
        this.dslData().exposeNumber("crafting.page", () -> this.craftingTabGroup == null
                ? 1 : this.craftingTabGroup.pageIndex() + 1);
        this.dslData().exposeNumber("crafting.page_count", () -> this.craftingTabGroup == null
                ? 1 : this.craftingTabGroup.pageCount());
    }

    private void rebuildCreativeTabContents() {
        if (this.minecraft == null || this.minecraft.player == null
                || this.minecraft.player.connection == null) {
            return;
        }
        CreativeModeTabs.tryRebuildTabContents(
                this.minecraft.player.connection.enabledFeatures(),
                this.minecraft.player.canUseGameMasterBlocks(),
                this.minecraft.player.level().registryAccess()
        );
    }

    private void createPageButtons() {
        int buttonSize = this.screenData.integer("page_button_size");
        int y = this.topPos + this.screenData.integer("page_button_y_offset");
        this.previousPageButton = Button.builder(
                        Component.literal("<"), ignored -> this.craftingTabGroup.changePage(-1))
                .pos(this.leftPos, y)
                .size(buttonSize, buttonSize)
                .build();
        this.nextPageButton = Button.builder(
                        Component.literal(">"), ignored -> this.craftingTabGroup.changePage(1))
                .pos(this.leftPos + this.imageWidth - buttonSize, y)
                .size(buttonSize, buttonSize)
                .build();
        this.craftingTabGroup.updatePageButtons();
    }

    private void createSearchBox() {
        this.searchEditBox = new EditBoxElement(
                this.leftPos + this.screenData.integer("search_x"),
                this.topPos + this.screenData.integer("search_y"),
                this.screenData.integer("search_width"),
                this.screenData.integer("search_height"),
                Component.translatable(this.screenData.string("search_placeholder_key"))
        );
        this.searchEditBox.setFont(this.font);
        this.searchEditBox.setHint(
                Component.translatable(this.screenData.string("search_placeholder_key"))
        );
        this.searchEditBox.setMaxLength(this.screenData.integer("search_max_length"));
        this.searchEditBox.setRenderer(this::renderSearchBox);
        this.searchEditBox.setResponder(ignored -> {
            if (this.craftingTabGroup != null && this.craftingTabGroup.isSearchSelected()) {
                this.reloadRecipes();
            }
        });
        this.setSearchBoxVisible(this.craftingTabGroup.isSearchSelected());
    }

    private void renderSearchBox(
            GuiGraphicsExtractor graphics,
            AbstractWidget widget,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        EditBoxElement box = (EditBoxElement)widget;
        Draw.image(this.screenData.asset("search_asset"), box.getX(), box.getY(), box.getWidth(), box.getHeight())
                .draw(graphics);

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
            Draw.text(Component.literal(this.screenData.string("search_cursor_text")), cursorX, box.getY() + textY)
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

    private void configureRecipeGrid(RecipeGridElement grid) {
        int cellSize = this.screenData.integer("recipe_cell_size");
        grid.setCellSize(cellSize, cellSize);
        grid.setAutoPlaceScrollbar(false);
        grid.setFixedScrollbarHandleSize(this.screenData.decimal("scrollbar_handle_size"));
        grid.scrollbar().setX(this.leftPos + this.screenData.integer("scrollbar_x"));
        grid.scrollbar().setY(this.topPos + this.screenData.integer("scrollbar_y"));
        grid.scrollbar().setSize(
                this.screenData.integer("scrollbar_width"),
                this.screenData.integer("scrollbar_height")
        );
        grid.scrollbar().setRenderer((graphics, ignored, mouseX, mouseY, partialTick) -> {
            boolean scrollable = grid.hasScrollableOverflow();
            grid.scrollbar().setActive(scrollable);
            Draw.image(
                    this.screenData.asset(scrollable
                            ? "scrollbar_asset"
                            : "scrollbar_disabled_asset"),
                    Math.round(grid.scrollbar().handleX()),
                    Math.round(grid.scrollbar().handleY()),
                    this.screenData.integer("scrollbar_handle_width"),
                    this.screenData.integer("scrollbar_handle_height")
            ).draw(graphics);
        });
    }

    private void updateResultTitle(int mouseX, int mouseY) {
        ItemStack resultStack = this.menu.slots.isEmpty()
                ? ItemStack.EMPTY
                : this.menu.slots.getFirst().getItem();
        if (resultStack.isEmpty() && this.minecraft != null
                && this.minecraft.player != null && this.recipeGrid != null) {
            RecipeVariantView preview = this.recipeGrid.previewedRecipe(
                    mouseX,
                    mouseY,
                    this.focusRegion == FocusRegion.RECIPES
            );
            if (preview != null) {
                resultStack = this.recipePreviewRenderer.resultStack(
                        preview.recipe().display(),
                        SlotDisplayContext.fromLevel(this.minecraft.player.level())
                );
            }
        }
        Component resultTitle = resultStack.isEmpty() ? Component.empty() : resultStack.getHoverName();
        this.currentResultTitle = resultTitle.getString();
    }

    @Override
    public final ItemSlotGrid itemSlotGrid() {
        return this.itemSlotGrid;
    }

    @Override
    public final VanillaItemSlotLayer itemSlotLayer() {
        return this.itemSlotLayer;
    }

    @Override
    public final CursorMagnetTarget cursorMagnetTargetAt(double guiX, double guiY) {
        CursorMagnetTarget recipeTarget = this.recipeGrid == null
                ? null
                : this.recipeGrid.cursorMagnetTargetAt(guiX, guiY);
        return recipeTarget != null
                ? recipeTarget
                : CustomItemSlotLayout.super.cursorMagnetTargetAt(guiX, guiY);
    }

    @Override
    public final TabNavigationController tabNavigationController() {
        return this.tabNavigation;
    }

    @Override
    public final boolean allowsItemSlotFocusNavigation() {
        return this.focusRegion == FocusRegion.INVENTORY;
    }

    @Override
    public final boolean seedFocusFromPointer(double guiX, double guiY) {
        Slot inventorySlot = this.itemSlotLayer == null || this.itemSlotGrid == null
                ? null
                : this.itemSlotLayer.getItemSlotAt(this.itemSlotGrid, guiX, guiY);
        if (inventorySlot != null) {
            this.enterInventoryFocus();
            this.itemSlotLayer.setFocusedItemSlot(inventorySlot);
            return true;
        }

        RecipeSlotElement recipeSlot = this.recipeGrid == null
                ? null
                : this.recipeGrid.previewedRecipeSlot(guiX, guiY, false);
        if (recipeSlot == null) {
            return this.assignFirstRecipeFocusAndMoveCursor();
        }

        this.recipeGrid.setFocused(recipeSlot);
        this.returnCraftingGridItemsToInventory();
        this.focusRegion = FocusRegion.RECIPES;
        if (this.itemSlotLayer != null) {
            this.itemSlotLayer.clearFocusedItemSlot();
        }
        this.setFocused(this.recipeGrid);
        return true;
    }

    @Override
    public final void clearFocusForPointer() {
        this.focusRegion = FocusRegion.NONE;
        if (this.recipeGrid != null) {
            this.recipeGrid.clearEntryFocus();
        }
        if (this.itemSlotLayer != null) {
            this.itemSlotLayer.clearFocusedItemSlot();
        }
    }

    @Override
    public final boolean shouldShowCursorDuringFocus() {
        return this.focusRegion == FocusRegion.RECIPES;
    }

    @Override
    public final boolean handleItemSlotNavigationBoundary(int keyCode) {
        if (this.focusRegion == FocusRegion.INVENTORY && keyCode == GLFW.GLFW_KEY_UP) {
            this.enterRecipeFocus(true);
            return true;
        }
        return false;
    }

    @Override
    public void recipesUpdated() {
        this.pendingDirectCraft = false;
        this.pendingCraftStateId = -1;
        if (this.craftingTabGroup != null) {
            this.craftingTabGroup.refreshPages();
        }
        this.reloadRecipes();
    }

    @Override
    public void fillGhostRecipe(RecipeDisplay display) {


        this.pendingDirectCraft = false;
        this.pendingCraftStateId = -1;
        this.reloadRecipes();
    }

    protected final void reloadRecipes() {
        if (this.minecraft == null || this.minecraft.player == null || this.recipeGrid == null) {
            return;
        }

        StackedItemContents contents = new StackedItemContents();
        this.minecraft.player.getInventory().fillStackedContents(contents);
        ContextMap displayContext = SlotDisplayContext.fromLevel(this.minecraft.player.level());
        List<RecipeEntryView> visibleRecipes = new ArrayList<>();
        List<RecipeVariantView> groupedWallVariants = new ArrayList<>();
        RecipeCollection wallCollection = null;
        CraftingCategory selectedCategory = this.craftingTabGroup == null
                ? CraftingCategory.BUILDING_BLOCKS
                : this.craftingTabGroup.selectedCategory();
        boolean groupWalls = selectedCategory == CraftingCategory.BUILDING_BLOCKS
                || (this.craftingTabGroup != null && this.craftingTabGroup.isSearchSelected());

        for (RecipeCollection collection : this.minecraft.player.getRecipeBook().getCollections()) {
            collection.selectRecipes(contents, this::fitsCraftingGrid);
            List<RecipeDisplayEntry> variants = collection.getRecipes().stream()
                    .filter(entry -> this.fitsCraftingGrid(entry.display()))
                    .filter(entry -> this.matchesSelectedCraftingCategory(entry, displayContext))
                    .toList();
            if (variants.isEmpty()) {
                continue;
            }

            List<RecipeVariantView> variantViews = this.createVariantViews(
                    collection,
                    variants,
                    displayContext
            );

            if (groupWalls) {
                List<RecipeVariantView> wallVariants = variantViews.stream()
                        .filter(variant -> CraftingRecipeClassifier.isWall(variant.icon()))
                        .toList();
                if (!wallVariants.isEmpty()) {
                    if (wallCollection == null) {
                        wallCollection = collection;
                    }
                    wallVariants.forEach(variant -> this.addDistinctResult(groupedWallVariants, variant));
                }

                List<RecipeVariantView> nonWallVariants = variantViews.stream()
                        .filter(variant -> !CraftingRecipeClassifier.isWall(variant.icon()))
                        .toList();
                if (!nonWallVariants.isEmpty()) {
                    visibleRecipes.add(new RecipeEntryView(collection, nonWallVariants));
                }
            } else {
                visibleRecipes.add(new RecipeEntryView(collection, variantViews));
            }
        }

        if (!groupedWallVariants.isEmpty() && wallCollection != null) {
            groupedWallVariants.sort(Comparator.comparing(
                    variant -> CraftingRecipeClassifier.buildingOrder(variant.icon())
            ));
            visibleRecipes.add(new RecipeEntryView(wallCollection, groupedWallVariants));
        }

        this.sortRecipeEntries(visibleRecipes);

        this.recipeGrid.setRecipeGroups(visibleRecipes);
    }

    private void sortRecipeEntries(List<RecipeEntryView> recipes) {
        ModCraftingTab selectedMod = this.craftingTabGroup == null
                ? null : this.craftingTabGroup.selectedMod();
        if (selectedMod != null) {
            recipes.sort(Comparator
                    .comparingInt((RecipeEntryView entry) -> entry.variants().stream()
                            .mapToInt(variant -> selectedMod.creativeOrder.getOrDefault(
                                    variant.icon().getItem(), Integer.MAX_VALUE))
                            .min().orElse(Integer.MAX_VALUE))
                    .thenComparing(entry -> entry.variants().stream()
                            .map(variant -> BuiltInRegistries.ITEM.getKey(variant.icon().getItem()).toString())
                            .min(String::compareTo).orElse("")));
            return;
        }
        CraftingCategory selectedCategory = this.craftingTabGroup == null
                ? CraftingCategory.BUILDING_BLOCKS
                : this.craftingTabGroup.selectedCategory();
        if (selectedCategory == CraftingCategory.EQUIPMENT_AND_TOOLS) {
            recipes.sort(Comparator.comparing(this::equipmentOrder));
        } else if (selectedCategory == CraftingCategory.BUILDING_BLOCKS) {
            recipes.sort(Comparator.comparing(this::buildingOrder));
        } else if (selectedCategory == CraftingCategory.DECORATION) {
            recipes.sort(Comparator.comparing(this::decorationOrder));
        } else if (selectedCategory == CraftingCategory.REDSTONE_AND_MECHANISMS) {
            recipes.sort(Comparator.comparing(this::redstoneOrder));
        } else if (selectedCategory == CraftingCategory.RESOURCES_AND_FOOD) {
            recipes.sort(Comparator.comparing(this::resourcesFoodOrder));
        }
    }

    private CraftingRecipeClassifier.EquipmentOrder equipmentOrder(RecipeEntryView entry) {
        return entry.variants().stream()
                .map(variant -> CraftingRecipeClassifier.equipmentOrder(variant.icon()))
                .min(Comparator.naturalOrder())
                .orElse(new CraftingRecipeClassifier.EquipmentOrder(4, 50, 99, ""));
    }

    private CraftingRecipeClassifier.BuildingOrder buildingOrder(RecipeEntryView entry) {
        return entry.variants().stream()
                .map(variant -> CraftingRecipeClassifier.buildingOrder(variant.icon()))
                .min(Comparator.naturalOrder())
                .orElse(new CraftingRecipeClassifier.BuildingOrder(99, 100, 99, ""));
    }

    private CraftingRecipeClassifier.DecorationOrder decorationOrder(RecipeEntryView entry) {
        return entry.variants().stream()
                .map(variant -> CraftingRecipeClassifier.decorationOrder(variant.icon()))
                .min(Comparator.naturalOrder())
                .orElse(new CraftingRecipeClassifier.DecorationOrder(99, 99, 100, ""));
    }

    private CraftingRecipeClassifier.RedstoneOrder redstoneOrder(RecipeEntryView entry) {
        return entry.variants().stream()
                .map(variant -> CraftingRecipeClassifier.redstoneOrder(variant.icon()))
                .min(Comparator.naturalOrder())
                .orElse(new CraftingRecipeClassifier.RedstoneOrder(99, 99, 100, ""));
    }

    private CraftingRecipeClassifier.ResourcesFoodOrder resourcesFoodOrder(RecipeEntryView entry) {
        return entry.variants().stream()
                .map(variant -> CraftingRecipeClassifier.resourcesFoodOrder(variant.icon()))
                .min(Comparator.naturalOrder())
                .orElse(new CraftingRecipeClassifier.ResourcesFoodOrder(99, 99, 99, 99, ""));
    }

    private boolean matchesSelectedCraftingCategory(
            RecipeDisplayEntry recipe,
            ContextMap displayContext
    ) {
        CraftingCategory selectedCategory = this.craftingTabGroup == null
                ? CraftingCategory.BUILDING_BLOCKS
                : this.craftingTabGroup.selectedCategory();
        if (this.craftingTabGroup != null && this.craftingTabGroup.isSearchSelected()) {
            String query = this.searchEditBox == null
                    ? ""
                    : this.searchEditBox.value().trim().toLowerCase(java.util.Locale.ROOT);
            if (query.isEmpty()) {
                return true;
            }
            ItemStack result = recipe.resultItems(displayContext).stream()
                    .findFirst()
                    .orElse(ItemStack.EMPTY);
            if (result.isEmpty()) {
                return false;
            }
            String itemName = result.getHoverName().getString().toLowerCase(java.util.Locale.ROOT);
            String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .getKey(result.getItem())
                    .toString()
                    .toLowerCase(java.util.Locale.ROOT);
            return itemName.contains(query) || itemId.contains(query);
        }

        ModCraftingTab selectedMod = this.craftingTabGroup == null
                ? null : this.craftingTabGroup.selectedMod();
        if (selectedMod != null) {

            return recipe.resultItems(displayContext).stream()
                    .filter(result -> !result.isEmpty())
                    .map(result -> BuiltInRegistries.ITEM.getKey(result.getItem()))
                    .anyMatch(id -> id != null && selectedMod.namespace.equals(id.getNamespace()));
        }

        ItemStack result = recipe.resultItems(displayContext).stream()
                .findFirst()
                .orElse(ItemStack.EMPTY);
        Identifier resultId = result.isEmpty() ? null
                : BuiltInRegistries.ITEM.getKey(result.getItem());
        if (resultId != null && this.craftingTabGroup != null
                && this.craftingTabGroup.hasMod(resultId.getNamespace())) {
            return false;
        }
        CraftingCategory classifiedCategory = CraftingRecipeClassifier.classify(recipe, result);
        if (this.craftingColumns == 2
                && selectedCategory == CraftingCategory.DECORATION) {

            return classifiedCategory == CraftingCategory.DECORATION
                    || classifiedCategory == CraftingCategory.REDSTONE_AND_MECHANISMS;
        }
        return classifiedCategory == selectedCategory;
    }

    private RecipeVariantView createVariantView(
            RecipeCollection collection,
            RecipeDisplayEntry recipe,
            ContextMap displayContext
    ) {
        ItemStack icon = recipe.resultItems(displayContext).stream()
                .findFirst()
                .orElse(ItemStack.EMPTY);
        return new RecipeVariantView(
                collection,
                recipe,
                icon,
                collection.isCraftable(recipe.id())
        );
    }

    private List<RecipeVariantView> createVariantViews(
            RecipeCollection collection,
            List<RecipeDisplayEntry> recipes,
            ContextMap displayContext
    ) {
        List<RecipeVariantView> results = new ArrayList<>();
        for (RecipeDisplayEntry recipe : recipes) {
            this.addDistinctResult(results, this.createVariantView(collection, recipe, displayContext));
        }
        return List.copyOf(results);
    }

    private void addDistinctResult(List<RecipeVariantView> results, RecipeVariantView candidate) {
        for (int index = 0; index < results.size(); index++) {
            RecipeVariantView existing = results.get(index);
            if (existing.hasSameResult(candidate.icon())) {
                results.set(index, existing.withAlternativesFrom(candidate));
                return;
            }
        }
        results.add(candidate);
    }

    public boolean isTargetedRecipeCraftable() {
        return this.recipeGrid != null
                && this.recipeGrid.previewedRecipeIsCraftable(
                this.focusRegion == FocusRegion.RECIPES
        );
    }

    public boolean hasTargetedRecipe() {
        return this.recipeGrid != null
                && this.recipeGrid.previewedRecipe(
                this.focusRegion == FocusRegion.RECIPES
        ) != null;
    }

    public RecipeSlotElement.ActivationFeedback recipeMouseActivationFeedbackAt(
            double mouseX,
            double mouseY,
            int button
    ) {
        return this.recipeGrid == null
                ? null
                : this.recipeGrid.mouseActivationFeedbackAt(mouseX, mouseY, button);
    }

    public RecipeSlotElement.ActivationFeedback focusedRecipeActivationFeedback(UiAction action) {
        return this.focusRegion == FocusRegion.RECIPES && this.recipeGrid != null
                ? this.recipeGrid.focusedActivationFeedback(action)
                : null;
    }

    @Override
    public RecipeControlHintHost.Activation controlHintActivation(UiAction action) {
        RecipeSlotElement.ActivationFeedback feedback = focusedRecipeActivationFeedback(action);
        if (feedback == RecipeSlotElement.ActivationFeedback.CRAFT) {
            return RecipeControlHintHost.Activation.CRAFT;
        }
        if (feedback == RecipeSlotElement.ActivationFeedback.CLICK) {
            return RecipeControlHintHost.Activation.VIEW_VARIANTS;
        }
        return RecipeControlHintHost.Activation.NONE;
    }

    private boolean fitsCraftingGrid(RecipeDisplay display) {
        if (display instanceof ShapedCraftingRecipeDisplay shaped) {
            return shaped.width() <= this.craftingColumns && shaped.height() <= this.craftingRows;
        }
        if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
            return shapeless.ingredients().size() <= this.craftingColumns * this.craftingRows;
        }
        return false;
    }

    private void craftRecipe(RecipeVariantView view) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.pendingDirectCraft = view.craftable();
            this.pendingCraftStateId = this.menu.getStateId();
            this.minecraft.gameMode.handlePlaceRecipe(
                    this.menu.containerId,
                    view.recipe().id(),
                    false
            );
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.finishPendingCraft();
    }

    private void finishPendingCraft() {
        if (!this.pendingDirectCraft || this.minecraft == null || this.minecraft.gameMode == null) {
            return;
        }
        if (this.menu.getStateId() == this.pendingCraftStateId || this.menu.slots.isEmpty()) {
            return;
        }

        Slot result = this.menu.slots.getFirst();
        if (!result.isActive() || !result.hasItem()) {
            return;
        }

        this.pendingDirectCraft = false;
        this.pendingCraftStateId = -1;
        ((AbstractContainerScreenAccessor)this).javaReforged$invokeSlotClicked(
                result,
                result.index,
                0,
                ContainerInput.QUICK_MOVE
        );
        this.reloadRecipes();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.extractDslBackground(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.updateResultTitle(mouseX, mouseY);
        this.extractDslContent(graphics, mouseX, mouseY, partialTick);
        this.recipeGrid.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.recipeTabs.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.previousPageButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.nextPageButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (this.searchEditBox != null) {
            this.searchEditBox.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
        this.itemSlotLayer.updateHovered(this.itemSlotGrid, mouseX, mouseY);
        this.itemSlotLayer.renderItemSlotMap(graphics, this.itemSlotGrid);
        this.renderRecipePreview(graphics, mouseX, mouseY);
        this.itemSlotLayer.renderHoveredItemSlot(graphics, this.itemSlotGrid);
        this.itemSlotLayer.renderCarriedItem(graphics, mouseX, mouseY);
        this.itemSlotLayer.renderTooltip(graphics, this.itemSlotGrid, mouseX, mouseY);
        this.renderRecipeTooltip(graphics, mouseX, mouseY);
    }

    private void renderRecipePreview(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.minecraft == null || this.minecraft.player == null || this.recipeGrid == null) {
            return;
        }
        RecipeVariantView preview = this.recipeGrid.previewedRecipe(
                mouseX,
                mouseY,
                this.focusRegion == FocusRegion.RECIPES
        );
        if (preview == null) {
            return;
        }
        this.recipePreviewRenderer.render(
                graphics,
                preview.recipe().display(),
                SlotDisplayContext.fromLevel(this.minecraft.player.level()),
                this.itemSlotGrid,
                this.menu,
                this.minecraft.player.getInventory(),
                preview.craftable()
        );
    }

    private void renderRecipeTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.menu.getCarried().isEmpty() || this.recipeGrid == null) {
            return;
        }

        RecipeSlotElement slot = this.recipeGrid.previewedRecipeSlot(
                mouseX,
                mouseY,
                this.focusRegion == FocusRegion.RECIPES
        );
        if (slot == null || slot.displayedRecipe().icon().isEmpty()) {
            return;
        }

        boolean pointerOverSlot = slot.isMouseOver(mouseX, mouseY);
        int tooltipX = pointerOverSlot ? mouseX : slot.getX() + slot.getWidth() / 2;
        int tooltipY = pointerOverSlot ? mouseY : slot.getY() + slot.getHeight() / 2;
        graphics.setTooltipForNextFrame(this.font, slot.displayedRecipe().icon(), tooltipX, tooltipY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.previousPageButton.mouseClicked(event, doubleClick)
                || this.nextPageButton.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (this.searchEditBox != null && this.searchEditBox.mouseClicked(event, doubleClick)) {
            this.focusSearchBox();
            return true;
        }
        if (this.recipeTabs.mouseClicked(event, doubleClick)) {
            return true;
        }
        RecipeSlotElement.ActivationFeedback recipeFeedback =
                this.recipeGrid.mouseActivationFeedbackAt(event.x(), event.y(), event.button());
        if (this.recipeGrid.mouseClicked(event, doubleClick)) {
            if (this.recipeGrid.shouldTakeFocusAfterInteraction()) {
                this.enterRecipeFocus(
                        false,
                        recipeFeedback != RecipeSlotElement.ActivationFeedback.CRAFT
                );
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
            this.enterInventoryFocus();
        }
        return handled;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean pageHandled = this.previousPageButton.mouseReleased(event)
                || this.nextPageButton.mouseReleased(event);
        boolean searchHandled = this.searchEditBox != null && this.searchEditBox.mouseReleased(event);
        boolean widgetHandled = this.recipeGrid.mouseReleased(event);
        widgetHandled = this.recipeTabs.mouseReleased(event) || widgetHandled;
        boolean slotHandled = this.itemSlotLayer.mouseReleased(
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
        return pageHandled || searchHandled || widgetHandled || slotHandled;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.searchEditBox != null && this.searchEditBox.mouseDragged(event, dragX, dragY)) {
            return true;
        }
        if (this.recipeGrid.mouseDragged(event, dragX, dragY)) {
            return true;
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return this.recipeGrid.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.searchEditBox != null && this.searchEditBox.keyPressed(event)) {
            return true;
        }
        UiAction navigation = switch (event.key()) {
            case GLFW.GLFW_KEY_LEFT -> UiAction.MOVE_LEFT;
            case GLFW.GLFW_KEY_RIGHT -> UiAction.MOVE_RIGHT;
            case GLFW.GLFW_KEY_UP -> UiAction.MOVE_UP;
            case GLFW.GLFW_KEY_DOWN -> UiAction.MOVE_DOWN;
            default -> null;
        };
        if (this.focusRegion == FocusRegion.RECIPES
                && navigation != null
                && this.handleUiAction(navigation)) {
            return true;
        }
        return this.itemSlotLayer.keyPressed(this.itemSlotGrid, event.key(), event.scancode()) || super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return this.searchEditBox != null && this.searchEditBox.charTyped(event)
                || super.charTyped(event);
    }

    @Override
    public boolean handleUiAction(UiAction action) {
        if (this.searchEditBox != null && GamepadTextInputFocus.handleFocusedNavigation(
                this,
                this.searchEditBox,
                action,
                this::assignFirstRecipeFocusAndMoveCursor)) {
            return true;
        }
        if (this.focusRegion != FocusRegion.RECIPES) {
            return false;
        }
        if (this.recipeGrid.handleUiAction(action)) {
            return true;
        }
        if (this.searchEditBox != null && GamepadTextInputFocus.focusFromContentBoundary(
                this,
                this.searchEditBox,
                action,
                this::prepareSearchFocus)) {
            return true;
        }
        if (action == UiAction.MOVE_DOWN) {
            this.enterInventoryFocus();
            return ContainerItemSlotFocusController.moveFocus(this, GLFW.GLFW_KEY_DOWN);
        }

        return action.isDirectional();
    }

    private boolean focusSearchBox() {
        return this.searchEditBox != null
            && GamepadTextInputFocus.focus(this, this.searchEditBox, this::prepareSearchFocus);
    }

    private void prepareSearchFocus() {
        this.returnCraftingGridItemsToInventory();
        this.focusRegion = FocusRegion.SEARCH;
        if (this.recipeGrid != null) {
            this.recipeGrid.clearEntryFocus();
        }
        if (this.itemSlotLayer != null) {
            this.itemSlotLayer.clearFocusedItemSlot();
        }
    }

    private void enterRecipeFocus(boolean focusLast) {
        this.enterRecipeFocus(focusLast, true);
    }

    private void enterRecipeFocus(boolean focusLast, boolean returnIngredients) {
        if (returnIngredients) {
            this.returnCraftingGridItemsToInventory();
        }
        this.focusRegion = FocusRegion.RECIPES;
        if (this.itemSlotLayer != null) {
            this.itemSlotLayer.clearFocusedItemSlot();
        }
        this.setFocused(this.recipeGrid);
        if (this.recipeGrid != null) {
            if (focusLast) {
                this.recipeGrid.focusLastEntry();
            } else if (this.recipeGrid.getFocused() == null) {
                this.recipeGrid.focusFirstEntry();
            } else {
                this.recipeGrid.getFocused().setFocused(true);
            }
        }
    }

    private boolean focusFirstRecipeEntryAndMoveCursor() {
        if (!this.assignFirstRecipeFocusAndMoveCursor()) {
            return false;
        }
        UiInputModeController.activateFocusNavigation();
        return true;
    }

    private boolean assignFirstRecipeFocusAndMoveCursor() {
        this.returnCraftingGridItemsToInventory();
        if (this.recipeGrid == null || !this.recipeGrid.focusFirstEntry()) {
            if (this.recipeGrid != null) {
                this.recipeGrid.clearEntryFocus();
            }
            if (this.getFocused() == this.recipeGrid) {
                this.setFocused(null);
            }
            return false;
        }
        this.focusRegion = FocusRegion.RECIPES;
        if (this.itemSlotLayer != null) {
            this.itemSlotLayer.clearFocusedItemSlot();
        }
        this.setFocused(this.recipeGrid);
        var rectangle = this.recipeGrid.getFocused().getRectangle();
        Cursor.moveTo(
            rectangle.left() + rectangle.width() / 2.0D,
            rectangle.top() + rectangle.height() / 2.0D
        );
        return true;
    }

    private void returnCraftingGridItemsToInventory() {
        int craftingSlotCount = this.craftingColumns * this.craftingRows;
        int end = Math.min(this.menu.slots.size(), craftingSlotCount + 1);
        boolean changed = false;
        for (int index = 1; index < end; index++) {
            Slot slot = this.menu.slots.get(index);
            if (!slot.isActive() || !slot.hasItem()) {
                continue;
            }
            ((AbstractContainerScreenAccessor)this).javaReforged$invokeSlotClicked(
                    slot,
                    slot.index,
                    0,
                    ContainerInput.QUICK_MOVE
            );
            changed = true;
        }
        if (changed) {
            this.reloadRecipes();
        }
    }

    private void prepareRecipeTabChange() {
        if (this.recipeGrid != null) {
            this.recipeGrid.clearEntryFocus();
        }
        if (this.itemSlotLayer != null) {
            this.itemSlotLayer.clearFocusedItemSlot();
        }
        this.setFocused(null);
        this.focusRegion = FocusRegion.NONE;
        this.returnCraftingGridItemsToInventory();
    }

    private void resetRecipeScroll() {
        if (this.recipeGrid != null) {
            this.recipeGrid.scrollToImmediately(0.0D, 0.0D);
        }
    }

    private void focusRecipeEntryUnderCursor() {
        if (this.recipeGrid == null) {
            return;
        }
        this.focusRegion = FocusRegion.RECIPES;
        RecipeSlotElement recipeSlot = this.recipeGrid.previewedRecipeSlot(
                Cursor.x(),
                Cursor.y(),
                false
        );
        if (recipeSlot == null) {
            return;
        }
        this.recipeGrid.setFocused(recipeSlot);
        this.setFocused(this.recipeGrid);
    }

    private void enterInventoryFocus() {
        this.focusRegion = FocusRegion.INVENTORY;
        if (this.recipeGrid != null) {
            this.recipeGrid.clearEntryFocus();
        }
        this.setFocused(null);
    }

    private Asset iconFor(CraftingCategory category) {
        return switch (category) {
            case BUILDING_BLOCKS -> this.screenData.asset("icon_building_asset");
            case EQUIPMENT_AND_TOOLS -> this.screenData.asset("icon_equipment_asset");
            case RESOURCES_AND_FOOD -> this.screenData.asset("icon_materials_asset");
            case REDSTONE_AND_MECHANISMS -> this.screenData.asset("icon_redstone_asset");
            case DECORATION -> this.screenData.asset("icon_decoration_asset");
        };
    }

    private void renderCraftingTab(
            GuiGraphicsExtractor graphics,
            TabElement tab,
            Asset icon
    ) {
        renderCraftingTabBackground(graphics, tab);
        int iconSize = this.screenData.integer("tab_icon_size");
        Draw.image(
                icon,
                tab.x() + this.screenData.integer("tab_icon_x"),
                tab.y() + this.screenData.integer("tab_icon_y"),
                iconSize,
                iconSize
        ).draw(graphics);
    }

    private void renderCraftingTab(
            GuiGraphicsExtractor graphics,
            TabElement tab,
            ItemStack icon
    ) {
        renderCraftingTabBackground(graphics, tab);
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

    private void renderCraftingTabBackground(GuiGraphicsExtractor graphics, TabElement tab) {
        boolean selected = tab.isSelected();
        Asset background = switch (tab.positionRole()) {
            case SINGLE, FIRST -> this.screenData.asset(
                    selected ? "tab_left_selected_asset" : "tab_left_asset");
            case MIDDLE -> this.screenData.asset(
                    selected ? "tab_center_selected_asset" : "tab_center_asset");
            case LAST -> this.screenData.asset(
                    selected ? "tab_right_selected_asset" : "tab_right_asset");
        };
        String config = UiPresentation.isSmall() ? "" : "fhd";


        Draw.imageFromMeta(
                background,
                tab.x(),
                tab.y(),
                this.screenData.integer("tab_width"),
                this.screenData.integer("tab_height")
        ).config(config).draw(graphics);
    }

    private record CraftingTab(CraftingCategory category, ModCraftingTab mod) {
        private static CraftingTab vanilla(CraftingCategory category) {
            return new CraftingTab(category, null);
        }

        private static CraftingTab mod(ModCraftingTab mod) {
            return new CraftingTab(null, mod);
        }

        private static CraftingTab search() {
            return new CraftingTab(null, null);
        }

        private boolean isSearch() {
            return this.category == null && this.mod == null;
        }
    }

    private static final class ModCraftingTab {
        private final String namespace;
        private final ItemStack icon;
        private final Component title;
        private final Map<Item, Integer> creativeOrder = new LinkedHashMap<>();

        private ModCraftingTab(String namespace, ItemStack icon) {
            this.namespace = namespace;
            this.icon = icon;
            this.title = ModList.get().getModContainerById(namespace)
                    .map(mod -> Component.literal(mod.getModInfo().getDisplayName()))
                    .orElse(Component.literal(namespace));
        }
    }

    private final class CraftingTabGroup implements TabNavigator {
        private final TabBarElement tabs;
        private final Map<TabElement, CraftingTab> tabByWidget =
                new IdentityHashMap<>();
        private final Map<String, ModCraftingTab> modsByNamespace = new LinkedHashMap<>();
        private final List<List<CraftingTab>> pages = new ArrayList<>();
        private CraftingTab selectedTab = CraftingTab.vanilla(CraftingCategory.BUILDING_BLOCKS);
        private int pageIndex;
        private boolean populatingTabs;

        private CraftingTabGroup() {
            this.tabs = new TabBarElement(
                    AbstractCraftingScreen.this.leftPos,
                    AbstractCraftingScreen.this.topPos
                            + AbstractCraftingScreen.this.screenData.integer("tab_y_offset"),
                    AbstractCraftingScreen.this.screenData.integer("tab_width"),
                    AbstractCraftingScreen.this.screenData.integer("tab_height")
            );
            this.tabs.setOrientation(TabBarElement.Orientation.HORIZONTAL);
            this.tabs.setPositionRoleCapacity(Math.max(
                    1,
                    AbstractCraftingScreen.this.screenData.integer("tabs_per_page")
            ));
            this.tabs.setPadding(AbstractCraftingScreen.this.screenData.decimal("tab_padding"));
            this.rebuildPages();
            this.populateTabs(false);
            this.tabs.onSelectionChange(this::onSelectionChange);
        }

        private boolean rebuildPages() {
            Map<String, ModCraftingTab> registeredMods = new LinkedHashMap<>();
            for (CreativeModeTab creativeTab : CreativeModeTabRegistry.getSortedCreativeModeTabs()) {
                if (!creativeTab.shouldDisplay() || !creativeTab.hasAnyItems()) {
                    continue;
                }
                Identifier id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(creativeTab);
                if (id == null || "minecraft".equals(id.getNamespace())) {
                    continue;
                }
                ModCraftingTab mod = registeredMods.computeIfAbsent(
                        id.getNamespace(),
                        namespace -> new ModCraftingTab(namespace, creativeTab.getIconItem())
                );
                for (ItemStack stack : creativeTab.getDisplayItems()) {
                    mod.creativeOrder.putIfAbsent(stack.getItem(), mod.creativeOrder.size());
                }
            }

            Map<String, Set<RecipeDisplayId>> recipesByMod = new HashMap<>();
            if (AbstractCraftingScreen.this.minecraft != null
                    && AbstractCraftingScreen.this.minecraft.player != null) {
                ContextMap displayContext = SlotDisplayContext.fromLevel(
                        AbstractCraftingScreen.this.minecraft.player.level());
                for (RecipeCollection collection : AbstractCraftingScreen.this.minecraft.player
                        .getRecipeBook().getCollections()) {
                    for (RecipeDisplayEntry recipe : collection.getRecipes()) {
                        if (!AbstractCraftingScreen.this.fitsCraftingGrid(recipe.display())) {
                            continue;
                        }
                        for (ItemStack result : recipe.resultItems(displayContext)) {
                            if (result.isEmpty()) {
                                continue;
                            }
                            Identifier id = BuiltInRegistries.ITEM.getKey(result.getItem());
                            if (id != null && registeredMods.containsKey(id.getNamespace())) {
                                recipesByMod.computeIfAbsent(id.getNamespace(), ignored -> new HashSet<>())
                                        .add(recipe.id());
                            }
                        }
                    }
                }
            }
            registeredMods.entrySet().removeIf(entry -> recipesByMod
                    .getOrDefault(entry.getKey(), Set.of()).size() < MIN_MOD_CRAFTING_RECIPES);

            if (!this.pages.isEmpty()
                    && List.copyOf(this.modsByNamespace.keySet())
                    .equals(List.copyOf(registeredMods.keySet()))) {
                return false;
            }

            String previousMod = this.selectedTab.mod() == null
                    ? null : this.selectedTab.mod().namespace;
            this.modsByNamespace.clear();
            this.modsByNamespace.putAll(registeredMods);
            if (previousMod != null) {
                ModCraftingTab updatedMod = this.modsByNamespace.get(previousMod);
                this.selectedTab = updatedMod == null
                        ? CraftingTab.search() : CraftingTab.mod(updatedMod);
            }
            this.pages.clear();
            List<ModCraftingTab> mods = List.copyOf(this.modsByNamespace.values());
            int tabsPerPage = Math.max(
                    this.categories().size() + 1,
                    AbstractCraftingScreen.this.screenData.integer("tabs_per_page")
            );
            int firstPageModTabs = Math.max(0, tabsPerPage - this.categories().size() - 1);
            List<CraftingTab> firstPage = new ArrayList<>();
            for (CraftingCategory category : this.categories()) {
                firstPage.add(CraftingTab.vanilla(category));
            }
            for (int i = 0; i < Math.min(firstPageModTabs, mods.size()); i++) {
                firstPage.add(CraftingTab.mod(mods.get(i)));
            }
            firstPage.add(CraftingTab.search());
            this.pages.add(List.copyOf(firstPage));

            int modTabsPerPage = Math.max(1, tabsPerPage - 1);
            for (int start = firstPageModTabs; start < mods.size(); start += modTabsPerPage) {
                int end = Math.min(start + modTabsPerPage, mods.size());
                List<CraftingTab> page = new ArrayList<>();
                for (int i = start; i < end; i++) {
                    page.add(CraftingTab.mod(mods.get(i)));
                }
                page.add(CraftingTab.search());
                this.pages.add(List.copyOf(page));
            }
            if (!this.selectedTab.isSearch()) {
                for (int i = 0; i < this.pages.size(); i++) {
                    if (this.pages.get(i).contains(this.selectedTab)) {
                        this.pageIndex = i;
                        break;
                    }
                }
            }
            this.pageIndex = Math.min(this.pageIndex, this.pages.size() - 1);
            return true;
        }

        private void refreshPages() {
            if (this.rebuildPages()) {
                this.populateTabs(false);
                this.updatePageButtons();
            }
        }

        private void populateTabs(boolean notifySelectionChange) {
            List<CraftingTab> page = this.pages.get(this.pageIndex);
            CraftingTab preferred = page.contains(this.selectedTab)
                    ? this.selectedTab : page.getFirst();
            this.populatingTabs = true;
            try {
                this.tabs.clearTabs();
                this.tabByWidget.clear();
                for (CraftingTab craftingTab : page) {
                    TabElement tab = this.createTab(craftingTab);
                    this.tabByWidget.put(tab, craftingTab);
                    this.tabs.addTab(tab);
                }
                this.tabs.select(page.indexOf(preferred));
            } finally {
                this.populatingTabs = false;
            }
            if (notifySelectionChange) {
                this.onSelectionChange(this.tabs.selected());
            } else {
                this.selectedTab = preferred;
                AbstractCraftingScreen.this.setSearchBoxVisible(this.isSearchSelected());
            }
        }

        private TabElement createTab(CraftingTab craftingTab) {
            TabElement tab = new TabElement(
                    0,
                    0,
                    AbstractCraftingScreen.this.screenData.integer("tab_width"),
                    AbstractCraftingScreen.this.screenData.integer("tab_height")
            );
            if (craftingTab.mod() != null) {
                tab.setRenderer((graphics, widget, mouseX, mouseY, partialTick) ->
                        renderCraftingTab(graphics, (TabElement)widget, craftingTab.mod().icon));
            } else {
                Asset icon = craftingTab.isSearch()
                        ? AbstractCraftingScreen.this.screenData.asset("icon_search_asset")
                        : iconFor(craftingTab.category());
                tab.setRenderer((graphics, widget, mouseX, mouseY, partialTick) ->
                        renderCraftingTab(graphics, (TabElement)widget, icon));
            }
            return tab;
        }

        private void onSelectionChange(TabElement selectedTab) {
            if (this.populatingTabs) {
                return;
            }
            CraftingTab newSelection = this.tabByWidget.get(selectedTab);
            if (newSelection == null || newSelection.equals(this.selectedTab)) {
                return;
            }
            AbstractCraftingScreen.this.prepareRecipeTabChange();
            this.selectedTab = newSelection;
            AbstractCraftingScreen.this.resetRecipeScroll();
            AbstractCraftingScreen.this.reloadRecipes();
            AbstractCraftingScreen.this.focusRecipeEntryUnderCursor();
            AbstractCraftingScreen.this.setSearchBoxVisible(this.isSearchSelected());
        }

        private boolean changePage(int delta) {
            int nextPage = Math.max(0, Math.min(this.pageIndex + delta, this.pages.size() - 1));
            if (nextPage == this.pageIndex) {
                return false;
            }
            this.pageIndex = nextPage;
            AbstractCraftingScreen.this.resetRecipeScroll();
            this.populateTabs(true);
            this.updatePageButtons();
            return true;
        }

        private void updatePageButtons() {
            boolean multiplePages = this.pages.size() > 1;
            AbstractCraftingScreen.this.previousPageButton.visible = multiplePages;
            AbstractCraftingScreen.this.nextPageButton.visible = multiplePages;
            AbstractCraftingScreen.this.previousPageButton.active = multiplePages && this.pageIndex > 0;
            AbstractCraftingScreen.this.nextPageButton.active = multiplePages
                    && this.pageIndex + 1 < this.pages.size();
        }

        @Override
        public boolean selectPrevious() {
            return this.changePage(-1);
        }

        @Override
        public boolean selectNext() {
            return this.changePage(1);
        }

        private List<CraftingCategory> categories() {
            if (AbstractCraftingScreen.this.craftingColumns == 2) {
                return List.of(
                        CraftingCategory.BUILDING_BLOCKS,
                        CraftingCategory.EQUIPMENT_AND_TOOLS,
                        CraftingCategory.RESOURCES_AND_FOOD,
                        CraftingCategory.DECORATION
                );
            }
            return List.of(
                    CraftingCategory.BUILDING_BLOCKS,
                    CraftingCategory.EQUIPMENT_AND_TOOLS,
                    CraftingCategory.RESOURCES_AND_FOOD,
                    CraftingCategory.REDSTONE_AND_MECHANISMS,
                    CraftingCategory.DECORATION
            );
        }

        private TabBarElement tabs() {
            return this.tabs;
        }

        private CraftingCategory selectedCategory() {
            return this.selectedTab.category();
        }

        private ModCraftingTab selectedMod() {
            return this.selectedTab.mod();
        }

        private boolean hasMod(String namespace) {
            return this.modsByNamespace.containsKey(namespace);
        }

        private boolean isSearchSelected() {
            return this.selectedTab.isSearch();
        }

        private int pageIndex() {
            return this.pageIndex;
        }

        private int pageCount() {
            return this.pages.size();
        }

        private Component selectedTitle() {
            if (this.selectedTab.mod() != null) {
                return this.selectedTab.mod().title;
            }
            return Component.translatable(
                    "crafting_tab.java_reforged."
                            + (this.isSearchSelected()
                            ? "search"
                            : this.selectedTab.category().name().toLowerCase(java.util.Locale.ROOT))
            );
        }
    }

    private enum FocusRegion {
        NONE,
        SEARCH,
        RECIPES,
        INVENTORY
    }

    private static void playTabSelectionSound() {
        Minecraft.getInstance().getSoundManager().play(
            SimpleSoundInstance.forUI(ModSounds.UI_HOVER.get(), 1.0F, 1.0F)
        );
    }
}
