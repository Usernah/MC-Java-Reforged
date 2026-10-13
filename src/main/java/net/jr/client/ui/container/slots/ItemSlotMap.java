package net.jr.client.ui.container.slots;

public final class ItemSlotMap {
    private final ItemSlotSlice[] slices = new ItemSlotSlice[ItemSlotGroup.values().length];

    private ItemSlotMap() {
    }

    public static ItemSlotMap build(ItemSlotGroup... order) {
        ItemSlotMap map = new ItemSlotMap();
        int cursor = 0;

        for (ItemSlotGroup group : order) {
            if (map.slices[group.ordinal()] != null) {
                throw new IllegalStateException("ItemSlotGroup duplicado en ItemSlotMap: " + group);
            }

            int[] ids = new int[group.count()];
            for (int i = 0; i < group.count(); i++) {
                ids[i] = cursor++;
            }

            map.slices[group.ordinal()] = new ItemSlotSlice(group, ids);
        }

        return map;
    }

    public static ItemSlotMap fromSlices(ItemSlotSlice... slices) {
        ItemSlotMap map = new ItemSlotMap();

        for (ItemSlotSlice slice : slices) {
            ItemSlotGroup group = slice.group();
            if (map.slices[group.ordinal()] != null) {
                throw new IllegalStateException("ItemSlotGroup duplicado en ItemSlotMap: " + group);
            }

            if (slice.size() != group.count()) {
                throw new IllegalStateException("ItemSlotGroup " + group + " esperaba " + group.count() + " slots y recibio " + slice.size());
            }

            map.slices[group.ordinal()] = slice;
        }

        return map;
    }

    public ItemSlotSlice get(ItemSlotGroup group) {
        ItemSlotSlice slice = slices[group.ordinal()];
        if (slice == null) {
            throw new IllegalStateException("ItemSlotGroup no definido en este ItemSlotMap: " + group);
        }
        return slice;
    }

    public boolean has(ItemSlotGroup group) {
        return slices[group.ordinal()] != null;
    }

    public int slot(ItemSlotGroup group, int index) {
        return get(group).slot(index);
    }

    public ItemSlotGroup findGroup(int slotId) {
        for (ItemSlotGroup group : ItemSlotGroup.values()) {
            ItemSlotSlice slice = slices[group.ordinal()];
            if (slice != null && slice.contains(slotId)) {
                return group;
            }
        }
        return null;
    }
}

