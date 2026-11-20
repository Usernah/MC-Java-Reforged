package net.jr.client.runtime.terrain;

import javax.annotation.Nullable;

import net.jr.api.client.local.Client;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotExecution;
import net.jr.client.runtime.client.ReadinessPolicy;
import net.jr.client.runtime.client.ClientInstance;
import net.jr.client.runtime.slot.Slots;
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
        ClientLevel ownerLevel = sourceLevel;
        if (ownerLevel == null) {
            ClientInstance activeClient = Client.currentOrNull();
            ownerLevel = activeClient != null ? activeClient.level() : null;
        }
        if (ownerLevel == null) {
            return false;
        }

        boolean routed = false;
        for (ClientInstance client : ClientRuntime.INSTANCE.clients().all()) {
            Slots slot = client.slot();
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
        Integer activeSlot = SlotExecution.currentSlotIdOrNull();
        if (activeSlot == null) {
            return true;
        }
        for (Slots slot : ClientRuntime.INSTANCE.viewports().drawableSlots()) {
            if (
                slot.id() > activeSlot
                    && slot.renderState().level() == level
                    && ClientRuntime.INSTANCE.clients().clientOrNull(slot.id()) != null
                    && ReadinessPolicy.worldReady(slot.id())
            ) {
                return false;
            }
        }
        return true;
    }

    private static void setDirty(
        Slots slot,
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
