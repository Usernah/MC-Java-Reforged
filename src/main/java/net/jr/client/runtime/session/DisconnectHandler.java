package net.jr.client.runtime.session;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.input.InputApi;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.slot.ClientSlots;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;

public final class DisconnectHandler {
    private DisconnectHandler() {
    }

    public static boolean disconnectCurrentSecondary(Minecraft minecraft) {
        Integer slotId = SlotScope.idOrNull();
        if (slotId == null || slotId == 0) {
            return false;
        }
        if (minecraft.isSameThread()) {
            disconnectSecondary(minecraft, slotId);
        } else {
            minecraft.execute(() -> disconnectSecondary(minecraft, slotId));
        }
        return true;
    }

    public static void disconnectSecondary(Minecraft minecraft, int slotId) {
        ClientRuntime runtime = ClientRuntime.INSTANCE;
        InputApi.resetSlotInputState(slotId);
        runtime.clients().disconnectSecondary(slotId);
        runtime.input().mutableAssignments().unassignSlot(slotId);
        if (runtime.viewports().isPresented(slotId)) {
            runtime.viewports().hide(slotId);
        }
        runtime.inputFocus().clampToSlots(runtime.viewports().presentedSlotIds());
        runtime.viewportResize().applyLayoutTransition(minecraft);
    }

    public static void disconnectSecondaryConnection(
        Minecraft minecraft,
        int slotId,
        Connection connection
    ) {
        ClientRuntime runtime = ClientRuntime.INSTANCE;
        var session = runtime.clients().sessionOrNull(slotId);
        if (session == null || !session.owns(connection)) {
            runtime.connections().unbind(connection);
            return;
        }
        disconnectSecondary(minecraft, slotId);
    }

    public static void returnToPrimaryOnly(Minecraft minecraft) {
        ClientRuntime runtime = ClientRuntime.INSTANCE;
        for (int slotId = 1; slotId < ClientSlots.MAX_SLOTS; slotId++) {
            InputApi.resetSlotInputState(slotId);
            if (runtime.clients().hasClient(slotId) || runtime.clients().sessionOrNull(slotId) != null) {
                runtime.clients().disconnectSecondary(slotId);
            }
        }
        // Keep controllers that made it out of the world connected. Pre-join
        // can then recreate their same split slots in the next world. A device
        // that remained disconnected is deliberately forgotten at this
        // session boundary instead.
        runtime.input().mutableAssignments().clearDisconnectedAssignments();
        runtime.viewports().presentPrimaryOnly();
        runtime.inputFocus().clampToSlots(runtime.viewports().presentedSlotIds());
        runtime.viewportResize().applyLayoutTransition(minecraft);
    }
}
