package net.jr.client.ui.container.actions;

import java.util.ArrayList;
import java.util.List;
import net.jr.experimental_features.minigame.client.BattleClientState;
import net.jr.experimental_features.minigame.network.BattleNetwork;
import net.jr.client.ui.container.slots.CustomItemSlotLayout;
import net.jr.client.ui.navigation.UiInputModeController;
import net.jr.mixin.accessors.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class ContainerInventoryActionHandler {
    private ContainerInventoryActionHandler() {
    }

    public static boolean pickupFocusedOrHoveredSlot(
        AbstractContainerScreen<?> screen,
        int mouseButton
    ) {
        return click(screen, actionSlot(screen, false), mouseButton, ContainerInput.PICKUP);
    }

    public static boolean quickMoveFocusedOrHoveredSlot(AbstractContainerScreen<?> screen) {
        if (!hasLocalInventoryAccess() || !screen.getMenu().getCarried().isEmpty()) {
            return false;
        }
        Slot target = actionSlot(screen, true);
        if (target == null) {
            return false;
        }
        if (screen instanceof CreativeModeInventoryScreen) {
            return CreativeContainerActions.quickMoveCreativeSlot(screen, target);
        }
        int containerSlotCount = containerSlotCount(screen);
        if (containerSlotCount >= 0 && target.index < containerSlotCount) {
            int playerSlotEnd = screen.getMenu().slots.size();
            int hotbarStart = Math.max(containerSlotCount, playerSlotEnd - 9);
            List<Integer> hotbarSlots = java.util.stream.IntStream
                .range(hotbarStart, playerSlotEnd)
                .boxed()
                .toList();
            List<Integer> inventorySlots = java.util.stream.IntStream
                .range(containerSlotCount, hotbarStart)
                .boxed()
                .toList();
            List<Integer> playerSlots = new ArrayList<>(hotbarSlots.size() + inventorySlots.size());
            playerSlots.addAll(hotbarSlots);
            playerSlots.addAll(inventorySlots);
            return moveSlotToPlayerInOrder(
                screen,
                target.index,
                playerSlots
            );
        }
        return click(screen, target, 0, ContainerInput.QUICK_MOVE);
    }

    public static boolean takeAllFromContainer(AbstractContainerScreen<?> screen) {
        if (!hasLocalInventoryAccess() || !screen.getMenu().getCarried().isEmpty()) {
            return false;
        }
        int containerSlotCount = containerSlotCount(screen);
        if (containerSlotCount < 0) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (BattleClientState.isBattle()
            && minecraft.getConnection() != null
            && minecraft.getConnection().hasChannel(BattleNetwork.TakeAllRequest.TYPE)) {
            ClientPacketDistributor.sendToServer(
                new BattleNetwork.TakeAllRequest(screen.getMenu().containerId)
            );
            return true;
        }

        int playerSlotEnd = screen.getMenu().slots.size();
        int hotbarStart = Math.max(containerSlotCount, playerSlotEnd - 9);
        List<Integer> hotbarSlots = java.util.stream.IntStream
            .range(hotbarStart, playerSlotEnd)
            .boxed()
            .toList();
        List<Integer> inventorySlots = java.util.stream.IntStream
            .range(containerSlotCount, hotbarStart)
            .boxed()
            .toList();
        List<Integer> playerSlots = new ArrayList<>(hotbarSlots.size() + inventorySlots.size());
        playerSlots.addAll(hotbarSlots);
        playerSlots.addAll(inventorySlots);
        boolean movedAny = false;
        for (int sourceIndex = 0; sourceIndex < containerSlotCount; sourceIndex++) {
            Slot source = screen.getMenu().slots.get(sourceIndex);
            if (source.isActive() && source.hasItem()) {
                movedAny |= moveSlotToPlayerInOrder(
                    screen,
                    sourceIndex,
                    playerSlots
                );
            }
        }
        return movedAny;
    }

    public static boolean storeAllInContainer(AbstractContainerScreen<?> screen) {
        if (!hasLocalInventoryAccess() || !screen.getMenu().getCarried().isEmpty()) {
            return false;
        }
        int containerSlotCount = containerSlotCount(screen);
        if (containerSlotCount < 0) {
            return false;
        }

        List<Integer> containerSlots = java.util.stream.IntStream
            .range(0, containerSlotCount)
            .boxed()
            .toList();
        boolean movedAny = false;
        for (int sourceIndex = containerSlotCount;
             sourceIndex < screen.getMenu().slots.size();
             sourceIndex++) {
            Slot source = screen.getMenu().slots.get(sourceIndex);
            if (source.isActive() && source.hasItem()) {
                movedAny |= moveSlotToContainerInOrder(screen, sourceIndex, containerSlots);
            }
        }
        return movedAny;
    }

    private static Slot actionSlot(AbstractContainerScreen<?> screen, boolean requireItem) {
        boolean focusNavigationActive = UiInputModeController.isFocusNavigationActive();
        CustomItemSlotLayout layout = CustomItemSlotLayout.active(screen);
        if (layout != null) {
            return layout.itemSlotLayer().resolveActionItemSlot(focusNavigationActive, requireItem);
        }

        Slot target = focusNavigationActive ? focused(screen) : hovered(screen);
        return isUsableActionSlot(target, requireItem) ? target : null;
    }

    private static Slot focused(AbstractContainerScreen<?> screen) {
        Integer focusedIndex = ContainerItemSlotFocusController.getFocusedItemSlotIndex(screen);
        if (focusedIndex == null
            || focusedIndex < 0
            || focusedIndex >= screen.getMenu().slots.size()) {
            return null;
        }
        return screen.getMenu().slots.get(focusedIndex);
    }

    private static Slot hovered(AbstractContainerScreen<?> screen) {
        return accessor(screen).javaReforged$getHoveredItemSlot();
    }

    private static boolean isUsableActionSlot(Slot slot, boolean requireItem) {
        return slot != null && slot.isActive() && (!requireItem || slot.hasItem());
    }

    private static int containerSlotCount(AbstractContainerScreen<?> screen) {
        if (screen.getMenu() instanceof ChestMenu chest) {
            return chest.getRowCount() * 9;
        }
        if (screen.getMenu() instanceof ShulkerBoxMenu) {
            return 27;
        }
        return -1;
    }

    private static boolean click(
        AbstractContainerScreen<?> screen,
        Slot slot,
        int button,
        ContainerInput input
    ) {
        if (!hasLocalInventoryAccess() || slot == null || !slot.isActive()) {
            return false;
        }
        accessor(screen).javaReforged$invokeSlotClicked(slot, slot.index, button, input);
        return true;
    }

    private static boolean moveSlotToPlayerInOrder(
        AbstractContainerScreen<?> screen,
        int sourceIndex,
        List<Integer> playerSlotOrder
    ) {
        Slot source = screen.getMenu().slots.get(sourceIndex);
        if (!source.hasItem()) {
            return false;
        }
        ItemStack original = source.getItem().copy();
        click(screen, source, 0, ContainerInput.PICKUP);
        if (screen.getMenu().getCarried().isEmpty()) {
            return !ItemStack.matches(original, source.getItem());
        }
        moveCarriedIntoMatchingSlots(screen, playerSlotOrder);
        moveCarriedIntoEmptySlots(screen, playerSlotOrder);
        if (!screen.getMenu().getCarried().isEmpty()) {
            click(screen, source, 0, ContainerInput.PICKUP);
        }
        return !ItemStack.matches(original, source.getItem());
    }

    private static boolean moveSlotToContainerInOrder(
        AbstractContainerScreen<?> screen,
        int sourceIndex,
        List<Integer> containerSlotOrder
    ) {
        Slot source = screen.getMenu().slots.get(sourceIndex);
        if (!source.hasItem()) {
            return false;
        }
        ItemStack original = source.getItem().copy();
        click(screen, source, 0, ContainerInput.PICKUP);
        if (screen.getMenu().getCarried().isEmpty()) {
            return !ItemStack.matches(original, source.getItem());
        }
        moveCarriedIntoMatchingSlots(screen, containerSlotOrder);
        moveCarriedIntoEmptySlots(screen, containerSlotOrder);
        if (!screen.getMenu().getCarried().isEmpty()) {
            click(screen, source, 0, ContainerInput.PICKUP);
        }
        return !ItemStack.matches(original, source.getItem());
    }

    private static void moveCarriedIntoMatchingSlots(
        AbstractContainerScreen<?> screen,
        List<Integer> order
    ) {
        for (int index : order) {
            ItemStack carried = screen.getMenu().getCarried();
            if (carried.isEmpty()) {
                return;
            }
            Slot slot = screen.getMenu().slots.get(index);
            ItemStack existing = slot.getItem();
            if (!slot.isActive()
                || !slot.mayPlace(carried)
                || existing.isEmpty()
                || !ItemStack.isSameItemSameComponents(existing, carried)) {
                continue;
            }
            int maximum = Math.min(slot.getMaxStackSize(carried), carried.getMaxStackSize());
            if (existing.getCount() < maximum) {
                click(screen, slot, 0, ContainerInput.PICKUP);
            }
        }
    }

    private static void moveCarriedIntoEmptySlots(
        AbstractContainerScreen<?> screen,
        List<Integer> order
    ) {
        for (int index : order) {
            ItemStack carried = screen.getMenu().getCarried();
            if (carried.isEmpty()) {
                return;
            }
            Slot slot = screen.getMenu().slots.get(index);
            if (slot.isActive() && slot.mayPlace(carried) && slot.getItem().isEmpty()) {
                click(screen, slot, 0, ContainerInput.PICKUP);
            }
        }
    }

    private static boolean hasLocalInventoryAccess() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && minecraft.gameMode != null;
    }

    private static AbstractContainerScreenAccessor accessor(AbstractContainerScreen<?> screen) {
        return (AbstractContainerScreenAccessor)screen;
    }
}
