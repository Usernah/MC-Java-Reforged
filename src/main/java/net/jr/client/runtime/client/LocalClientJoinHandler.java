package net.jr.client.runtime.client;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.input.InputDeviceRouter;
import net.minecraft.client.Minecraft;

public final class LocalClientJoinHandler {
    private LocalClientJoinHandler() {
    }

    public static int join(Minecraft minecraft, long deviceId) {
        if (!LocalClientJoinPolicy.canStartJoin(deviceId)) {
            throw new IllegalStateException("No local client slot is available for this gamepad");
        }

        Integer slotId = LocalClientJoinPolicy.nextFreeSecondarySlot();
        if (slotId == null) {
            throw new IllegalStateException("No secondary local client slot is available");
        }

        ClientRuntime runtime = ClientRuntime.INSTANCE;
        boolean presented = false;
        boolean assigned = false;

        try {
            runtime.viewports().present(slotId);
            presented = true;
            runtime.viewportResize().applyLayoutTransition(minecraft);
            runtime.inputFocus().clampToSlots(runtime.viewports().presentedSlotIds());

            InputDeviceRouter.assignGamepad(deviceId, slotId);
            assigned = true;

            runtime.clients().ensureClient(minecraft, slotId);
            return slotId;
        } catch (RuntimeException | Error error) {
            if (runtime.clients().hasClient(slotId)
                || runtime.clients().sessionOrNull(slotId) != null) {
                runtime.clients().disconnectSecondary(slotId);
            }

            if (assigned) {
                InputDeviceRouter.unassignGamepad(deviceId);
            }

            if (presented && runtime.viewports().isPresented(slotId)) {
                runtime.viewports().hide(slotId);
                runtime.viewportResize().applyLayoutTransition(minecraft);
            }

            runtime.inputFocus().clampToSlots(runtime.viewports().presentedSlotIds());
            throw error;
        }
    }
}
