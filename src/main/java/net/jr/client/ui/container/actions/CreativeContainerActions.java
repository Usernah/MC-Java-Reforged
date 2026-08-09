package net.jr.client.ui.container.actions;

import net.jr.mixin.accesors.CreativeModeInventoryScreenAccessor;
import net.jr.mixin.controlhints.AbstractContainerScreenAccessor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public final class CreativeContainerActions {
    private CreativeContainerActions() {
    }

    public static boolean isCreativeInventoryTab(AbstractContainerScreen<?> screen) {
        return screen instanceof CreativeModeInventoryScreen creativeScreen
                && creativeScreen.isInventoryOpen();
    }

    public static boolean isCreativePickerSlot(AbstractContainerScreen<?> screen, @Nullable Slot slot) {
        if (!(screen instanceof CreativeModeInventoryScreen creativeScreen) || slot == null) {
            return false;
        }

        return ((CreativeModeInventoryScreenAccessor) creativeScreen).javaReforged$isCreativeSlot(slot);
    }

    public static boolean isCreativeDestroySlot(AbstractContainerScreen<?> screen, @Nullable Slot slot) {
        if (!(screen instanceof CreativeModeInventoryScreen creativeScreen) || slot == null) {
            return false;
        }

        Slot destroySlot = ((CreativeModeInventoryScreenAccessor) creativeScreen).javaReforged$getDestroyItemSlot();
        return slot == destroySlot;
    }

    public static boolean isCreativeHotbarSlot(AbstractContainerScreen<?> screen, @Nullable Slot slot) {
        if (!(screen instanceof CreativeModeInventoryScreen) || slot == null) {
            return false;
        }

        if (isCreativeInventoryTab(screen)) {
            return false;
        }

        if (isCreativePickerSlot(screen, slot) || isCreativeDestroySlot(screen, slot)) {
            return false;
        }

        int containerIndex = slot.getSlotIndex();
        return containerIndex >= 0 && containerIndex < 9;
    }

    public static boolean quickMoveCreativeSlot(AbstractContainerScreen<?> screen, @Nullable Slot targetSlot) {
        if (!(screen instanceof CreativeModeInventoryScreen) || targetSlot == null) {
            return false;
        }

        if (isCreativeHotbarSlot(screen, targetSlot)) {
            clickSlot(screen, targetSlot, 0, ContainerInput.QUICK_MOVE);
            return true;
        }

        if (!isCreativePickerSlot(screen, targetSlot) || !targetSlot.hasItem()) {
            return false;
        }


        LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
        MultiPlayerGameMode gameMode = net.minecraft.client.Minecraft.getInstance().gameMode;
        if (player == null || gameMode == null) {
            return false;
        }

        ItemStack stack = targetSlot.getItem().copy();
        stack.setCount(stack.getMaxStackSize());

        Slot emptyHotbarSlot = findFirstEmptyCreativeHotbarSlot(screen);
        if (emptyHotbarSlot == null) {
            screen.getMenu().setCarried(stack);
            return true;
        }

        int hotbarSlot = 36 + emptyHotbarSlot.getSlotIndex();

        player.getInventory().setItem(emptyHotbarSlot.getSlotIndex(), stack.copy());
        player.inventoryMenu.broadcastChanges();
        gameMode.handleCreativeModeItemAdd(stack, hotbarSlot);

        return true;
    }

    @Nullable
    public static Slot findFirstEmptyCreativeHotbarSlot(AbstractContainerScreen<?> screen) {
        for (Slot slot : screen.getMenu().slots) {
            if (isCreativeHotbarSlot(screen, slot) && !slot.hasItem()) {
                return slot;
            }
        }

        return null;
    }

    private static void clickSlot(AbstractContainerScreen<?> screen, @Nullable Slot slot, int mouseButton, ContainerInput clickType) {
        ((AbstractContainerScreenAccessor) screen).javareforged$invokeSlotClicked(
                slot,
                slot == null ? -1 : slot.index,
                mouseButton,
                clickType
        );
    }
}

