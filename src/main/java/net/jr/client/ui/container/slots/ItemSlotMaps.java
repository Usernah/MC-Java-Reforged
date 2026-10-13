package net.jr.client.ui.container.slots;

public final class ItemSlotMaps {
    public static final ItemSlotMap PLAYER_INVENTORY = ItemSlotMap.build(
        ItemSlotGroup.PLAYER_ARMOR,
        ItemSlotGroup.PLAYER_INVENTORY,
        ItemSlotGroup.PLAYER_HOTBAR,
        ItemSlotGroup.PLAYER_OFFHAND
    );

    public static final ItemSlotMap SURVIVAL_INVENTORY = ItemSlotMap.fromSlices(
        new ItemSlotSlice(ItemSlotGroup.PLAYER_ARMOR, new int[] {5, 6, 7, 8}),
        new ItemSlotSlice(ItemSlotGroup.PLAYER_INVENTORY, new int[] {
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35
        }),
        new ItemSlotSlice(ItemSlotGroup.PLAYER_HOTBAR, new int[] {36, 37, 38, 39, 40, 41, 42, 43, 44}),
        new ItemSlotSlice(ItemSlotGroup.PLAYER_OFFHAND, new int[] {45})
    );

    public static final ItemSlotMap SURVIVAL_CRAFTING = ItemSlotMap.fromSlices(
        new ItemSlotSlice(ItemSlotGroup.PLAYER_CRAFTING_RESULT, new int[] {0}),
        new ItemSlotSlice(ItemSlotGroup.PLAYER_CRAFTING_GRID, new int[] {1, 2, 3, 4}),
        new ItemSlotSlice(ItemSlotGroup.PLAYER_ARMOR, new int[] {5, 6, 7, 8}),
        new ItemSlotSlice(ItemSlotGroup.PLAYER_INVENTORY, new int[] {
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35
        }),
        new ItemSlotSlice(ItemSlotGroup.PLAYER_HOTBAR, new int[] {36, 37, 38, 39, 40, 41, 42, 43, 44}),
        new ItemSlotSlice(ItemSlotGroup.PLAYER_OFFHAND, new int[] {45})
    );

    public static final ItemSlotMap CRAFTING_TABLE = ItemSlotMap.fromSlices(
        new ItemSlotSlice(ItemSlotGroup.CRAFTING_RESULT, new int[] {0}),
        new ItemSlotSlice(ItemSlotGroup.CRAFTING_GRID_3X3, new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9}),
        new ItemSlotSlice(ItemSlotGroup.PLAYER_INVENTORY, new int[] {
            10, 11, 12, 13, 14, 15, 16, 17, 18,
            19, 20, 21, 22, 23, 24, 25, 26, 27,
            28, 29, 30, 31, 32, 33, 34, 35, 36
        }),
        new ItemSlotSlice(ItemSlotGroup.PLAYER_HOTBAR, new int[] {37, 38, 39, 40, 41, 42, 43, 44, 45})
    );

    public static final ItemSlotMap BATTLE_PLAYER = ItemSlotMap.build(
        ItemSlotGroup.PLAYER_ARMOR,
        ItemSlotGroup.PLAYER_HOTBAR,
        ItemSlotGroup.PLAYER_OFFHAND
    );

    public static final ItemSlotMap CHEST_9 = ItemSlotMap.build(
        ItemSlotGroup.GENERIC_X9,
        ItemSlotGroup.PLAYER_INVENTORY,
        ItemSlotGroup.PLAYER_HOTBAR
    );

    public static final ItemSlotMap CHEST_18 = ItemSlotMap.build(
        ItemSlotGroup.GENERIC_X18,
        ItemSlotGroup.PLAYER_INVENTORY,
        ItemSlotGroup.PLAYER_HOTBAR
    );

    public static final ItemSlotMap CHEST_27 = ItemSlotMap.build(
        ItemSlotGroup.GENERIC_X27,
        ItemSlotGroup.PLAYER_INVENTORY,
        ItemSlotGroup.PLAYER_HOTBAR
    );

    public static final ItemSlotMap CHEST_36 = ItemSlotMap.build(
        ItemSlotGroup.GENERIC_X36,
        ItemSlotGroup.PLAYER_INVENTORY,
        ItemSlotGroup.PLAYER_HOTBAR
    );

    public static final ItemSlotMap CHEST_45 = ItemSlotMap.build(
        ItemSlotGroup.GENERIC_X45,
        ItemSlotGroup.PLAYER_INVENTORY,
        ItemSlotGroup.PLAYER_HOTBAR
    );

    public static final ItemSlotMap CHEST_54 = ItemSlotMap.build(
        ItemSlotGroup.GENERIC_X54,
        ItemSlotGroup.PLAYER_INVENTORY,
        ItemSlotGroup.PLAYER_HOTBAR
    );

    private ItemSlotMaps() {
    }

    public static ItemSlotMap named(String name) {
        return switch (name.trim().replace('-', '_').toUpperCase(java.util.Locale.ROOT)) {
            case "PLAYER_INVENTORY" -> PLAYER_INVENTORY;
            case "SURVIVAL_INVENTORY" -> SURVIVAL_INVENTORY;
            case "SURVIVAL_CRAFTING" -> SURVIVAL_CRAFTING;
            case "CRAFTING_TABLE" -> CRAFTING_TABLE;
            case "BATTLE_PLAYER" -> BATTLE_PLAYER;
            case "CHEST_9" -> CHEST_9;
            case "CHEST_18" -> CHEST_18;
            case "CHEST_27" -> CHEST_27;
            case "CHEST_36" -> CHEST_36;
            case "CHEST_45" -> CHEST_45;
            case "CHEST_54" -> CHEST_54;
            default -> throw new IllegalArgumentException("Unknown item slot map '" + name + "'");
        };
    }
}

