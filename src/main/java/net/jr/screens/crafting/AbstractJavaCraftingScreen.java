package net.jr.screens.crafting;

import net.jr.api.client.render.Draw;
import net.jr.api.client.resource.Asset;
import net.jr.client.components.navigation.UiAction;
import net.jr.client.components.navigation.UiActionHandler;
import net.jr.client.components.elements.TextElement;
import net.jr.client.components.widgets.EditBoxElement;
import net.jr.client.components.widgets.TabBarElement;
import net.jr.client.components.widgets.TabElement;
import net.jr.client.components.widgets.recipe.RecipeEntryView;
import net.jr.client.components.widgets.recipe.RecipeGridElement;
import net.jr.client.components.widgets.recipe.RecipeSlotElement;
import net.jr.client.components.widgets.recipe.RecipeVariantView;
import net.jr.client.ui.navigation.UiInputBinding;
import net.jr.client.ui.cursor.CursorMagnetTarget;
import net.jr.client.input.gamepad.GamepadDigitalInput;
import net.jr.client.sound.action.InputActionSounds;
import net.jr.client.ui.container.actions.ContainerSlotFocusController;
import net.jr.client.ui.container.slots.SlotGrid;
import net.jr.client.ui.container.slots.SlotGroup;
import net.jr.client.ui.container.slots.SlotMaps;
import net.jr.client.ui.container.slots.VanillaSlotLayer;
import net.jr.client.ui.container.slots.CustomSlotLayout;
import net.jr.client.ui.navigation.FocusCursorVisibilityHost;
import net.jr.client.ui.navigation.tabs.TabNavigationController;
import net.jr.client.ui.navigation.tabs.TabNavigationHost;
import net.jr.client.ui.hint.provider.RecipeControlHintHost;
import net.jr.mixin.accessors.AbstractContainerScreenAccessor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractJavaCraftingScreen<M extends AbstractContainerMenu>
        extends AbstractContainerScreen<M>
        implements UiActionHandler, CustomSlotLayout, TabNavigationHost, FocusCursorVisibilityHost,
        RecipeControlHintHost {

    private static final int CRAFTING_TAB_WIDTH = 32;
    private static final int CRAFTING_TAB_HEIGHT = 27;
    private static final int CRAFTING_TAB_Y_OFFSET = -23;
    private static final int CRAFTING_TAB_ICON_SIZE = 18;
    private static final int CRAFTING_TAB_ICON_OFFSET_X = 7;
    private static final int CRAFTING_TAB_ICON_OFFSET_Y = 4;

    private static final Asset TAB_LEFT =
            Asset.MC("textures/gui/sprites/container/crafting_menu/tab_left.png");
    private static final Asset TAB_LEFT_SELECTED =
            Asset.MC("textures/gui/sprites/container/crafting_menu/tab_left_selected.png");
    private static final Asset TAB_CENTER =
            Asset.MC("textures/gui/sprites/container/crafting_menu/tab_center.png");
    private static final Asset TAB_CENTER_SELECTED =
            Asset.MC("textures/gui/sprites/container/crafting_menu/tab_center_selected.png");
    private static final Asset SCROLLER =
            Asset.MC("textures/gui/sprites/container/crafting_menu/scroller.png");
    private static final Asset SCROLLER_DISABLED =
            Asset.MC("textures/gui/sprites/container/crafting_menu/scroller_disabled.png");
    private static final Asset SEARCH_BOX =
            Asset.MC("textures/gui/sprites/widgets/text_edit_box.png");

    private static final Asset ICON_BUILDING_BLOCKS =
            Asset.MC("textures/gui/sprites/container/crafting_menu/icons/building_blocks.png");
    private static final Asset ICON_EQUIPMENT =
            Asset.MC("textures/gui/sprites/container/crafting_menu/icons/equipment.png");
    private static final Asset ICON_MATERIALS =
            Asset.MC("textures/gui/sprites/container/crafting_menu/icons/materials.png");
    private static final Asset ICON_REDSTONE_AND_MECHANISMS =
            Asset.MC("textures/gui/sprites/container/crafting_menu/icons/redstone_and_mechanisms.png");
    private static final Asset ICON_DECORATION =
            Asset.MC("textures/gui/sprites/container/crafting_menu/icons/decoration.png");
    private static final Asset ICON_SEARCH =
            Asset.MC("textures/gui/sprites/container/crafting_menu/icons/search.png");

    private final Asset background;
    private final int craftingColumns;
    private final int craftingRows;
    private final CraftingScreenLayout layout;
    private final CraftingRecipePreviewRenderer recipePreviewRenderer;
    private final TabNavigationController tabNavigation =
            new TabNavigationController();
    private CraftingTabGroup craftingTabGroup;
    private FocusRegion focusRegion = FocusRegion.RECIPES;
    private boolean pendingDirectCraft;
    private int pendingCraftStateId = -1;

    protected VanillaSlotLayer slotLayer;
    protected SlotGrid slotGrid;
    protected RecipeGridElement recipeGrid;
    protected TabBarElement recipeTabs;
    private TextElement craftingTitleText;
    private TextElement inventoryTitleText;
    private TextElement tabTitleText;
    private EditBoxElement searchEditBox;

    protected AbstractJavaCraftingScreen(
            M menu,
            Inventory inventory,
            Component title,
            Asset background,
            int imageWidth,
            int imageHeight,
            int craftingColumns,
            int craftingRows,
            CraftingScreenLayout layout
    ) {
        super(menu, inventory, title, imageWidth, imageHeight);
        this.background = background;
        this.craftingColumns = craftingColumns;
        this.craftingRows = craftingRows;
        this.layout = layout;
        this.recipePreviewRenderer = new CraftingRecipePreviewRenderer(
                craftingColumns,
                craftingRows,
                layout.craftingGridGroup(),
                layout.craftingResultGroup()
        );
    }

    @Override
    protected void init() {
        super.init();
        this.topPos += 12;
        this.slotLayer = new VanillaSlotLayer(this, this.menu, this.font, 14);
        this.slotGrid = this.createSlotGrid();

        this.recipeGrid = new RecipeGridElement(
                this.leftPos + 10,
                this.topPos + 23,
                this.layout.recipeColumns() * this.layout.recipeCellSize(),
                this.layout.recipeRows() * this.layout.recipeCellSize(),
                this.layout.recipeRows(),
                this.layout.recipeColumns()
        );
        this.configureRecipeGrid(this.recipeGrid);
        this.recipeGrid.onCraft(this::craftRecipe);

        this.craftingTabGroup = new CraftingTabGroup();
        this.recipeTabs = this.craftingTabGroup.tabs();
        this.createTexts();
        this.createSearchBox();
        this.tabNavigation.clear();
        this.tabNavigation.bind(
                this.recipeTabs,
                UiInputBinding.gamepad(GamepadDigitalInput.BUMPER_LEFT),
                UiInputBinding.gamepad(GamepadDigitalInput.BUMPER_RIGHT),
                Component.translatable("hint.container.java_reforged.change_tab"),
                InputActionSounds::playFocus
        );

        this.addRenderableWidget(this.recipeGrid);
        this.addRenderableWidget(this.recipeTabs);
        this.addRenderableWidget(this.searchEditBox);
        this.reloadRecipes();
        this.enterRecipeFocus(false);
    }

    private void createSearchBox() {
        int searchX = this.craftingColumns == 3 ? 84 : 78;
        this.searchEditBox = new EditBoxElement(
                this.leftPos + searchX,
                this.topPos + 5,
                114,
                13,
                Component.translatable("crafting_tab.java_reforged.search_placeholder")
        );
        this.searchEditBox.setFont(this.font);
        this.searchEditBox.setHint(
                Component.translatable("crafting_tab.java_reforged.search_placeholder")
        );
        this.searchEditBox.setMaxLength(64);
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

    private void configureRecipeGrid(RecipeGridElement grid) {
        grid.setCellSize(this.layout.recipeCellSize(), this.layout.recipeCellSize());
        grid.setAutoPlaceScrollbar(false);
        grid.setFixedScrollbarHandleSize(15.0F);
        grid.scrollbar().setX(this.leftPos + 17
                + this.layout.recipeColumns() * this.layout.recipeCellSize());
        grid.scrollbar().setY(this.topPos + 20);
        grid.scrollbar().setSize(
                9,
                this.layout.recipeRows() * this.layout.recipeCellSize() + 4
        );
        grid.scrollbar().setRenderer((graphics, ignored, mouseX, mouseY, partialTick) -> {
            boolean scrollable = grid.hasScrollableOverflow();
            grid.scrollbar().setActive(scrollable);
            Draw.image(
                    scrollable ? SCROLLER : SCROLLER_DISABLED,
                    Math.round(grid.scrollbar().handleX()),
                    Math.round(grid.scrollbar().handleY()),
                    12,
                    15
            ).draw(graphics);
        });
    }

    private void createTexts() {
        this.craftingTitleText = this.createText(
                this.title,
                this.layout.craftingTitleX(),
                this.layout.craftingTitleY(),
                Draw.TextAlign.LEFT
        );
        this.craftingTitleText.setScrolling(
                this.craftingColumns == 3 ? 116.0F : 98.0F,
                TextElement.ScrollType.PING_PONG
        );
        this.craftingTitleText.setScrollVelocity(0.005F);
        this.craftingTitleText.setScrollDelayMs(500L);
        this.inventoryTitleText = this.createText(
                Component.translatable("container.inventory"),
                this.layout.inventoryTitleX(),
                this.layout.inventoryTitleY(),
                Draw.TextAlign.LEFT
        );
        this.tabTitleText = this.createText(
                this.craftingTabGroup.selectedTitle(),
                this.layout.tabTitleX(),
                this.layout.tabTitleY(),
                Draw.TextAlign.CENTER
        );
    }

    private TextElement createText(Component text, int relativeX, int relativeY, Draw.TextAlign align) {
        TextElement element = new TextElement(
                text,
                this.leftPos + relativeX,
                this.topPos + relativeY,
                null
        );
        element.setColor(0xFF404040);
        element.setAlign(align);
        element.setScale(0.8F);
        return element;
    }

    private void renderTexts(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.craftingTitleText == null || this.inventoryTitleText == null || this.tabTitleText == null) {
            return;
        }
        RecipeVariantView targetedRecipe = this.recipeGrid == null
                ? null
                : this.recipeGrid.previewedRecipe(
                        mouseX,
                        mouseY,
                        this.focusRegion == FocusRegion.RECIPES
                );
        this.craftingTitleText.setText(
                targetedRecipe == null || targetedRecipe.icon().isEmpty()
                        ? Component.empty()
                        : targetedRecipe.icon().getHoverName()
        );
        this.tabTitleText.setText(this.craftingTabGroup.selectedTitle());
        this.craftingTitleText.draw(graphics);
        this.inventoryTitleText.draw(graphics);
        if (!this.craftingTabGroup.isSearchSelected()) {
            this.tabTitleText.draw(graphics);
        }
    }

    protected abstract SlotGrid createSlotGrid();

    @Override
    public final SlotGrid slotGrid() {
        return this.slotGrid;
    }

    @Override
    public final VanillaSlotLayer slotLayer() {
        return this.slotLayer;
    }

    @Override
    public final CursorMagnetTarget cursorMagnetTargetAt(double guiX, double guiY) {
        CursorMagnetTarget recipeTarget = this.recipeGrid == null
                ? null
                : this.recipeGrid.cursorMagnetTargetAt(guiX, guiY);
        return recipeTarget != null
                ? recipeTarget
                : CustomSlotLayout.super.cursorMagnetTargetAt(guiX, guiY);
    }

    @Override
    public final TabNavigationController tabNavigationController() {
        return this.tabNavigation;
    }

    @Override
    public final boolean allowsSlotFocusNavigation() {
        return this.focusRegion == FocusRegion.INVENTORY;
    }

    @Override
    public final boolean seedFocusFromPointer(double guiX, double guiY) {
        Slot inventorySlot = this.slotLayer == null || this.slotGrid == null
                ? null
                : this.slotLayer.getSlotAt(this.slotGrid, guiX, guiY);
        if (inventorySlot != null) {
            this.enterInventoryFocus();
            this.slotLayer.setFocusedSlot(inventorySlot);
            return true;
        }

        RecipeSlotElement recipeSlot = this.recipeGrid == null
                ? null
                : this.recipeGrid.previewedRecipeSlot(guiX, guiY, false);
        if (recipeSlot == null) {
            return false;
        }

        this.focusRegion = FocusRegion.RECIPES;
        if (this.slotLayer != null) {
            this.slotLayer.clearFocusedSlot();
        }
        this.setFocused(this.recipeGrid);
        this.recipeGrid.setFocused(recipeSlot);
        return true;
    }

    @Override
    public final void clearFocusForPointer() {
        this.focusRegion = FocusRegion.NONE;
        if (this.recipeGrid != null) {
            this.recipeGrid.clearEntryFocus();
        }
        if (this.slotLayer != null) {
            this.slotLayer.clearFocusedSlot();
        }
    }

    @Override
    public final boolean shouldShowCursorDuringFocus() {
        return this.focusRegion == FocusRegion.RECIPES;
    }

    @Override
    public final boolean handleSlotNavigationBoundary(int keyCode) {
        if (this.focusRegion == FocusRegion.INVENTORY && keyCode == GLFW.GLFW_KEY_UP) {
            this.enterRecipeFocus(true);
            return true;
        }
        return false;
    }

    protected final SlotGrid craftingTableSlotGrid(
            int craftingX,
            int craftingY,
            int resultX,
            int resultY,
            int inventoryX,
            int inventoryY,
            int hotbarX,
            int hotbarY
    ) {
        return new SlotGrid(SlotMaps.CRAFTING_TABLE)
                .grid(SlotGroup.CRAFTING_GRID_3X3, craftingX, craftingY, 3, 18, 18, 18, 18)
                .point(SlotGroup.CRAFTING_RESULT, 0, resultX, resultY, 26, 26, 24)
                .grid(SlotGroup.PLAYER_INVENTORY, inventoryX, inventoryY, 9, 14, 14, 14, 14)
                .grid(SlotGroup.PLAYER_HOTBAR, hotbarX, hotbarY, 9, 14, 14, 14, 14);
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
        boolean groupWalls = selectedCategory == null
                || selectedCategory == CraftingCategory.BUILDING_BLOCKS;

        for (RecipeCollection collection : this.minecraft.player.getRecipeBook().getCollections()) {
            collection.selectRecipes(contents, this::fitsCraftingGrid);
            List<RecipeDisplayEntry> variants = collection.getRecipes().stream()
                    .filter(entry -> this.fitsCraftingGrid(entry.display()))
                    .filter(entry -> this.matchesSelectedCraftingCategory(entry, displayContext))
                    .toList();
            if (variants.isEmpty()) {
                continue;
            }

            List<RecipeVariantView> variantViews = variants.stream()
                    .map(entry -> this.createVariantView(collection, entry, displayContext))
                    .toList();

            if (groupWalls) {
                List<RecipeVariantView> wallVariants = variantViews.stream()
                        .filter(variant -> CraftingRecipeClassifier.isWall(variant.icon()))
                        .toList();
                if (!wallVariants.isEmpty()) {
                    if (wallCollection == null) {
                        wallCollection = collection;
                    }
                    groupedWallVariants.addAll(wallVariants);
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
        if (selectedCategory == null) {
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

        ItemStack result = recipe.resultItems(displayContext).stream()
                .findFirst()
                .orElse(ItemStack.EMPTY);
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
        Draw.imageFromMeta(this.background, this.leftPos, this.topPos, this.imageWidth, this.imageHeight).draw(graphics);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.renderTexts(graphics, mouseX, mouseY);
        this.recipeGrid.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.recipeTabs.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (this.searchEditBox != null) {
            this.searchEditBox.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
        this.slotLayer.updateHovered(this.slotGrid, mouseX, mouseY);
        this.slotLayer.renderSlotMap(graphics, this.slotGrid);
        this.renderRecipePreview(graphics, mouseX, mouseY);
        this.slotLayer.renderHoveredSlot(graphics, this.slotGrid);
        this.slotLayer.renderCarriedItem(graphics, mouseX, mouseY);
        this.slotLayer.renderTooltip(graphics, this.slotGrid, mouseX, mouseY);
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
                this.slotGrid,
                this.menu,
                this.minecraft.player.getInventory()
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
        if (this.searchEditBox != null && this.searchEditBox.mouseClicked(event, doubleClick)) {
            this.setFocused(this.searchEditBox);
            return true;
        }
        if (this.recipeTabs.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (this.recipeGrid.mouseClicked(event, doubleClick)) {
            this.enterRecipeFocus(false);
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
            this.enterInventoryFocus();
        }
        return handled;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean searchHandled = this.searchEditBox != null && this.searchEditBox.mouseReleased(event);
        boolean widgetHandled = this.recipeGrid.mouseReleased(event);
        widgetHandled = this.recipeTabs.mouseReleased(event) || widgetHandled;
        boolean slotHandled = this.slotLayer.mouseReleased(
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
        return searchHandled || widgetHandled || slotHandled;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.searchEditBox != null && this.searchEditBox.mouseDragged(event, dragX, dragY)) {
            return true;
        }
        if (this.recipeGrid.mouseDragged(event, dragX, dragY)) {
            return true;
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return this.recipeGrid.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.searchEditBox != null && this.searchEditBox.keyPressed(event)) {
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE && this.recipeGrid.closeExpansion()) {
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
        return this.slotLayer.keyPressed(this.slotGrid, event.key(), event.scancode()) || super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return this.searchEditBox != null && this.searchEditBox.charTyped(event)
                || super.charTyped(event);
    }

    @Override
    public boolean handleUiAction(UiAction action) {
        if (this.focusRegion != FocusRegion.RECIPES) {
            return false;
        }
        if (this.recipeGrid.handleUiAction(action)) {
            return true;
        }
        if (action == UiAction.MOVE_DOWN) {
            this.enterInventoryFocus();
            return ContainerSlotFocusController.moveFocus(this, GLFW.GLFW_KEY_DOWN);
        }

        return action.isDirectional();
    }

    private void enterRecipeFocus(boolean focusLast) {
        this.focusRegion = FocusRegion.RECIPES;
        if (this.slotLayer != null) {
            this.slotLayer.clearFocusedSlot();
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

    private void enterInventoryFocus() {
        this.focusRegion = FocusRegion.INVENTORY;
        if (this.recipeGrid != null) {
            this.recipeGrid.clearEntryFocus();
        }
        this.setFocused(null);
    }

    private static Asset iconFor(CraftingCategory category) {
        return switch (category) {
            case BUILDING_BLOCKS -> ICON_BUILDING_BLOCKS;
            case EQUIPMENT_AND_TOOLS -> ICON_EQUIPMENT;
            case RESOURCES_AND_FOOD -> ICON_MATERIALS;
            case REDSTONE_AND_MECHANISMS -> ICON_REDSTONE_AND_MECHANISMS;
            case DECORATION -> ICON_DECORATION;
        };
    }

    private static void renderCraftingTab(
            GuiGraphicsExtractor graphics,
            TabElement tab,
            Asset icon
    ) {
        boolean leftCorner = tab.positionRole() == TabElement.PositionRole.FIRST
                || tab.positionRole() == TabElement.PositionRole.SINGLE;
        boolean selected = tab.isSelected() || tab.isPressed();
        Asset background = leftCorner
                ? selected ? TAB_LEFT_SELECTED : TAB_LEFT
                : selected ? TAB_CENTER_SELECTED : TAB_CENTER;

        Draw.image(
                background,
                tab.x(),
                tab.y(),
                CRAFTING_TAB_WIDTH,
                CRAFTING_TAB_HEIGHT
        ).draw(graphics);
        Draw.image(
                icon,
                tab.x() + CRAFTING_TAB_ICON_OFFSET_X,
                tab.y() + CRAFTING_TAB_ICON_OFFSET_Y,
                CRAFTING_TAB_ICON_SIZE,
                CRAFTING_TAB_ICON_SIZE
        ).draw(graphics);
    }

    private final class CraftingTabGroup {
        private final TabBarElement tabs;
        private final Map<TabElement, CraftingCategory> categoryByTab =
                new IdentityHashMap<>();
        private CraftingCategory selectedCategory =
                CraftingCategory.BUILDING_BLOCKS;

        private CraftingTabGroup() {
            this.tabs = new TabBarElement(
                    AbstractJavaCraftingScreen.this.leftPos,
                    AbstractJavaCraftingScreen.this.topPos + CRAFTING_TAB_Y_OFFSET,
                    CRAFTING_TAB_WIDTH,
                    CRAFTING_TAB_HEIGHT
            );
            this.tabs.setOrientation(TabBarElement.Orientation.HORIZONTAL);
            this.tabs.setPadding(-1.0F);

            for (CraftingCategory category : this.categories()) {
                TabElement tab = this.createTab(iconFor(category));
                this.categoryByTab.put(tab, category);
                this.tabs.addTab(tab);
            }
            this.tabs.addTab(this.createTab(ICON_SEARCH));
            this.tabs.onSelectionChange(this::onSelectionChange);
        }

        private TabElement createTab(Asset icon) {
            TabElement tab = new TabElement(
                    0,
                    0,
                    CRAFTING_TAB_WIDTH,
                    CRAFTING_TAB_HEIGHT
            );
            tab.setRenderer((graphics, widget, mouseX, mouseY, partialTick) ->
                    renderCraftingTab(graphics, (TabElement)widget, icon)
            );
            return tab;
        }

        private void onSelectionChange(TabElement selectedTab) {
            this.selectedCategory = this.categoryByTab.get(selectedTab);

            AbstractJavaCraftingScreen.this.focusRegion = FocusRegion.RECIPES;
            if (AbstractJavaCraftingScreen.this.slotLayer != null) {
                AbstractJavaCraftingScreen.this.slotLayer.clearFocusedSlot();
            }
            if (AbstractJavaCraftingScreen.this.recipeGrid != null) {
                AbstractJavaCraftingScreen.this.recipeGrid.clearEntryFocus();
            }
            AbstractJavaCraftingScreen.this.setFocused(null);
            AbstractJavaCraftingScreen.this.reloadRecipes();

            if (AbstractJavaCraftingScreen.this.recipeGrid != null) {
                AbstractJavaCraftingScreen.this.recipeGrid.focusFirstEntry();
            }
            AbstractJavaCraftingScreen.this.enterRecipeFocus(false);
            AbstractJavaCraftingScreen.this.setSearchBoxVisible(this.isSearchSelected());
        }

        private List<CraftingCategory> categories() {
            if (AbstractJavaCraftingScreen.this.craftingColumns == 2) {
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
            return this.selectedCategory;
        }

        private boolean isSearchSelected() {
            return this.selectedCategory == null;
        }

        private Component selectedTitle() {
            return Component.translatable(
                    "crafting_tab.java_reforged."
                            + (this.selectedCategory == null
                            ? "search"
                            : this.selectedCategory.name().toLowerCase(java.util.Locale.ROOT))
            );
        }
    }

    protected record CraftingScreenLayout(
            SlotGroup craftingGridGroup,
            SlotGroup craftingResultGroup,
            int recipeColumns,
            int recipeRows,
            int recipeCellSize,
            int craftingTitleX,
            int craftingTitleY,
            int inventoryTitleX,
            int inventoryTitleY,
            int tabTitleX,
            int tabTitleY
    ) {}

    private enum FocusRegion {
        NONE,
        RECIPES,
        INVENTORY
    }
}
