package net.jr.client.ui.container.slots;

public final class SlotMap {
    private final SlotSlice[] slices = new SlotSlice[SlotGroup.values().length];

    private SlotMap() {
    }

    public static SlotMap build(SlotGroup... order) {
        SlotMap map = new SlotMap();
        int cursor = 0;

        for (SlotGroup group : order) {
            if (map.slices[group.ordinal()] != null) {
                throw new IllegalStateException("SlotGroup duplicado en SlotMap: " + group);
            }

            int[] ids = new int[group.count()];
            for (int i = 0; i < group.count(); i++) {
                ids[i] = cursor++;
            }

            map.slices[group.ordinal()] = new SlotSlice(group, ids);
        }

        return map;
    }

    public static SlotMap fromSlices(SlotSlice... slices) {
        SlotMap map = new SlotMap();

        for (SlotSlice slice : slices) {
            SlotGroup group = slice.group();
            if (map.slices[group.ordinal()] != null) {
                throw new IllegalStateException("SlotGroup duplicado en SlotMap: " + group);
            }

            if (slice.size() != group.count()) {
                throw new IllegalStateException("SlotGroup " + group + " esperaba " + group.count() + " slots y recibio " + slice.size());
            }

            map.slices[group.ordinal()] = slice;
        }

        return map;
    }

    public SlotSlice get(SlotGroup group) {
        SlotSlice slice = slices[group.ordinal()];
        if (slice == null) {
            throw new IllegalStateException("SlotGroup no definido en este SlotMap: " + group);
        }
        return slice;
    }

    public boolean has(SlotGroup group) {
        return slices[group.ordinal()] != null;
    }

    public int slot(SlotGroup group, int index) {
        return get(group).slot(index);
    }

    public SlotGroup findGroup(int slotId) {
        for (SlotGroup group : SlotGroup.values()) {
            SlotSlice slice = slices[group.ordinal()];
            if (slice != null && slice.contains(slotId)) {
                return group;
            }
        }
        return null;
    }
}

