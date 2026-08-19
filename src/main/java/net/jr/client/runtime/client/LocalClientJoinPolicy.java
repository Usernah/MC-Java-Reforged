package net.jr.client.runtime.client;

import javax.annotation.Nullable;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.input.InputDeviceRouter;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;

public final class LocalClientJoinPolicy {
    private LocalClientJoinPolicy() {
    }

    public static boolean canStartJoin(long deviceId) {
        return InputDeviceRouter.connectedGamepads().contains(deviceId)
            && InputDeviceRouter.assignedSlot(deviceId) == null
            && nextFreeSecondarySlot() != null;
    }

    public static boolean shouldShowJoinPrompt() {
        int connectedControllers = InputDeviceRouter.connectedGamepads().size();
        if (connectedControllers < 2) {
            return false;
        }
        if (nextFreeSecondarySlot() == null) {
            return false;
        }
        return InputDeviceRouter.connectedGamepads().stream()
            .anyMatch(deviceId -> InputDeviceRouter.assignedSlot(deviceId) == null);
    }

    public static boolean hasFreeSecondarySlot() {
        return nextFreeSecondarySlot() != null;
    }

    @Nullable
    public static Integer nextFreeSecondarySlot() {
        ClientRuntime runtime = ClientRuntime.INSTANCE;
        for (int slotId = 1; slotId < LocalClientSlotRegistry.MAX_SLOTS; slotId++) {
            if (!runtime.clients().hasClient(slotId)
                && runtime.clients().sessionOrNull(slotId) == null
                && !runtime.viewports().isPresented(slotId)) {
                return slotId;
            }
        }
        return null;
    }
}
