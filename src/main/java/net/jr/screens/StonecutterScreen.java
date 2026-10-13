package net.jr.screens;

import java.util.ArrayList;
import java.util.List;

import net.jr.api.client.render.Draw;
import net.jr.api.client.resource.Asset;
import net.jr.api.client.ui.Cursor;
import net.jr.client.components.navigation.UiAction;
import net.jr.client.components.navigation.UiActionHandler;
import net.jr.client.components.widgets.JavaBaseWidget;
import net.jr.client.components.widgets.ScrollbarElement;
import net.jr.client.components.widgets.item.AbstractItemGridElement;
import net.jr.client.ui.container.slots.CustomItemSlotLayout;
import net.jr.client.ui.container.slots.ItemSlotGrid;
import net.jr.client.ui.container.slots.ItemSlotGroup;
import net.jr.client.ui.container.slots.ItemSlotHighlightRenderer;
import net.jr.client.ui.container.slots.ItemSlotMap;
import net.jr.client.ui.container.slots.ItemSlotSlice;
import net.jr.client.ui.container.slots.VanillaItemSlotLayer;
import net.jr.client.ui.cursor.CursorMagnetTarget;
import net.jr.client.ui.navigation.FocusCursorVisibilityHost;
import net.jr.client.ui.navigation.UiInputModeController;
import net.jr.mixin.accessors.AbstractContainerScreenAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import org.lwjgl.glfw.GLFW;

