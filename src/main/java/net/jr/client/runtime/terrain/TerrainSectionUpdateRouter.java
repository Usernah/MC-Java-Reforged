package net.jr.client.runtime.terrain;

import javax.annotation.Nullable;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.context.LocalClientScope;
import net.jr.client.runtime.client.LocalClientReadinessPolicy;
import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.minecraft.client.SectionUpdateTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;

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
        LocalClient activeClient = LocalClientScope.currentClientOrNull();
        if (activeClient != null && activeClient.level() != null) {
            setDirty(activeClient.slot(), sectionX, sectionY, sectionZ, playerChanged);
            markSharedRevision(activeClient.level(), sectionX, sectionY, sectionZ);
            return true;
        }

        for (LocalClient client : ClientRuntime.INSTANCE.clients().all()) {
            if (client.level() == sourceLevel) {
                setDirty(client.slot(), sectionX, sectionY, sectionZ, playerChanged);
                markSharedRevision(sourceLevel, sectionX, sectionY, sectionZ);
                return true;
            }
        }

        // Unknown unscoped callers retain the conservative fan-out so an
        // update can never be lost. Packet and tick paths are always scoped.
        boolean routed = false;
        if (sourceLevel != null) {
            for (LocalClient client : ClientRuntime.INSTANCE.clients().all()) {
                ClientLevel level = client.level();
                if (level != null && level.dimension().equals(sourceLevel.dimension())) {
                    setDirty(client.slot(), sectionX, sectionY, sectionZ, playerChanged);
                    routed = true;
                }
            }
            if (routed) {
                markSharedRevision(sourceLevel, sectionX, sectionY, sectionZ);
            }
        }
        return routed;
    }

    public static boolean isLastVisibleViewer(@Nullable ClientLevel level) {
        if (level == null) {
            return true;
        }
        Integer activeSlot = SlotScope.idOrNull();
        if (activeSlot == null) {
            return true;
        }
        for (LocalClientSlot slot : ClientRuntime.INSTANCE.viewports().drawableSlots()) {
            if (
                slot.id() > activeSlot
                    && slot.renderState().level() == level
                    && ClientRuntime.INSTANCE.clients().clientOrNull(slot.id()) != null
                    && LocalClientReadinessPolicy.worldReady(slot.id())
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

    private static void markSharedRevision(
        ClientLevel level,
        int sectionX,
        int sectionY,
        int sectionZ
    ) {
        SharedTerrainStore store = TerrainStateStore.nullableTerrainStore();
        if (store != null) {
            store.markDirty(level, SectionPos.asLong(sectionX, sectionY, sectionZ));
        }
    }
}
