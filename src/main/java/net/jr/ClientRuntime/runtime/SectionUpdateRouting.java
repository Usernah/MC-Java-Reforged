package net.jr.ClientRuntime.runtime;

import javax.annotation.Nullable;
import net.jr.ClientRuntime.slot.PlayerSlot;
import net.minecraft.client.SectionUpdateTracker;
import net.minecraft.client.multiplayer.ClientLevel;

/** Routes Vanilla dirty-section notifications to every logical view of the changed dimension. */
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
        PlayerSlot owner = activeClient != null
            ? activeClient.rawSlot()
            : LocalPlayers.INSTANCE.primarySlot();
        ClientLevel ownerLevel = owner.renderState().level();

        /*
         * LevelExtractor is one shared vanilla engine. Between extraction passes
         * its `level` field belongs to the last extracted slot, so sourceLevel is
         * not authoritative for asynchronous packets handled by vanilla slot 0.
         * An explicit client scope identifies secondary connections; without one,
         * vanilla's packet path belongs to the primary client.
         */
        if (ownerLevel == null) {
            return false;
        }

        boolean routed = false;
        for (int slotId = 0; slotId < net.jr.ClientRuntime.slot.PlayerSlots.MAX_SLOTS; slotId++) {
            PlayerSlot slot = LocalPlayers.INSTANCE.slots().slot(slotId);
            ClientLevel slotLevel = slot.renderState().level();
            if (
                slot.connected()
                    && slotLevel != null
                    && slotLevel.dimension().equals(ownerLevel.dimension())
            ) {
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