public final class StonecutterScreen extends AbstractContainerScreen<StonecutterMenu>
        implements UiActionHandler, CustomItemSlotLayout, FocusCursorVisibilityHost {
    private static final Asset BACKGROUND =
            Asset.MC("textures/gui/container/stonecutter.png");
    private static final Asset RECIPE_SLOT =
            Asset.MC("textures/gui/sprites/container/crafting_menu/recipe_slot_available.png");
    private static final Asset SCROLLER =
            Asset.MC("textures/gui/sprites/container/stonecutter/scroller.png");
    private static final Asset SCROLLER_DISABLED =
            Asset.MC("textures/gui/sprites/container/stonecutter/scroller_disabled.png");
    private static final ItemSlotMap SLOT_MAP = ItemSlotMap.fromSlices(
            new ItemSlotSlice(ItemSlotGroup.STONECUTTER_INPUT, new int[] {0}),
            new ItemSlotSlice(ItemSlotGroup.PLAYER_INVENTORY, new int[] {
                    2, 3, 4, 5, 6, 7, 8, 9, 10,
                    11, 12, 13, 14, 15, 16, 17, 18, 19,
                    20, 21, 22, 23, 24, 25, 26, 27, 28
            }),
            new ItemSlotSlice(ItemSlotGroup.PLAYER_HOTBAR, new int[] {
                    29, 30, 31, 32, 33, 34, 35, 36, 37
            })
    );

    private VanillaItemSlotLayer itemSlotLayer;
    private ItemSlotGrid itemSlotGrid;
    private RecipeGrid recipeGrid;
    private FocusRegion focusRegion = FocusRegion.RECIPES;
    private PendingCraft pendingCraft;

    public StonecutterScreen(StonecutterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.menu.registerUpdateListener(this::recipesChanged);
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        this.itemSlotLayer = new VanillaItemSlotLayer(this, this.menu, this.font, 18);
        this.itemSlotGrid = new ItemSlotGrid(SLOT_MAP)
                .point(ItemSlotGroup.STONECUTTER_INPUT, 0, this.leftPos + 18, this.topPos + 32, 18, 18, 16)
                .grid(ItemSlotGroup.PLAYER_INVENTORY, this.leftPos + 7, this.topPos + 83, 9, 18, 18, 16)
                .grid(ItemSlotGroup.PLAYER_HOTBAR, this.leftPos + 7, this.topPos + 141, 9, 18, 18, 16);
        this.recipeGrid = new RecipeGrid(this.leftPos + 52, this.topPos + 15);
        this.recipeGrid.setAutoPlaceScrollbar(false);
        this.recipeGrid.setFixedScrollbarHandleSize(15.0F);
        ScrollbarElement scrollbar = this.recipeGrid.scrollbar();
        scrollbar.setBounds(this.leftPos + 152, this.topPos + 15, 12, 54);
        scrollbar.setTrack(0.0F, 0.0F, 54.0F, 12.0F);
        scrollbar.setRenderer((graphics, widget, mouseX, mouseY, partialTick) -> {
            ScrollbarElement bar = (ScrollbarElement)widget;
            boolean active = this.recipeGrid.hasScrollableOverflow();
            Asset texture = active ? SCROLLER : SCROLLER_DISABLED;
            Draw.imageFromMeta(texture, bar.handleX(), active ? bar.handleY() : bar.getY(), 12, 15)
                    .draw(graphics);
        });
        this.addRenderableWidget(this.recipeGrid);
        this.recipesChanged();
        this.focusFirstRecipeAndMoveCursor();
    }

    private void recipesChanged() {
        if (this.recipeGrid == null || this.minecraft == null || this.minecraft.level == null) {
            return;
        }
        ContextMap context = SlotDisplayContext.fromLevel(this.minecraft.level);
        List<RecipeOption> recipes = new ArrayList<>();
        var visibleRecipes = this.menu.getVisibleRecipes();
        for (int index = 0; index < visibleRecipes.size(); index++) {
            ItemStack icon = visibleRecipes.entries().get(index).recipe().optionDisplay()
                    .resolveForFirstStack(context);
            recipes.add(new RecipeOption(index, icon));
        }
        this.recipeGrid.setEntries(recipes);
        this.recipeGrid.scrollbar().active = this.recipeGrid.hasScrollableOverflow();
        if (recipes.isEmpty() && this.focusRegion == FocusRegion.RECIPES) {
            this.recipeGrid.clearEntryFocus();
            this.setFocused(null);
        }
    }

    private boolean selectRecipe(RecipeOption recipe) {
        if (this.minecraft == null || this.minecraft.player == null || this.minecraft.gameMode == null
                || recipe.index < 0 || recipe.index >= this.menu.getNumberOfVisibleRecipes()) {
            return false;
        }
        if (this.menu.getSelectedRecipeIndex() == recipe.index) {
            return true;
        }
        if (!this.menu.clickMenuButton(this.minecraft.player, recipe.index)) {
            return false;
        }
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, recipe.index);
        return true;
    }

    private boolean craftOnce(RecipeOption recipe) {
        return this.requestCraft(recipe, CraftAmount.ONE);
    }

    private boolean craftStack(RecipeOption recipe) {
        return this.requestCraft(recipe, CraftAmount.STACK);
    }

    private boolean requestCraft(RecipeOption recipe, CraftAmount amount) {
        if (!this.menu.getCarried().isEmpty()) {
            return false;
        }
        if (this.pendingCraft != null && this.pendingCraft.recipeIndex == recipe.index) {
            this.pendingCraft = new PendingCraft(
                    recipe.index,
                    amount,
                    this.pendingCraft.stateId
            );
            return true;
        }
        if (this.menu.getSelectedRecipeIndex() != recipe.index) {
            int stateId = this.menu.getStateId();
            if (!this.selectRecipe(recipe)) {
                return false;
            }
            this.pendingCraft = new PendingCraft(recipe.index, amount, stateId);
            return true;
        }
        return this.finishCraft(amount);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.pendingCraft == null || this.menu.getStateId() == this.pendingCraft.stateId) {
            return;
        }
        PendingCraft pending = this.pendingCraft;
        this.pendingCraft = null;
        if (this.menu.getSelectedRecipeIndex() == pending.recipeIndex) {
            this.finishCraft(pending.amount);
        }
    }

    private boolean finishCraft(CraftAmount amount) {
        Slot result = this.menu.slots.get(StonecutterMenu.RESULT_SLOT);
        if (!result.hasItem() || !this.hasInventoryCapacity(result.getItem())) {
            return false;
        }
        if (amount == CraftAmount.STACK) {
            this.clickSlot(result, ContainerInput.QUICK_MOVE);
            return true;
        }
        this.clickSlot(result, ContainerInput.PICKUP);
        this.placeCarriedInInventory();
        return this.menu.getCarried().isEmpty();
    }

    public boolean craftTargetedRecipeStack() {
        if (this.recipeGrid == null) {
            return false;
        }
        RecipeOption recipe = this.recipeGrid.recipeAt(
                Cursor.x(),
                Cursor.y(),
                this.focusRegion == FocusRegion.RECIPES && UiInputModeController.isFocusNavigationActive()
        );
        return recipe != null && this.craftStack(recipe);
    }

    private boolean hasInventoryCapacity(ItemStack stack) {
        int remaining = stack.getCount();
        for (int index = this.menu.slots.size() - 1; index >= 2 && remaining > 0; index--) {
            Slot slot = this.menu.slots.get(index);
            if (!slot.isActive() || !slot.mayPlace(stack)) {
                continue;
            }
            ItemStack existing = slot.getItem();
            if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack)) {
                continue;
            }
            int maximum = Math.min(slot.getMaxStackSize(stack), stack.getMaxStackSize());
            remaining -= Math.max(0, maximum - existing.getCount());
        }
        return remaining <= 0;
    }

    private void placeCarriedInInventory() {
        for (int index = this.menu.slots.size() - 1; index >= 2 && !this.menu.getCarried().isEmpty(); index--) {
            Slot slot = this.menu.slots.get(index);
            ItemStack carried = this.menu.getCarried();
            ItemStack existing = slot.getItem();
            if (slot.isActive() && slot.mayPlace(carried)
                    && (existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, carried))) {
                this.clickSlot(slot, ContainerInput.PICKUP);
            }
        }
    }

    private void clickSlot(Slot slot, ContainerInput input) {
        ((AbstractContainerScreenAccessor)(Object)this).javaReforged$invokeSlotClicked(
                slot,
                slot.index,
                0,
                input
        );
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Draw.imageFromMeta(BACKGROUND, this.leftPos, this.topPos, this.imageWidth, this.imageHeight)
                .draw(graphics);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Draw.text(this.title, this.leftPos + this.titleLabelX, this.topPos + this.titleLabelY)
                .color(0xFF404040)
                .draw(graphics);
        Draw.text(this.playerInventoryTitle, this.leftPos + this.inventoryLabelX, this.topPos + this.inventoryLabelY)
                .color(0xFF404040)
                .draw(graphics);
        this.recipeGrid.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.itemSlotLayer.updateHovered(this.itemSlotGrid, mouseX, mouseY);
        this.itemSlotLayer.renderItemSlotMap(graphics, this.itemSlotGrid);
        this.itemSlotLayer.renderHoveredItemSlot(graphics, this.itemSlotGrid);
        this.itemSlotLayer.renderCarriedItem(graphics, mouseX, mouseY);
        this.itemSlotLayer.renderTooltip(graphics, this.itemSlotGrid, mouseX, mouseY);
        RecipeCell recipe = this.recipeGrid.recipeCellAt(
                mouseX,
                mouseY,
                this.focusRegion == FocusRegion.RECIPES
        );
        if (recipe != null && this.menu.getCarried().isEmpty() && !recipe.recipe.icon.isEmpty()) {
            int tooltipX = recipe.isMouseOver(mouseX, mouseY)
                    ? mouseX
                    : recipe.getX() + recipe.getWidth() / 2;
            int tooltipY = recipe.isMouseOver(mouseX, mouseY)
                    ? mouseY
                    : recipe.getY() + recipe.getHeight() / 2;
            graphics.setTooltipForNextFrame(this.font, recipe.recipe.icon, tooltipX, tooltipY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.recipeGrid.mouseClicked(event, doubleClick)) {
            this.enterRecipeFocus();
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
            this.enterItemSlotFocus();
        }
        return handled;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean recipeHandled = this.recipeGrid.mouseReleased(event);
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
        return recipeHandled || slotHandled;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return this.recipeGrid.mouseDragged(event, dragX, dragY)
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
        return this.recipeGrid.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
                || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        UiAction navigation = switch (event.key()) {
            case GLFW.GLFW_KEY_LEFT -> UiAction.MOVE_LEFT;
            case GLFW.GLFW_KEY_RIGHT -> UiAction.MOVE_RIGHT;
            case GLFW.GLFW_KEY_UP -> UiAction.MOVE_UP;
            case GLFW.GLFW_KEY_DOWN -> UiAction.MOVE_DOWN;
            default -> null;
        };
        if (this.focusRegion == FocusRegion.RECIPES && navigation != null && this.handleUiAction(navigation)) {
            return true;
        }
        return this.itemSlotLayer.keyPressed(this.itemSlotGrid, event.key(), event.scancode())
                || super.keyPressed(event);
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
            this.enterItemSlotFocus();
            this.focusItemSlot(2);
            return true;
        }
        if (action == UiAction.MOVE_LEFT) {
            this.enterItemSlotFocus();
            this.focusItemSlot(0);
            return true;
        }
        return action.isDirectional();
    }

    private void enterRecipeFocus() {
        this.focusRegion = FocusRegion.RECIPES;
        this.itemSlotLayer.clearFocusedItemSlot();
        this.setFocused(this.recipeGrid);
        if (this.recipeGrid.getFocused() == null) {
            this.recipeGrid.focusFirstEntry();
        }
    }

    private void enterItemSlotFocus() {
        this.focusRegion = FocusRegion.SLOTS;
        this.recipeGrid.clearEntryFocus();
        this.setFocused(null);
    }

    private void focusItemSlot(int index) {
        Slot slot = this.menu.slots.get(index);
        this.itemSlotLayer.setFocusedItemSlot(slot);
        var position = this.itemSlotGrid.get(index);
        Cursor.moveTo(
                position.x() + position.width() / 2.0D,
                position.y() + position.height() / 2.0D
        );
    }

    private boolean focusFirstRecipeAndMoveCursor() {
        if (this.recipeGrid == null || !this.recipeGrid.focusFirstEntry()) {
            return false;
        }
        this.enterRecipeFocus();
        var rectangle = this.recipeGrid.getFocused().getRectangle();
        Cursor.moveTo(
                rectangle.left() + rectangle.width() / 2.0D,
                rectangle.top() + rectangle.height() / 2.0D
        );
        UiInputModeController.activateFocusNavigation();
        return true;
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
    public boolean allowsItemSlotFocusNavigation() {
        return this.focusRegion == FocusRegion.SLOTS;
    }

    @Override
    public boolean seedFocusFromPointer(double guiX, double guiY) {
        Slot slot = this.itemSlotLayer.getItemSlotAt(this.itemSlotGrid, guiX, guiY);
        if (slot != null) {
            this.enterItemSlotFocus();
            this.itemSlotLayer.setFocusedItemSlot(slot);
            return true;
        }
        RecipeCell recipe = this.recipeGrid.recipeCellAt(guiX, guiY, false);
        if (recipe == null) {
            return this.focusFirstRecipeAndMoveCursor();
        }
        this.recipeGrid.setFocused(recipe);
        this.enterRecipeFocus();
        return true;
    }

    @Override
    public void clearFocusForPointer() {
        this.focusRegion = FocusRegion.NONE;
        this.recipeGrid.clearEntryFocus();
        this.itemSlotLayer.clearFocusedItemSlot();
    }

    @Override
    public boolean shouldShowCursorDuringFocus() {
        return this.focusRegion == FocusRegion.RECIPES;
    }

    @Override
    public boolean handleItemSlotNavigationBoundary(int keyCode) {
        if (this.focusRegion == FocusRegion.SLOTS && keyCode == GLFW.GLFW_KEY_UP) {
            return this.focusFirstRecipeAndMoveCursor();
        }
        return false;
    }

    @Override
    public CursorMagnetTarget cursorMagnetTargetAt(double guiX, double guiY) {
        CursorMagnetTarget recipeTarget = this.recipeGrid.cursorMagnetTargetAt(guiX, guiY);
        return recipeTarget != null
                ? recipeTarget
                : CustomItemSlotLayout.super.cursorMagnetTargetAt(guiX, guiY);
    }

    private final class RecipeGrid extends AbstractItemGridElement<RecipeOption> {
        private RecipeGrid(int x, int y) {
            super(x, y, 90, 54, 3, 5);
            this.setCellSize(18, 18);
        }

        @Override
        protected JavaBaseWidget createCell(RecipeOption recipe, int index, int width, int height) {
            return new RecipeCell(recipe, width, height);
        }

        private RecipeCell recipeCellAt(double mouseX, double mouseY, boolean includeFocused) {
            JavaBaseWidget cell = this.previewedCellAt(mouseX, mouseY, includeFocused);
            return cell instanceof RecipeCell recipeCell ? recipeCell : null;
        }

        private RecipeOption recipeAt(double mouseX, double mouseY, boolean includeFocused) {
            RecipeCell cell = this.recipeCellAt(mouseX, mouseY, includeFocused);
            return cell == null ? null : cell.recipe;
        }
    }

    private final class RecipeCell extends JavaBaseWidget {
        private final RecipeOption recipe;

        private RecipeCell(RecipeOption recipe, int width, int height) {
            super(0, 0, width, height, Component.empty());
            this.recipe = recipe;
        }

        @Override
        protected void extractWidgetRenderState(
                GuiGraphicsExtractor graphics,
                int mouseX,
                int mouseY,
                float partialTick
        ) {
            Draw.imageFromMeta(RECIPE_SLOT, this.x(), this.y(), this.width(), this.height())
                    .draw(graphics);
            if (!this.recipe.icon.isEmpty()) {
                graphics.item(this.recipe.icon, this.getX() + 1, this.getY() + 1);
            }
            if (this.isFocused() || this.isHovered()) {
                ItemSlotHighlightRenderer.draw(
                        graphics,
                        this.getX(),
                        this.getY(),
                        this.getWidth(),
                        this.getHeight()
                );
            }
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            StonecutterScreen.this.craftOnce(this.recipe);
        }

        @Override
        public boolean handleUiAction(UiAction action) {
            if (!this.active || !this.visible()) {
                return false;
            }
            if (action == UiAction.ACCEPT || action == UiAction.ALTERNATE) {
                return StonecutterScreen.this.craftOnce(this.recipe);
            }
            return false;
        }

        @Override
        public boolean prefersDirectFocusAction() {
            return true;
        }
    }

    private record RecipeOption(int index, ItemStack icon) {
    }

    private enum FocusRegion {
        RECIPES,
        SLOTS,
        NONE
    }

    private enum CraftAmount {
        ONE,
        STACK
    }

    private record PendingCraft(int recipeIndex, CraftAmount amount, int stateId) {
    }
}
