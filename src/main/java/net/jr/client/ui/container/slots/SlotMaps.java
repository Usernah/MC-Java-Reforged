package net.jr.client.ui.container.slots;

public final class SlotMaps {
    public static final SlotMap PLAYER_INVENTORY = SlotMap.build(
        SlotGroup.PLAYER_ARMOR,
        SlotGroup.PLAYER_INVENTORY,
        SlotGroup.PLAYER_HOTBAR,
        SlotGroup.PLAYER_OFFHAND
    );

    public static final SlotMap SURVIVAL_INVENTORY = SlotMap.fromSlices(
        new SlotSlice(SlotGroup.PLAYER_ARMOR, new int[] {5, 6, 7, 8}),
        new SlotSlice(SlotGroup.PLAYER_INVENTORY, new int[] {
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35
        }),
        new SlotSlice(SlotGroup.PLAYER_HOTBAR, new int[] {36, 37, 38, 39, 40, 41, 42, 43, 44}),
        new SlotSlice(SlotGroup.PLAYER_OFFHAND, new int[] {45})
    );

    public static final SlotMap SURVIVAL_CRAFTING = SlotMap.fromSlices(
        new SlotSlice(SlotGroup.PLAYER_CRAFTING_RESULT, new int[] {0}),
        new SlotSlice(SlotGroup.PLAYER_CRAFTING_GRID, new int[] {1, 2, 3, 4}),
        new SlotSlice(SlotGroup.PLAYER_ARMOR, new int[] {5, 6, 7, 8}),
        new SlotSlice(SlotGroup.PLAYER_INVENTORY, new int[] {
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35
        }),
        new SlotSlice(SlotGroup.PLAYER_HOTBAR, new int[] {36, 37, 38, 39, 40, 41, 42, 43, 44}),
        new SlotSlice(SlotGroup.PLAYER_OFFHAND, new int[] {45})
    );

    public static final SlotMap BATTLE_PLAYER = SlotMap.build(
        SlotGroup.PLAYER_ARMOR,
        SlotGroup.PLAYER_HOTBAR,
        SlotGroup.PLAYER_OFFHAND
    );

    public static final SlotMap CHEST_9 = SlotMap.build(
        SlotGroup.GENERIC_X9,
        SlotGroup.PLAYER_INVENTORY,
        SlotGroup.PLAYER_HOTBAR
    );

    public static final SlotMap CHEST_18 = SlotMap.build(
        SlotGroup.GENERIC_X18,
        SlotGroup.PLAYER_INVENTORY,
        SlotGroup.PLAYER_HOTBAR
    );

    public static final SlotMap CHEST_27 = SlotMap.build(
        SlotGroup.GENERIC_X27,
        SlotGroup.PLAYER_INVENTORY,
        SlotGroup.PLAYER_HOTBAR
    );

    public static final SlotMap CHEST_36 = SlotMap.build(
        SlotGroup.GENERIC_X36,
        SlotGroup.PLAYER_INVENTORY,
        SlotGroup.PLAYER_HOTBAR
    );

    public static final SlotMap CHEST_45 = SlotMap.build(
        SlotGroup.GENERIC_X45,
        SlotGroup.PLAYER_INVENTORY,
        SlotGroup.PLAYER_HOTBAR
    );

    public static final SlotMap CHEST_54 = SlotMap.build(
        SlotGroup.GENERIC_X54,
        SlotGroup.PLAYER_INVENTORY,
        SlotGroup.PLAYER_HOTBAR
    );

    private SlotMaps() {
    }
}

