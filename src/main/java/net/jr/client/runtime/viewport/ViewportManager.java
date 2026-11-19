package net.jr.client.runtime.viewport;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.jr.api.client.split.SplitOrientation;
import net.jr.client.runtime.slot.ClientSlots;
import net.jr.client.runtime.slot.Slots;
import net.jr.client.ui.dsl.ScreenEditorHooks;
import net.minecraft.client.Minecraft;

public final class ViewportManager {
    private final ClientSlots slots;
    private final boolean[] presented = new boolean[ClientSlots.MAX_SLOTS];
    private final int[] viewportIds = new int[ClientSlots.MAX_SLOTS];
    private ViewportLayout layout = ViewportLayout.SINGLE;
    private SplitOrientation twoPlayerOrientation = SplitOrientation.VERTICAL;
    @Nullable
    private ViewportTable viewportTable;
    @Nullable
    private ViewportTable fullWindowViewportTable;
    @Nullable
    private WindowMetrics windowMetrics;

    public ViewportManager(ClientSlots slots) {
        this.slots = slots;
        this.presented[0] = true;
        this.reassignViewportIds();
    }

    public boolean isPresented(int slotId) {
        ClientSlots.requireSlotId(slotId);
        return this.presented[slotId];
    }

    public void present(int slotId) {
        ClientSlots.requireSlotId(slotId);
        if (this.presented[slotId]) {
            return;
        }
        this.presented[slotId] = true;
        this.rebuildLayoutForPresentation();
    }

    public void hide(int slotId) {
        ClientSlots.requireSlotId(slotId);
        if (slotId == 0) {
            throw new IllegalArgumentException("Primary slot cannot be hidden");
        }
        if (!this.presented[slotId]) {
            return;
        }
        this.presented[slotId] = false;
        this.rebuildLayoutForPresentation();
    }

    public void presentPrimaryOnly() {
        for (int slotId = 1; slotId < this.presented.length; slotId++) {
            this.presented[slotId] = false;
        }
        this.presented[0] = true;
        this.rebuildLayoutForPresentation();
    }

    public int presentedCount() {
        int count = 0;
        for (boolean value : this.presented) {
            if (value) {
                count++;
            }
        }
        return count;
    }

    public List<Integer> presentedSlotIds() {
        ArrayList<Integer> result = new ArrayList<>();
        for (int slotId = 0; slotId < this.presented.length; slotId++) {
            if (this.presented[slotId]) {
                result.add(slotId);
            }
        }
        return List.copyOf(result);
    }

    public List<Slots> presentedSlots() {
        return this.presentedSlotIds().stream().map(this.slots::slot).toList();
    }

    public List<Slots> drawableSlots() {
        return this.drawableViewports().stream().map(SlotViewport::slot).toList();
    }


    public List<SlotViewport> drawableViewports() {
        if (this.viewportTable == null) {
            return List.of();
        }
        ArrayList<SlotViewport> result = new ArrayList<>();
        for (Slots slot : this.presentedSlots()) {
            ViewportArea viewport = this.viewportOrNull(slot.id());
            if (viewport != null) {
                result.add(new SlotViewport(slot, viewport));
            }
        }
        return List.copyOf(result);
    }

    public int viewportId(int slotId) {
        ClientSlots.requireSlotId(slotId);
        if (!this.presented[slotId]) {
            throw new IllegalStateException("Slot " + slotId + " is not presented");
        }
        return this.viewportIds[slotId];
    }

    public boolean hasViewport(int slotId) {
        ClientSlots.requireSlotId(slotId);
        ViewportLayout effectiveLayout = this.effectiveLayout();
        return this.presented[slotId]
            && this.viewportTable != null
            && effectiveLayout.hasViewport(this.viewportIds[slotId]);
    }

    @Nullable
    public ViewportArea viewportOrNull(int slotId) {
        if (!this.hasViewport(slotId)) {
            return null;
        }
        return this.effectiveViewportTable().viewport(this.viewportIds[slotId]);
    }

    public ViewportArea viewport(int slotId) {
        ViewportArea viewport = this.viewportOrNull(slotId);
        if (viewport == null) {
            throw new IllegalStateException("Slot " + slotId + " has no resolved viewport");
        }
        return viewport;
    }

    public ViewportLayout layout() {
        return this.effectiveLayout();
    }

    public int viewportCount() {
        ViewportTable table = this.usesEditorLayout()
            ? this.fullWindowViewportTable
            : this.viewportTable;
        return table != null ? table.size() : this.presentedCount();
    }

