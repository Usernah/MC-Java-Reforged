package net.jr.client.ui.container.slots;

public enum ItemSlotGroup {
    PLAYER_HOTBAR(9),
    PLAYER_INVENTORY(27),
    PLAYER_ARMOR(4),
    PLAYER_OFFHAND(1),
    PLAYER_CRAFTING_RESULT(1),
    PLAYER_CRAFTING_GRID(4),
    CRAFTING_RESULT(1),
    CRAFTING_GRID_3X3(9),
    STONECUTTER_INPUT(1),

    GENERIC_X9(9),
    GENERIC_X18(18),
    GENERIC_X27(27),
    GENERIC_X36(36),
    GENERIC_X45(45),
    GENERIC_X54(54);

    private final int count;

    ItemSlotGroup(int count) {
        this.count = count;
    }

    public int count() {
        return count;
    }
}

