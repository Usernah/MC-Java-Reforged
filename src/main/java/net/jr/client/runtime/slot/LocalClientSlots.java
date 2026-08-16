package net.jr.client.runtime.slot;

import java.util.ArrayList;
import java.util.List;
import net.jr.api.client.split.SplitOrientation;
import net.jr.client.runtime.viewport.ViewportLayout;
import net.jr.client.runtime.viewport.ViewportTable;
import net.jr.client.runtime.viewport.WindowMetrics;
import net.minecraft.client.Minecraft;

public final class LocalClientSlots {
    public static final int MAX_SLOTS = 4;

    private final LocalClientSlot[] slots = new LocalClientSlot[MAX_SLOTS];
    private ViewportLayout layout;
    private ViewportTable viewportTable;
    private SplitOrientation twoPlayerOrientation = SplitOrientation.VERTICAL;

    public LocalClientSlots() {
        this(ViewportLayout.FOUR_GRID);
    }

    public LocalClientSlots(ViewportLayout layout) {
        this.layout = layout;
        for (int id = 0; id < this.slots.length; id++) {
            this.slots[id] = new LocalClientSlot(id);
        }
    }

    public LocalClientSlot slot(int id) {
        requireSlot(id);
        return this.slots[id];
    }

    public List<LocalClientSlot> visibleSlots() {
        ArrayList<LocalClientSlot> visible = new ArrayList<>();
        for (LocalClientSlot slot : this.slots) {
            if (slot.drawable()) {
                visible.add(slot);
            }
        }
        return List.copyOf(visible);
    }

    public int presentSlotCount() {
        int count = 0;
        for (LocalClientSlot slot : this.slots) {
            if (slot.connected() && slot.visible()) {
                count++;
            }
        }
        return count;
    }

    public void setLayout(ViewportLayout layout) {
        this.layout = layout;
        if (this.viewportTable != null) {
            this.viewportTable = this.createViewportTable(layout, this.viewportTable.metrics());
            this.bindResolvedViewports();
        }
    }

    public void setVisiblePlayerCount(int playerCount) {
        if (playerCount < 1 || playerCount > MAX_SLOTS) {
            throw new IllegalArgumentException("playerCount must be between 1 and " + MAX_SLOTS);
        }
        for (int slotId = 0; slotId < MAX_SLOTS; slotId++) {
            LocalClientSlot slot = this.slots[slotId];
            boolean active = slotId < playerCount;
            slot.setConnected(active);
            slot.setVisible(active);
            if (active) {
                slot.setViewportId(slotId);
            } else {
                slot.clearViewport();
            }
        }
        this.setLayout(this.layoutForPlayerCount(playerCount));
    }

    public void setClientConnected(int slotId, boolean connected) {
        requireSlot(slotId);
        if (slotId == 0 && !connected) {
            throw new IllegalArgumentException("Slot 0 cannot be disconnected through PlayerSlots");
        }

        LocalClientSlot slot = this.slots[slotId];
        slot.setConnected(connected);
        slot.setVisible(connected);
        if (!connected) {
            slot.clearViewport();
        }
        this.rebuildLayoutForConnectedSlots();
    }

    public void assignViewport(int slotId, int viewportId) {
        requireSlot(slotId);
        this.layout.requireViewport(viewportId);
        LocalClientSlot slot = this.slots[slotId];
        slot.setViewportId(viewportId);
        if (this.viewportTable != null) {
            slot.bindViewport(this.viewportTable.viewport(viewportId));
        }
    }

    public void rebuildViewports(WindowMetrics metrics) {
        this.viewportTable = this.createViewportTable(this.layout, metrics);
        this.bindResolvedViewports();
    }

    private void rebuildLayoutForConnectedSlots() {
        int presentSlots = this.presentSlotCount();
        this.layout = this.layoutForPlayerCount(Math.max(1, presentSlots));

        int viewportId = 0;
        for (LocalClientSlot slot : this.slots) {
            if (slot.connected() && slot.visible()) {
                slot.setViewportId(viewportId++);
            } else {
                slot.clearViewport();
            }
        }

        if (this.viewportTable != null) {
            this.viewportTable = this.createViewportTable(this.layout, this.viewportTable.metrics());
            this.bindResolvedViewports();
        }
    }

    private void bindResolvedViewports() {
        for (LocalClientSlot slot : this.slots) {
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

    private ViewportTable createViewportTable(ViewportLayout layout, WindowMetrics metrics) {
        int[] requestedScales = new int[MAX_SLOTS];
        for (LocalClientSlot slot : this.slots) {
            if (slot.connected() && slot.visible() && layout.hasViewport(slot.viewportId())) {
                requestedScales[slot.viewportId()] = slot.optionsState().requestedGuiScale();
            }
        }
        Minecraft minecraft = Minecraft.getInstance();
        return new ViewportTable(layout, metrics, requestedScales, minecraft.isEnforceUnicode());
    }

    public void setTwoPlayerOrientation(SplitOrientation orientation) {
        this.twoPlayerOrientation = orientation;
        if (this.presentSlotCount() == 2) {
            this.setLayout(this.twoPlayerLayout());
        }
    }

    private ViewportLayout layoutForPlayerCount(int playerCount) {
        return switch (playerCount) {
            case 1 -> ViewportLayout.SINGLE;
            case 2 -> this.twoPlayerLayout();
            case 3, 4 -> ViewportLayout.FOUR_GRID;
            default -> throw new IllegalArgumentException("Unsupported player count " + playerCount);
        };
    }

    private ViewportLayout twoPlayerLayout() {
        return this.twoPlayerOrientation == SplitOrientation.HORIZONTAL
            ? ViewportLayout.TWO_HORIZONTAL
            : ViewportLayout.TWO_VERTICAL;
    }
}