    public boolean hasWindowMetrics() {
        return this.windowMetrics != null;
    }

    public WindowMetrics windowMetrics() {
        if (this.windowMetrics == null) {
            throw new IllegalStateException("Window metrics have not been captured yet");
        }
        return this.windowMetrics;
    }

    public ViewportTable viewportTable() {
        return this.effectiveViewportTable();
    }

    public void rebuild(WindowMetrics metrics) {
        this.windowMetrics = metrics;
        this.viewportTable = this.createViewportTable(this.layout, metrics);
        this.fullWindowViewportTable = this.createViewportTable(ViewportLayout.SINGLE, metrics);
    }

    public void setTwoPlayerOrientation(SplitOrientation orientation) {
        this.twoPlayerOrientation = orientation;
        if (this.presentedCount() == 2) {
            this.layout = this.twoPlayerLayout();
            this.rebuildExistingTable();
        }
    }

    private void rebuildLayoutForPresentation() {
        this.layout = this.layoutForCount(this.presentedCount());
        this.reassignViewportIds();
        this.rebuildExistingTable();
    }

    private void reassignViewportIds() {
        for (int slotId = 0; slotId < this.presented.length; slotId++) {
            if (!this.presented[slotId]) {
                this.viewportIds[slotId] = -1;
            }
        }

        int nextViewportId = 0;

        for (int oldViewportId = 0; oldViewportId < this.viewportIds.length; oldViewportId++) {
            for (int slotId = 0; slotId < this.presented.length; slotId++) {
                if (this.presented[slotId] && this.viewportIds[slotId] == oldViewportId) {
                    this.viewportIds[slotId] = nextViewportId++;
                    break;
                }
            }
        }

        for (int slotId = 0; slotId < this.presented.length; slotId++) {
            if (this.presented[slotId] && this.viewportIds[slotId] == -1) {
                this.viewportIds[slotId] = nextViewportId++;
            }
        }
    }
    private int getFirstAvailableViewportId() {
        int candidateId = 0;
        while (true) {
            boolean idInUse = false;

            for (int id : this.viewportIds) {
                if (id == candidateId) {
                    idInUse = true;
                    break;
                }
            }

            if (!idInUse) {
                return candidateId;
            }
            candidateId++;
        }
    }

    private void rebuildExistingTable() {
        if (this.windowMetrics != null) {
            this.viewportTable = this.createViewportTable(this.layout, this.windowMetrics);
            this.fullWindowViewportTable = this.createViewportTable(
                ViewportLayout.SINGLE,
                this.windowMetrics
            );
        }
    }

    private ViewportLayout layoutForCount(int count) {
        return switch (count) {
            case 1 -> ViewportLayout.SINGLE;
            case 2 -> this.twoPlayerLayout();
            case 3, 4 -> ViewportLayout.FOUR_GRID;
            default -> throw new IllegalStateException("Unsupported presented slot count " + count);
        };
    }

    private ViewportLayout twoPlayerLayout() {
        return this.twoPlayerOrientation == SplitOrientation.HORIZONTAL
            ? ViewportLayout.TWO_HORIZONTAL
            : ViewportLayout.TWO_VERTICAL;
    }

    public boolean usesEditorLayout() {
        return ScreenEditorHooks.fullWindowFor(
            this.slots.slot(0).screenState().screen()
        );
    }

    private ViewportLayout effectiveLayout() {
        return this.usesEditorLayout() ? ViewportLayout.SINGLE : this.layout;
    }

    private ViewportTable effectiveViewportTable() {
        ViewportTable table = this.usesEditorLayout()
            ? this.fullWindowViewportTable
            : this.viewportTable;
        if (table == null) {
            throw new IllegalStateException("Viewport table has not been rebuilt yet");
        }
        return table;
    }

    private ViewportTable createViewportTable(ViewportLayout layout, WindowMetrics metrics) {
        int[] requestedScales = new int[ClientSlots.MAX_SLOTS];
        for (int slotId = 0; slotId < this.presented.length; slotId++) {
            if (!this.presented[slotId]) {
                continue;
            }
            int viewportId = this.viewportIds[slotId];
            if (layout.hasViewport(viewportId)) {
                requestedScales[viewportId] = this.slots.slot(slotId).optionsState().requestedGuiScale();
            }
        }
        return new ViewportTable(
            layout,
            metrics,
            requestedScales,
            Minecraft.getInstance().isEnforceUnicode()
        );
    }

    public record SlotViewport(Slots slot, ViewportArea viewport) {
    }
}
