package net.jr.experimental_features.minigame.battle;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

/** Server-side battle variant of TAKE_ALL, including the four armor slots. */
public final class BattleTakeAllAction {
    private static final EquipmentSlot[] ARMOR_ORDER = {
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET
    };

    private BattleTakeAllAction() {
    }

    public static void takeAll(ServerPlayer player, int containerId) {
        if (player.isSpectator()) {
            return;
        }

        AbstractContainerMenu menu = player.containerMenu;
        int containerSlotCount = containerSlotCount(menu);
        if (menu.containerId != containerId
            || containerSlotCount < 0
            || menu.slots.size() < containerSlotCount + 36
            || !menu.stillValid(player)
            || !menu.getCarried().isEmpty()) {
            return;
        }

        if (BattleRuntime.isActive()) {
            for (EquipmentSlot equipmentSlot : ARMOR_ORDER) {
                equipFirstMatching(player, menu, containerSlotCount, equipmentSlot);
            }
        }

        List<Integer> playerSlotOrder = playerSlotOrder(menu, containerSlotCount);
        for (int sourceIndex = 0; sourceIndex < containerSlotCount; sourceIndex++) {
            moveToPlayer(player, menu.slots.get(sourceIndex), menu, playerSlotOrder);
        }

        menu.broadcastChanges();
        player.inventoryMenu.broadcastChanges();
    }

    private static void equipFirstMatching(
        ServerPlayer player,
        AbstractContainerMenu menu,
        int containerSlotCount,
        EquipmentSlot equipmentSlot
    ) {
        if (!player.canUseSlot(equipmentSlot) || !player.getItemBySlot(equipmentSlot).isEmpty()) {
            return;
        }

        for (int index = 0; index < containerSlotCount; index++) {
            Slot source = menu.slots.get(index);
            if (!source.isActive() || !source.hasItem() || !source.mayPickup(player)) {
                continue;
            }
            Equippable equippable = source.getItem().get(DataComponents.EQUIPPABLE);
            if (equippable == null
                || !isArmorForSlot(source.getItem(), equipmentSlot)
                || equippable.slot() != equipmentSlot
                || !equippable.canBeEquippedBy(player.typeHolder())) {
                continue;
            }

            ItemStack taken = source.safeTake(1, source.getItem().getCount(), player);
            if (!taken.isEmpty()) {
                player.setItemSlot(equipmentSlot, taken);
                return;
            }
        }
    }

    private static boolean isArmorForSlot(ItemStack stack, EquipmentSlot equipmentSlot) {
        return switch (equipmentSlot) {
            case HEAD -> stack.is(ItemTags.HEAD_ARMOR);
            case CHEST -> stack.is(ItemTags.CHEST_ARMOR);
            case LEGS -> stack.is(ItemTags.LEG_ARMOR);
            case FEET -> stack.is(ItemTags.FOOT_ARMOR);
            default -> false;
        };
    }

    private static void moveToPlayer(
        ServerPlayer player,
        Slot source,
        AbstractContainerMenu menu,
        List<Integer> playerSlotOrder
    ) {
        if (!source.isActive() || !source.hasItem() || !source.mayPickup(player)) {
            return;
        }

        ItemStack sourceStack = source.getItem();
        if (!hasPlayerSpace(menu, playerSlotOrder, sourceStack)) {
            return;
        }

        ItemStack remainder = source.safeTake(sourceStack.getCount(), sourceStack.getCount(), player);
        if (remainder.isEmpty()) {
            return;
        }
        insertIntoMatchingSlots(menu, playerSlotOrder, remainder);
        insertIntoEmptySlots(menu, playerSlotOrder, remainder);
        if (!remainder.isEmpty()) {
            source.setByPlayer(remainder);
        }
    }

    private static boolean hasPlayerSpace(
        AbstractContainerMenu menu,
        List<Integer> order,
        ItemStack stack
    ) {
        for (int index : order) {
            Slot destination = menu.slots.get(index);
            if (!destination.isActive() || !destination.mayPlace(stack)) {
                continue;
            }
            ItemStack existing = destination.getItem();
            int maximum = destination.getMaxStackSize(stack);
            if ((existing.isEmpty() && maximum > 0)
                || (!existing.isEmpty()
                    && ItemStack.isSameItemSameComponents(existing, stack)
                    && existing.getCount() < maximum)) {
                return true;
            }
        }
        return false;
    }

    private static void insertIntoMatchingSlots(
        AbstractContainerMenu menu,
        List<Integer> order,
        ItemStack stack
    ) {
        for (int index : order) {
            if (stack.isEmpty()) {
                return;
            }
            Slot destination = menu.slots.get(index);
            ItemStack existing = destination.getItem();
            if (destination.isActive()
                && destination.mayPlace(stack)
                && !existing.isEmpty()
                && ItemStack.isSameItemSameComponents(existing, stack)
                && existing.getCount() < destination.getMaxStackSize(stack)) {
                destination.safeInsert(stack);
            }
        }
    }

    private static void insertIntoEmptySlots(
        AbstractContainerMenu menu,
        List<Integer> order,
        ItemStack stack
    ) {
        for (int index : order) {
            if (stack.isEmpty()) {
                return;
            }
            Slot destination = menu.slots.get(index);
            if (destination.isActive() && destination.getItem().isEmpty() && destination.mayPlace(stack)) {
                destination.safeInsert(stack);
            }
        }
    }

    private static List<Integer> playerSlotOrder(AbstractContainerMenu menu, int containerSlotCount) {
        int end = menu.slots.size();
        int hotbarStart = Math.max(containerSlotCount, end - 9);
        List<Integer> order = new ArrayList<>(end - containerSlotCount);
        for (int index = hotbarStart; index < end; index++) {
            order.add(index);
        }
        for (int index = containerSlotCount; index < hotbarStart; index++) {
            order.add(index);
        }
        return order;
    }

    private static int containerSlotCount(AbstractContainerMenu menu) {
        if (menu instanceof ChestMenu chest) {
            return chest.getRowCount() * 9;
        }
        return menu instanceof ShulkerBoxMenu ? 27 : -1;
    }
}
