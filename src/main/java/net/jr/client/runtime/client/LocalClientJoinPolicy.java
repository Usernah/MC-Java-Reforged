package net.jr.client.runtime.client;

import javax.annotation.Nullable;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.input.InputDeviceRouter;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;
import net.jr.api.client.split.SplitScreen;

public final class LocalClientJoinPolicy {
    private LocalClientJoinPolicy() {
    }

    public static boolean canStartJoin(long deviceId) {
        return InputDeviceRouter.connectedGamepads().contains(deviceId)
            && InputDeviceRouter.assignedSlot(deviceId) == null
            && !InputDeviceRouter.isAwaitingNeutral(deviceId)
            && nextFreeSecondarySlot() != null;
    }

    /**
     * Lets the pre-join flow recreate a secondary client from a retained
     * device-to-slot association. Regular in-world joins still require an
     * unassigned controller through {@link #canStartJoin(long)}.
     */
    public static boolean canStartJoin(long deviceId, int slotId) {
        LocalClientSlotRegistry.requireSlotId(slotId);
        Integer assignedSlot = InputDeviceRouter.assignedSlot(deviceId);
        return slotId != 0
            && InputDeviceRouter.connectedGamepads().contains(deviceId)
            && (assignedSlot == null || assignedSlot == slotId)
            && !InputDeviceRouter.isAwaitingNeutral(deviceId)
            && isFreeSecondarySlot(slotId);
    }

    public static boolean shouldShowJoinPrompt() {
        if (!SplitScreen.hasRequiredControllerCountForJoin()) {
            return false;
        }
        if (nextFreeSecondarySlot() == null) {
            return false;
        }
        return InputDeviceRouter.connectedGamepads().stream()
            .anyMatch(deviceId -> InputDeviceRouter.assignedSlot(deviceId) == null
                && !InputDeviceRouter.isAwaitingNeutral(deviceId));
    }

    public static boolean hasFreeSecondarySlot() {
        return nextFreeSecondarySlot() != null;
    }

    @Nullable
    public static Integer nextFreeSecondarySlot() {
        for (int slotId = 1; slotId < LocalClientSlotRegistry.MAX_SLOTS; slotId++) {
            if (isFreeSecondarySlot(slotId)) {
                return slotId;
            }
        }
        return null;
    }

    private static boolean isFreeSecondarySlot(int slotId) {
        ClientRuntime runtime = ClientRuntime.INSTANCE;
        return !runtime.clients().hasClient(slotId)
            && runtime.clients().sessionOrNull(slotId) == null
            && !runtime.viewports().isPresented(slotId);
    }
}
