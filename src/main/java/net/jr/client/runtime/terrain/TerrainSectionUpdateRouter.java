package net.jr.client.runtime.terrain;

import javax.annotation.Nullable;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.ActiveClientSlot;
import net.jr.client.runtime.context.LocalClientAcces;
import net.jr.client.runtime.context.LocalClient;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.jr.client.runtime.slot.LocalClientSlots;
import net.minecraft.client.SectionUpdateTracker;
import net.minecraft.client.multiplayer.ClientLevel;

/** Routes Vanilla dirty-section notifications to every logical view of the changed dimension. */
public final class TerrainSectionUpdateRouter {
    private TerrainSectionUpdateRouter() {
    }

    public static boolean setDirty(
        @Nullable ClientLevel sourceLevel,
        int sectionX,
        int sectionY,
        int sectionZ,
        boolean playerChanged
    ) {
        LocalClient activeClient = LocalClientAcces.currentOrNull();
        LocalClientSlot owner = activeClient != null
            ? activeClient.slot()
            : ClientRuntime.INSTANCE.primarySlot();
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
        for (int slotId = 0; slotId < LocalClientSlots.MAX_SLOTS; slotId++) {
            LocalClientSlot slot = ClientRuntime.INSTANCE.slots().slot(slotId);
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
        Integer activeSlot = ActiveClientSlot.idOrNull();
        if (activeSlot == null) {
            return true;
        }
        for (LocalClientSlot slot : ClientRuntime.INSTANCE.slots().visibleSlots()) {
            if (
                slot.id() > activeSlot
                    && slot.renderState().level() == level
                    && slot.gameplayState().player() != null
                    && !slot.gameplayState().player().isRemoved()
                    && !ClientRuntime.INSTANCE.sessions().isJoining(slot.id())
            ) {
                return false;
            }
        }
        return true;
    }

    private static void setDirty(
        LocalClientSlot slot,
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
