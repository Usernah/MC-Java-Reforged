package net.jr.client.ui.container.slots;

import java.util.HashMap;
import java.util.Map;

public final class SlotGrid {
    private final SlotMap map;
    private final Map<Integer, SlotPos> positions = new HashMap<>();

    public SlotGrid(SlotMap map) {
        this.map = map;
    }

    public SlotGrid grid(SlotGroup group, int startX, int startY, int columns, int step) {
        return this.grid(group, startX, startY, columns, step, step);
    }

    public SlotGrid grid(SlotGroup group, int startX, int startY, int columns, int stepX, int stepY) {
        SlotSlice slice = map.get(group);

        for (int i = 0; i < slice.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            positions.put(
                slice.slot(i),
                new SlotPos(startX + col * stepX, startY + row * stepY)
            );
        }

        return this;
    }

    public SlotGrid line(SlotGroup group, int startX, int startY, int step) {
        SlotSlice slice = map.get(group);

        for (int i = 0; i < slice.size(); i++) {
            positions.put(
                slice.slot(i),
                new SlotPos(startX + i * step, startY)
            );
        }

        return this;
    }

    public SlotGrid column(SlotGroup group, int startX, int startY, int step) {
        SlotSlice slice = map.get(group);

        for (int i = 0; i < slice.size(); i++) {
            positions.put(
                slice.slot(i),
                new SlotPos(startX, startY + i * step)
            );
        }

        return this;
    }

    public SlotGrid point(SlotGroup group, int localIndex, int x, int y) {
        positions.put(map.slot(group, localIndex), new SlotPos(x, y));
        return this;
    }

    public SlotPos get(int slotId) {
        return positions.get(slotId);
    }

    public SlotPos get(SlotGroup group, int localIndex) {
        return get(map.slot(group, localIndex));
    }

    public boolean has(int slotId) {
        return positions.containsKey(slotId);
    }
}

