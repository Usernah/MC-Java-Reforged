package net.alnv.javareforged.ClientRuntime.slot;

import java.util.ArrayList;
import java.util.List;
import net.alnv.javareforged.ClientRuntime.viewport.ViewportLayout;
import net.alnv.javareforged.ClientRuntime.viewport.ViewportTable;
import net.alnv.javareforged.ClientRuntime.viewport.WindowMetrics;

public final class PlayerSlots {
    public static final int MAX_SLOTS = 4;

    private final PlayerSlot[] slots = new PlayerSlot[MAX_SLOTS];
    private ViewportLayout layout;
    private ViewportTable viewportTable;

    public PlayerSlots() {
        this(ViewportLayout.FOUR_GRID);
    }

    public PlayerSlots(ViewportLayout layout) {
        this.layout = layout;
        for (int id = 0; id < this.slots.length; id++) {
            this.slots[id] = new PlayerSlot(id);
        }
    }

    public PlayerSlot slot(int id) {
        requireSlot(id);
        return this.slots[id];
    }

    public List<PlayerSlot> visibleSlots() {
        ArrayList<PlayerSlot> visible = new ArrayList<>();
        for (PlayerSlot slot : this.slots) {
            if (slot.drawable()) {
                visible.add(slot);
            }
        }
        return List.copyOf(visible);
    }

    public int presentSlotCount() {
        int count = 0;
        for (PlayerSlot slot : this.slots) {
            if (slot.connected() && slot.visible()) {
                count++;
            }
        }
        return count;
    }

    public void setLayout(ViewportLayout layout) {
        this.layout = layout;
        if (this.viewportTable != null) {
            this.viewportTable = new ViewportTable(layout, this.viewportTable.metrics());
            this.bindResolvedViewports();
        }
    }

    public void setVisiblePlayerCount(int playerCount) {
        if (playerCount < 1 || playerCount > MAX_SLOTS) {
            throw new IllegalArgumentException("playerCount must be between 1 and " + MAX_SLOTS);
        }
        this.setLayout(layoutForPlayerCount(playerCount));
        for (int slotId = 0; slotId < MAX_SLOTS; slotId++) {
            PlayerSlot slot = this.slots[slotId];
            boolean active = slotId < playerCount;
            slot.setConnected(active);
            slot.setVisible(active);
            if (active) {
                slot.setViewportId(slotId);
                if (this.viewportTable != null) {
                    slot.bindViewport(this.viewportTable.viewport(slotId));
                }
            } else {
                slot.clearViewport();
            }
        }
    }

    public void setClientConnected(int slotId, boolean connected) {
        requireSlot(slotId);
        if (slotId == 0 && !connected) {
            throw new IllegalArgumentException("Slot 0 cannot be disconnected through PlayerSlots");
        }

        PlayerSlot slot = this.slots[slotId];
        slot.setConnected(connected);
        slot.setVisible(connected);
        if (connected) {
            slot.setViewportId(slotId);
        } else {
            slot.clearViewport();
        }
        this.rebuildLayoutForConnectedSlots();
    }

    public void assignViewport(int slotId, int viewportId) {
        requireSlot(slotId);
        this.layout.requireViewport(viewportId);
        PlayerSlot slot = this.slots[slotId];
        slot.setViewportId(viewportId);
        if (this.viewportTable != null) {
            slot.bindViewport(this.viewportTable.viewport(viewportId));
        }
    }

    public void rebuildViewports(WindowMetrics metrics) {
        this.viewportTable = new ViewportTable(this.layout, metrics);
        this.bindResolvedViewports();
    }

    private void rebuildLayoutForConnectedSlots() {
        int highestConnectedSlot = 0;
        for (PlayerSlot slot : this.slots) {
            if (slot.connected() && slot.visible()) {
                highestConnectedSlot = Math.max(highestConnectedSlot, slot.id());
            }
        }
        this.layout = layoutForHighestConnectedSlot(highestConnectedSlot);
        if (this.viewportTable != null) {
            this.viewportTable = new ViewportTable(this.layout, this.viewportTable.metrics());
            this.bindResolvedViewports();
        }
    }

    private void bindResolvedViewports() {
        for (PlayerSlot slot : this.slots) {
            if (slot.connected() && slot.visible() && this.layout.hasViewport(slot.viewportId())) {
                slot.bindViewport(this.viewportTable.viewport(slot.viewportId()));
            } else {
                slot.clearViewport();
            }
        }
    }

    private static void requireSlot(int id) {
        if (id < 0 || id >= MAX_SLOTS) {
            throw new IndexOutOfBoundsException("Player slot id " + id + " is not valid");
        }
    }

    public ViewportLayout layout() {
        return this.layout;
    }

    public ViewportTable viewportTable() {
        if (this.viewportTable == null) {
            throw new IllegalStateException("Viewport table has not been rebuilt yet");
        }
        return this.viewportTable;
    }

    private static ViewportLayout layoutForPlayerCount(int playerCount) {
        return switch (playerCount) {
            case 1 -> ViewportLayout.SINGLE;
            case 2 -> ViewportLayout.TWO_VERTICAL;
            case 3, 4 -> ViewportLayout.FOUR_GRID;
            default -> throw new IllegalArgumentException("Unsupported player count " + playerCount);
        };
    }

    private static ViewportLayout layoutForHighestConnectedSlot(int highestSlotId) {
        return switch (highestSlotId) {
            case 0 -> ViewportLayout.SINGLE;
            case 1 -> ViewportLayout.TWO_VERTICAL;
            case 2, 3 -> ViewportLayout.FOUR_GRID;
            default -> throw new IllegalArgumentException("Unsupported highest slot " + highestSlotId);
        };
    }
}
