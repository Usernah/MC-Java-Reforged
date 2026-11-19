package net.jr.client.ui.container.slots;

import net.jr.client.ui.presentation.UiPresentation;
import net.jr.client.ui.navigation.UiInputModeController;
import net.jr.client.render.GuiGraphicsExtractorBridge;
import net.jr.mixin.accessors.AbstractContainerScreenAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.Util;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public final class VanillaItemSlotLayer {
    private final AbstractContainerMenu menu;
    private final Font font;
    private final Minecraft minecraft;
    private final int slotSize;
    @Nullable
    private final AbstractContainerScreen<?> hostScreen;

    @Nullable
    private Integer hoveredSlotId;
    @Nullable
    private Integer focusedSlotId;

    private final Set<Slot> quickCraftSlots = new HashSet<>();
    @Nullable
    private Slot quickCraftOriginSlot;
    private boolean isQuickCrafting;
    private int quickCraftingType;
    private int quickCraftingButton;
    private boolean skipNextRelease = true;
    private int quickCraftingRemainder;
    private long lastClickTime;
    private int lastClickButton;
    private boolean doubleClick;
    private int lastClickSlotId = Integer.MIN_VALUE;
    private ItemStack lastQuickMoved = ItemStack.EMPTY;

    public VanillaItemSlotLayer(
        @Nullable AbstractContainerScreen<?> hostScreen,
        AbstractContainerMenu menu,
        Font font,
        int slotSize
    ) {
        this.hostScreen = hostScreen;
        this.menu = menu;
        this.font = font;
        this.minecraft = Minecraft.getInstance();
        this.slotSize = slotSize;
    }

    public boolean matches(@Nullable AbstractContainerScreen<?> hostScreen, AbstractContainerMenu menu) {
        return this.hostScreen == hostScreen && this.menu == menu;
    }

    public void renderItemSlotMap(GuiGraphicsExtractor graphics, ItemSlotGrid grid) {
        for (int slotId = 0; slotId < this.menu.slots.size(); slotId++) {
            if (grid.has(slotId)) {
                this.renderSlot(graphics, slotId, grid);
            }
        }
    }

    public void updateHovered(ItemSlotGrid grid, double mouseX, double mouseY) {
        if (this.hostScreen != null && UiInputModeController.isFocusNavigationActive()) {
            this.hoveredSlotId = this.focusedSlotId != null && grid.has(this.focusedSlotId)
                ? this.focusedSlotId
                : null;
        } else {
            this.hoveredSlotId = this.findSlotIdAt(grid, mouseX, mouseY);
        }
        this.syncScreenState();
    }

    public void renderHoveredItemSlot(GuiGraphicsExtractor graphics, ItemSlotGrid grid) {
        if (this.hoveredSlotId == null) {
            return;
        }
        ItemSlotPos slotPos = grid.get(this.hoveredSlotId);
        if (slotPos == null) {
            return;
        }

        ItemSlotHighlightRenderer.draw(
                graphics,
                slotPos.x(),
                slotPos.y(),
                slotPos.width(),
                slotPos.height()
        );
    }

    public void renderTooltip(GuiGraphicsExtractor graphics, ItemSlotGrid grid, int mouseX, int mouseY) {
        Slot hoveredSlot = this.getHoveredItemSlot();
        if (hoveredSlot == null || !hoveredSlot.hasItem() || !this.menu.getCarried().isEmpty()) {
            return;
        }

        int tooltipX = mouseX;
        int tooltipY = mouseY;
        if (UiInputModeController.isFocusNavigationActive()) {
            ItemSlotPos slotPos = grid.get(hoveredSlot.index);
            if (slotPos != null) {
                tooltipX = slotPos.x() + slotPos.width() / 2;
                tooltipY = slotPos.y() + slotPos.height() / 2;
            }
        }
        graphics.setTooltipForNextFrame(this.font, hoveredSlot.getItem(), tooltipX, tooltipY);
    }

    public void renderCarriedItem(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ItemStack carriedItem = this.menu.getCarried();
        if (carriedItem.isEmpty()) {
            return;
        }

        float scale = 32.0F / 16.0F;
        graphics.nextStratum();
        graphics.pose().pushMatrix();
        graphics.pose().translate(mouseX - 9.0F - scale / 2.0F, mouseY - 9.0F - scale / 2.0F);
        graphics.pose().scale(scale, scale);
        Font itemFont = IClientItemExtensions.of(carriedItem)
            .getFont(carriedItem, IClientItemExtensions.FontContext.ITEM_COUNT);
        ((GuiGraphicsExtractorBridge)graphics).javaReforged$renderCarriedItemStackAtSize(
            carriedItem,
            0,
            0,
            0,
                UiPresentation.isSmall() ? 16 : 32,
            false,
            itemFont == null ? this.font : itemFont,
            null
        );
        graphics.pose().popMatrix();
    }

    public boolean mouseClicked(
        ItemSlotGrid grid,
        double mouseX,
        double mouseY,
        int button,
        boolean hasContainerBounds,
        int containerLeft,
        int containerTop,
        int containerWidth,
        int containerHeight
    ) {
        if (!this.hasLocalInventoryAccess()) {
            return false;
        }

        Integer slotId = this.findSlotIdAt(grid, mouseX, mouseY);
        Slot slot = slotId != null ? this.menu.slots.get(slotId) : null;
        MouseButtonEvent mouseEvent = mouseEvent(mouseX, mouseY, button);
        boolean isPickItemClick = this.minecraft.options.keyPickItem.matchesMouse(mouseEvent);
        long now = Util.getMillis();

        this.doubleClick = this.lastClickSlotId == (slotId == null ? Integer.MIN_VALUE : slotId)
            && now - this.lastClickTime < 250L
            && this.lastClickButton == button;
        this.skipNextRelease = false;

        if (button != 0 && button != 1 && !isPickItemClick) {
            this.checkHotbarMouseClicked(mouseEvent);
        } else {
            boolean clickedOutside = hasContainerBounds
                && this.hasClickedOutside(
                    mouseX,
                    mouseY,
                    containerLeft,
                    containerTop,
                    containerWidth,
                    containerHeight
                );
            if (slot != null) {
                clickedOutside = false;
            }

            int targetSlotId = slot != null ? slot.index : -1;
            if (clickedOutside) {
                targetSlotId = -999;
            }

            if (targetSlotId != -1) {
                if (this.menu.getCarried().isEmpty()) {
                    if (isPickItemClick) {
                        this.slotClicked(slot, targetSlotId, button, ContainerInput.CLONE);
                    } else {
                        boolean shiftDown = targetSlotId != -999 && mouseEvent.hasShiftDown();
                        ContainerInput input = ContainerInput.PICKUP;
                        if (shiftDown) {
                            this.lastQuickMoved = slot != null && slot.hasItem()
                                ? slot.getItem().copy()
                                : ItemStack.EMPTY;
                            input = ContainerInput.QUICK_MOVE;
                        } else if (targetSlotId == -999) {
                            input = ContainerInput.THROW;
                        }
                        this.slotClicked(slot, targetSlotId, button, input);
                    }
                    this.skipNextRelease = true;
                } else if (!this.isQuickCrafting) {
                    this.isQuickCrafting = true;
                    this.quickCraftOriginSlot = slot;
                    this.quickCraftingButton = button;
                    this.quickCraftSlots.clear();
                    if (button == 0) {
                        this.quickCraftingType = 0;
                    } else if (button == 1) {
                        this.quickCraftingType = 1;
                    } else if (isPickItemClick) {
                        this.quickCraftingType = 2;
                    }
                    this.recalculateQuickCraftRemaining();
                }
            }
        }

        this.lastClickSlotId = slotId == null ? Integer.MIN_VALUE : slotId;
        this.lastClickTime = now;
        this.lastClickButton = button;
        this.syncScreenState();
        return true;
    }

    public boolean mouseReleased(
        ItemSlotGrid grid,
        double mouseX,
        double mouseY,
        int button,
        boolean hasContainerBounds,
        int containerLeft,
        int containerTop,
        int containerWidth,
        int containerHeight
    ) {
        if (!this.hasLocalInventoryAccess()) {
            return false;
        }

        Integer slotId = this.findSlotIdAt(grid, mouseX, mouseY);
        Slot slot = slotId != null ? this.menu.slots.get(slotId) : null;
        boolean clickedOutside = hasContainerBounds
            && this.hasClickedOutside(
                mouseX,
                mouseY,
                containerLeft,
                containerTop,
                containerWidth,
                containerHeight
            );
        if (slot != null) {
            clickedOutside = false;
        }

        MouseButtonEvent mouseEvent = mouseEvent(mouseX, mouseY, button);
        int targetSlotId = slot != null ? slot.index : -1;
        if (clickedOutside) {
            targetSlotId = -999;
        }

        if (this.doubleClick
            && slot != null
            && button == 0
            && this.menu.canTakeItemForPickAll(ItemStack.EMPTY, slot)) {
            if (mouseEvent.hasShiftDown()) {
                if (!this.lastQuickMoved.isEmpty()) {
                    for (Slot otherSlot : this.menu.slots) {
                        if (otherSlot != null
                            && otherSlot.mayPickup(this.minecraft.player)
                            && otherSlot.hasItem()
                            && otherSlot.container == slot.container
                            && AbstractContainerMenu.canItemQuickReplace(otherSlot, this.lastQuickMoved, true)) {
                            this.slotClicked(
                                otherSlot,
                                otherSlot.index,
                                button,
                                ContainerInput.QUICK_MOVE
                            );
                        }
                    }
                }
            } else {
                this.slotClicked(slot, targetSlotId, button, ContainerInput.PICKUP_ALL);
            }
            this.doubleClick = false;
            this.lastClickTime = 0L;
        } else {
            if (this.isQuickCrafting && this.quickCraftingButton != button) {
                this.isQuickCrafting = false;
                this.quickCraftOriginSlot = null;
                this.quickCraftSlots.clear();
                this.quickCraftingRemainder = 0;
                this.skipNextRelease = true;
                this.syncScreenState();
                return true;
            }

            if (this.skipNextRelease) {
                this.skipNextRelease = false;
                this.syncScreenState();
                return true;
            }

            if (this.isQuickCrafting && !this.quickCraftSlots.isEmpty()) {
                this.slotClicked(
                    null,
                    -999,
                    AbstractContainerMenu.getQuickcraftMask(0, this.quickCraftingType),
                    ContainerInput.QUICK_CRAFT
                );
                for (Slot quickCraftSlot : this.quickCraftSlots) {
                    this.slotClicked(
                        quickCraftSlot,
                        quickCraftSlot.index,
                        AbstractContainerMenu.getQuickcraftMask(1, this.quickCraftingType),
                        ContainerInput.QUICK_CRAFT
                    );
                }
                this.slotClicked(
                    null,
                    -999,
                    AbstractContainerMenu.getQuickcraftMask(2, this.quickCraftingType),
                    ContainerInput.QUICK_CRAFT
                );
            } else if (!this.menu.getCarried().isEmpty()) {
                if (this.minecraft.options.keyPickItem.matchesMouse(mouseEvent)) {
                    this.slotClicked(slot, targetSlotId, button, ContainerInput.CLONE);
                } else {
                    boolean shiftDown = targetSlotId != -999 && mouseEvent.hasShiftDown();
                    if (shiftDown) {
                        this.lastQuickMoved = slot != null && slot.hasItem()
                            ? slot.getItem().copy()
                            : ItemStack.EMPTY;
                    }
                    this.slotClicked(
                        slot,
                        targetSlotId,
                        button,
                        shiftDown ? ContainerInput.QUICK_MOVE : ContainerInput.PICKUP
                    );
                }
            }
        }

        if (this.menu.getCarried().isEmpty()) {
            this.lastClickTime = 0L;
        }
        this.isQuickCrafting = false;
        this.quickCraftOriginSlot = null;
        this.quickCraftSlots.clear();
        this.quickCraftingRemainder = 0;
        this.syncScreenState();
        return true;
    }

    public boolean mouseDragged(
        ItemSlotGrid grid,
        double mouseX,
        double mouseY,
        int button,
        double dragX,
        double dragY
    ) {
        if (!this.hasLocalInventoryAccess()) {
            return false;
        }
        Slot slot = this.findSlot(grid, mouseX, mouseY);
        ItemStack carried = this.menu.getCarried();
        if (this.isQuickCrafting
            && this.quickCraftOriginSlot != null
            && slot != null
            && slot != this.quickCraftOriginSlot
            && !carried.isEmpty()
            && (carried.getCount() > this.quickCraftSlots.size() || this.quickCraftingType == 2)
            && this.canQuickCraftInto(slot, carried)) {
            if (this.quickCraftSlots.isEmpty()) {
                if (!this.canQuickCraftInto(this.quickCraftOriginSlot, carried)) {
                    return true;
                }
                this.quickCraftSlots.add(this.quickCraftOriginSlot);
            }
            this.quickCraftSlots.add(slot);
            this.recalculateQuickCraftRemaining();
            this.syncScreenState();
        }
        return true;
    }

    private boolean canQuickCraftInto(Slot slot, ItemStack carried) {
        return slot.isActive()
            && AbstractContainerMenu.canItemQuickReplace(slot, carried, true)
            && slot.mayPlace(carried)
            && this.menu.canDragTo(slot);
    }

    public boolean keyPressed(ItemSlotGrid grid, int keyCode, int scanCode) {
        if (!this.hasLocalInventoryAccess()) {
            return false;
        }
        KeyEvent event = new KeyEvent(keyCode, scanCode, currentModifiers());
        boolean handled = this.checkHotbarKeyPressed(event);
        Slot hoveredSlot = this.getHoveredItemSlot();
        if (hoveredSlot != null && hoveredSlot.hasItem()) {
            if (this.minecraft.options.keyPickItem.matches(event)) {
                this.slotClicked(hoveredSlot, hoveredSlot.index, 0, ContainerInput.CLONE);
                handled = true;
            } else if (this.minecraft.options.keyDrop.matches(event)) {
                this.slotClicked(
                    hoveredSlot,
                    hoveredSlot.index,
                    event.hasControlDown() ? 1 : 0,
                    ContainerInput.THROW
                );
                handled = true;
            }
        } else if (this.minecraft.options.keyDrop.matches(event)) {
            handled = true;
        }
        return handled;
    }

    @Nullable
    public Slot getHoveredItemSlot() {
        if (this.hoveredSlotId == null
            || this.hoveredSlotId < 0
            || this.hoveredSlotId >= this.menu.slots.size()) {
            return null;
        }
        Slot slot = this.menu.slots.get(this.hoveredSlotId);
        return slot.isActive() ? slot : null;
    }

    @Nullable
    public Slot getItemSlotAt(ItemSlotGrid grid, double mouseX, double mouseY) {
        Integer slotId = this.findSlotIdAt(grid, mouseX, mouseY);
        return slotId == null ? null : this.menu.slots.get(slotId);
    }

    @Nullable
    public Slot getFocusedItemSlot() {
        if (this.focusedSlotId == null
            || this.focusedSlotId < 0
            || this.focusedSlotId >= this.menu.slots.size()) {
            this.focusedSlotId = null;
            return null;
        }
        Slot slot = this.menu.slots.get(this.focusedSlotId);
        if (!slot.isActive()) {
            this.focusedSlotId = null;
            return null;
        }
        return slot;
    }

    @Nullable
    public Integer getFocusedItemSlotId() {
        Slot focusedSlot = this.getFocusedItemSlot();
        return focusedSlot == null ? null : focusedSlot.index;
    }

    public boolean hasFocusedItemSlot() {
        return this.getFocusedItemSlot() != null;
    }

    public void setFocusedItemSlot(@Nullable Slot slot) {
        this.focusedSlotId = slot == null ? null : slot.index;
        this.hoveredSlotId = this.focusedSlotId;
        this.syncScreenState();
    }

    public void clearFocusedItemSlot() {
        this.focusedSlotId = null;
        this.hoveredSlotId = null;
        this.syncScreenState();
    }

    @Nullable
    public Slot resolveActionItemSlot(boolean focusNavigationActive, boolean requireItem) {
        Slot target = focusNavigationActive ? this.getFocusedItemSlot() : this.getHoveredItemSlot();
        return isUsableActionSlot(target, requireItem) ? target : null;
    }

    @Nullable
    private Slot findSlot(ItemSlotGrid grid, double mouseX, double mouseY) {
        Integer slotId = this.findSlotIdAt(grid, mouseX, mouseY);
        return slotId == null ? null : this.menu.slots.get(slotId);
    }

    @Nullable
    private Integer findSlotIdAt(ItemSlotGrid grid, double mouseX, double mouseY) {
        for (int slotId = 0; slotId < this.menu.slots.size(); slotId++) {
            if (!grid.has(slotId)) {
                continue;
            }
            Slot slot = this.menu.slots.get(slotId);
            ItemSlotPos slotPos = grid.get(slotId);
            if (slot.isActive() && slotPos != null && this.isHovering(slotPos, mouseX, mouseY)) {
                return slotId;
            }
        }
        return null;
    }

    private void renderSlot(GuiGraphicsExtractor graphics, int slotId, ItemSlotGrid grid) {
        Slot slot = this.menu.slots.get(slotId);
        ItemSlotPos slotPos = grid.get(slotId);
        if (!slot.isActive() || slotPos == null) {
            return;
        }

        ItemStack renderStack = slot.getItem();
        ItemStack carried = this.menu.getCarried();
        String countText = null;
        boolean renderQuickCraftOverlay = false;
        if (this.isQuickCrafting && this.quickCraftSlots.contains(slot) && !carried.isEmpty()) {
            if (this.quickCraftSlots.size() == 1) {
                return;
            }
            if (AbstractContainerMenu.canItemQuickReplace(slot, carried, true) && this.menu.canDragTo(slot)) {
                renderQuickCraftOverlay = true;
                int maxCount = Math.min(carried.getMaxStackSize(), slot.getMaxStackSize(carried));
                int currentCount = slot.getItem().isEmpty() ? 0 : slot.getItem().getCount();
                int placedCount = AbstractContainerMenu.getQuickCraftPlaceCount(
                    this.quickCraftSlots.size(),
                    this.quickCraftingType,
                    carried
                ) + currentCount;
                if (placedCount > maxCount) {
                    placedCount = maxCount;
                    countText = ChatFormatting.YELLOW + Integer.toString(maxCount);
                }
                renderStack = carried.copyWithCount(placedCount);
            } else {
                this.quickCraftSlots.remove(slot);
                this.recalculateQuickCraftRemaining();
                this.syncScreenState();
            }
        }

        if (renderQuickCraftOverlay) {
            graphics.fill(
                slotPos.x(), slotPos.y(),
                slotPos.x() + slotPos.width(), slotPos.y() + slotPos.height(),
                -2130706433
            );
        }
        if (renderStack.isEmpty()) {
            if (slot.isActive() && slot.getNoItemIcon() != null) {
                graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    slot.getNoItemIcon(),
                    slotPos.x(),
                    slotPos.y(),
                    slotPos.width(),
                    slotPos.height()
                );
            }
            return;
        }
        int itemSize = slotPos.itemSize();
        int itemX = slotPos.x() + (slotPos.width() - itemSize) / 2;
        int itemY = slotPos.y() + (slotPos.height() - itemSize) / 2;
        float scale = itemSize / 16.0F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(itemX, itemY);
        graphics.pose().scale(scale, scale);
        ((GuiGraphicsExtractorBridge)graphics).javaReforged$renderItemStackAtSize(
            renderStack,
            0,
            0,
            slotId,
            itemSize,
            slot.isFake(),
            this.font,
            countText
        );
        graphics.pose().popMatrix();
    }

    private void recalculateQuickCraftRemaining() {
        ItemStack carried = this.menu.getCarried();
        if (carried.isEmpty() || !this.isQuickCrafting) {
            this.quickCraftingRemainder = 0;
            return;
        }
        if (this.quickCraftingType == 2) {
            this.quickCraftingRemainder = carried.getMaxStackSize();
            return;
        }
        this.quickCraftingRemainder = carried.getCount();
        for (Slot slot : this.quickCraftSlots) {
            ItemStack slotStack = slot.getItem();
            int currentCount = slotStack.isEmpty() ? 0 : slotStack.getCount();
            int maxCount = Math.min(carried.getMaxStackSize(), slot.getMaxStackSize(carried));
            int placedCount = Math.min(
                AbstractContainerMenu.getQuickCraftPlaceCount(
                    this.quickCraftSlots.size(),
                    this.quickCraftingType,
                    carried
                ) + currentCount,
                maxCount
            );
            this.quickCraftingRemainder -= placedCount - currentCount;
        }
    }

    private boolean checkHotbarKeyPressed(KeyEvent event) {
        Slot hoveredSlot = this.getHoveredItemSlot();
        if (!this.menu.getCarried().isEmpty() || hoveredSlot == null) {
            return false;
        }
        if (this.minecraft.options.keySwapOffhand.matches(event)) {
            this.slotClicked(hoveredSlot, hoveredSlot.index, 40, ContainerInput.SWAP);
            return true;
        }
        for (int i = 0; i < 9; i++) {
            if (this.minecraft.options.keyHotbarSlots[i].matches(event)) {
                this.slotClicked(hoveredSlot, hoveredSlot.index, i, ContainerInput.SWAP);
                return true;
            }
        }
        return false;
    }

    private void checkHotbarMouseClicked(MouseButtonEvent event) {
        Slot hoveredSlot = this.getHoveredItemSlot();
        if (hoveredSlot == null || !this.menu.getCarried().isEmpty()) {
            return;
        }
        if (this.minecraft.options.keySwapOffhand.matchesMouse(event)) {
            this.slotClicked(hoveredSlot, hoveredSlot.index, 40, ContainerInput.SWAP);
            return;
        }
        for (int i = 0; i < 9; i++) {
            if (this.minecraft.options.keyHotbarSlots[i].matchesMouse(event)) {
                this.slotClicked(hoveredSlot, hoveredSlot.index, i, ContainerInput.SWAP);
            }
        }
    }

    private boolean hasClickedOutside(
        double mouseX,
        double mouseY,
        int left,
        int top,
        int width,
        int height
    ) {
        return mouseX < left || mouseY < top || mouseX >= left + width || mouseY >= top + height;
    }

    private boolean isHovering(ItemSlotPos slotPos, double mouseX, double mouseY) {
        return mouseX >= slotPos.x()
            && mouseX < slotPos.x() + slotPos.width()
            && mouseY >= slotPos.y()
            && mouseY < slotPos.y() + slotPos.height();
    }

    private static boolean isUsableActionSlot(@Nullable Slot slot, boolean requireItem) {
        return slot != null && slot.isActive() && (!requireItem || slot.hasItem());
    }

    private void slotClicked(
        @Nullable Slot slot,
        int slotId,
        int mouseButton,
        ContainerInput input
    ) {
        if (slot != null) {
            slotId = slot.index;
        }
        if (this.hostScreen instanceof AbstractContainerScreenAccessor accessor) {
            accessor.javaReforged$invokeSlotClicked(slot, slotId, mouseButton, input);
            return;
        }
        LocalPlayer player = this.minecraft.player;
        MultiPlayerGameMode gameMode = this.minecraft.gameMode;
        if (player != null && gameMode != null) {
            gameMode.handleContainerInput(this.menu.containerId, slotId, mouseButton, input, player);
        }
    }

    private boolean hasLocalInventoryAccess() {
        return this.minecraft.player != null && this.minecraft.gameMode != null;
    }

    private void syncScreenState() {
        if (!(this.hostScreen instanceof AbstractContainerScreenAccessor accessor)) {
            return;
        }
        accessor.javaReforged$setHoveredSlot(this.getHoveredItemSlot());
        accessor.javaReforged$setQuickCrafting(this.isQuickCrafting);
        accessor.javaReforged$setQuickCraftingRemainder(this.quickCraftingRemainder);
        Set<Slot> screenQuickCraftSlots = accessor.javaReforged$getQuickCraftSlots();
        screenQuickCraftSlots.clear();
        screenQuickCraftSlots.addAll(this.quickCraftSlots);
    }

    private static MouseButtonEvent mouseEvent(double mouseX, double mouseY, int button) {
        return new MouseButtonEvent(
            mouseX,
            mouseY,
            new MouseButtonInfo(button, currentModifiers())
        );
    }

    private static int currentModifiers() {
        long window = Minecraft.getInstance().getWindow().handle();
        int modifiers = 0;
        if (isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
            || isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT)) {
            modifiers |= GLFW.GLFW_MOD_SHIFT;
        }
        if (isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
            || isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL)) {
            modifiers |= GLFW.GLFW_MOD_CONTROL;
        }
        if (isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT)
            || isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT)) {
            modifiers |= GLFW.GLFW_MOD_ALT;
        }
        if (isKeyDown(window, GLFW.GLFW_KEY_LEFT_SUPER)
            || isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SUPER)) {
            modifiers |= GLFW.GLFW_MOD_SUPER;
        }
        return modifiers;
    }

    private static boolean isKeyDown(long window, int key) {
        int state = GLFW.glfwGetKey(window, key);
        return state == GLFW.GLFW_PRESS || state == GLFW.GLFW_REPEAT;
    }
}
