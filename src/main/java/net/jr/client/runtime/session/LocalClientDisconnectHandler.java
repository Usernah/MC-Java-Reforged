package net.jr.client.runtime.session;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.input.InputApi;
import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.context.LocalClientScope;
import net.jr.experimental_features.pre_join.PreJoinRoster;
import net.minecraft.client.Minecraft;

public final class LocalClientDisconnectHandler {
    private LocalClientDisconnectHandler() {
    }

    public static boolean disconnectCurrentFromPauseMenu(Minecraft minecraft) {
        LocalClient client = LocalClientScope.currentClientOrNull();
        int slotId = client == null ? 0 : client.slotId();
        if (slotId == 0) {
            return false;
        }
        PreJoinRoster.forget(slotId);
        disconnectSecondary(minecraft, slotId);
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

    public static void returnToPrimaryOnly(Minecraft minecraft) {
        ClientRuntime runtime = ClientRuntime.INSTANCE;
        for (int slotId = 1; slotId < net.jr.client.runtime.slot.LocalClientSlotRegistry.MAX_SLOTS; slotId++) {
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
        PreJoinRoster.forgetDisconnectedDevices(runtime.input().devices().gamepads());
        runtime.viewports().presentPrimaryOnly();
        runtime.inputFocus().clampToSlots(runtime.viewports().presentedSlotIds());
        runtime.viewportResize().applyLayoutTransition(minecraft);
    }
}
