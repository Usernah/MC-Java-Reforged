package net.jr.ClientRuntime.runtime;

import javax.annotation.Nullable;
import net.jr.ClientRuntime.slot.PlayerSlot;
import net.minecraft.client.SectionUpdateTracker;
import net.minecraft.client.multiplayer.ClientLevel;

/** Routes Vanilla dirty-section notifications to the owning player's Vanilla tracker. */
public final class SectionUpdateRouting {
    private SectionUpdateRouting() {
    }

    public static boolean setDirty(
        @Nullable ClientLevel sourceLevel,
        int sectionX,
        int sectionY,
        int sectionZ,
        boolean playerChanged
    ) {
        LocalClient activeClient = Client.currentOrNull();
        if (activeClient != null) {
            PlayerSlot slot = activeClient.rawSlot();
            if (sourceLevel == null || slot.renderState().level() == sourceLevel) {
                setDirty(slot, sectionX, sectionY, sectionZ, playerChanged);
            }
            // A tracker created later starts fully dirty, so the event is still covered.
            return true;
        }

        boolean routed = false;
        for (int slotId = 0; slotId < net.jr.ClientRuntime.slot.PlayerSlots.MAX_SLOTS; slotId++) {
            PlayerSlot slot = LocalPlayers.INSTANCE.slots().slot(slotId);
            if (slot.connected() && (sourceLevel == null || slot.renderState().level() == sourceLevel)) {
                routed = true;
                setDirty(slot, sectionX, sectionY, sectionZ, playerChanged);
            }
        }
        return routed;
    }

    /** Returns true only for the final extraction that consumes a level's shared chunk delta buffer. */
    public static boolean isLastVisibleViewer(@Nullable ClientLevel level) {
        if (level == null) {
            return true;
        }
        Integer activeSlot = ActiveSlot.idOrNull();
        if (activeSlot == null) {
            return true;
        }
        for (PlayerSlot slot : LocalPlayers.INSTANCE.slots().visibleSlots()) {
            if (
                slot.id() > activeSlot
                    && slot.renderState().level() == level
                    && slot.gameplayState().player() != null
                    && !slot.gameplayState().player().isRemoved()
                    && !LocalPlayers.INSTANCE.sessions().isJoining(slot.id())
            ) {
                return false;
            }
        }
        return true;
    }

    private static void setDirty(
        PlayerSlot slot,
        int sectionX,
        int sectionY,
        int sectionZ,
        boolean playerChanged
    ) {
        SectionUpdateTracker tracker = slot.renderState().levelExtractionState().sectionUpdateTracker();
        if (tracker == null) {
            return;
        }
        tracker.setDirty(sectionX, sectionY, sectionZ, playerChanged);
    }
}
