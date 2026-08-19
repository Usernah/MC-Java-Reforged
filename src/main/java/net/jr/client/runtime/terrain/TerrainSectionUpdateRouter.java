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
        LocalClientSlot owner = activeClient != null
            ? activeClient.slot()
            : ClientRuntime.INSTANCE.slots().primary();
        ClientLevel ownerLevel = owner.renderState().level();
        if (ownerLevel == null) {
            return false;
        }

        boolean routed = false;
        for (LocalClient client : ClientRuntime.INSTANCE.clients().all()) {
            LocalClientSlot slot = client.slot();
            ClientLevel slotLevel = slot.renderState().level();
            if (slotLevel != null && slotLevel.dimension().equals(ownerLevel.dimension())) {
                routed = true;
                setDirty(slot, sectionX, sectionY, sectionZ, playerChanged);
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
}
