package net.jr.client.runtime.session;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.context.LocalClientScope;
import net.jr.client.runtime.input.InputDeviceRouter;
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
        disconnectSecondary(minecraft, slotId);
        return true;
    }

    public static void disconnectSecondary(Minecraft minecraft, int slotId) {
        ClientRuntime runtime = ClientRuntime.INSTANCE;
        runtime.clients().disconnectSecondary(slotId);
        InputDeviceRouter.unassignSlot(slotId);
        if (runtime.viewports().isPresented(slotId)) {
            runtime.viewports().hide(slotId);
        }
        runtime.inputFocus().clampToSlots(runtime.viewports().presentedSlotIds());
        runtime.viewportResize().applyLayoutTransition(minecraft);
    }

    public static void returnToPrimaryOnly(Minecraft minecraft) {
        ClientRuntime runtime = ClientRuntime.INSTANCE;
        for (int slotId = 1; slotId < net.jr.client.runtime.slot.LocalClientSlotRegistry.MAX_SLOTS; slotId++) {
            if (runtime.clients().hasClient(slotId) || runtime.clients().sessionOrNull(slotId) != null) {
                runtime.clients().disconnectSecondary(slotId);
            }
            InputDeviceRouter.unassignSlot(slotId);
        }
        runtime.viewports().presentPrimaryOnly();
        runtime.inputFocus().clampToSlots(runtime.viewports().presentedSlotIds());
        runtime.viewportResize().applyLayoutTransition(minecraft);
    }
}
