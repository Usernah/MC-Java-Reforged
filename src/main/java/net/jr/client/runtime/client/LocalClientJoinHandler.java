package net.jr.client.runtime.client;

import java.util.Collection;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.input.InputDeviceRouter;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;
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

        int joinedSlot = slotId;
        try {
            joinWithoutPresenting(minecraft, deviceId, joinedSlot);
            presentJoinedPlayers(minecraft, java.util.List.of(joinedSlot));
            return joinedSlot;
        } catch (RuntimeException | Error error) {
            rollback(deviceId, joinedSlot);
            throw error;
        }
    }

    public static void joinWithoutPresenting(Minecraft minecraft, long deviceId, int slotId) {
        LocalClientSlotRegistry.requireSlotId(slotId);
        if (slotId == 0) {
            throw new IllegalArgumentException("Primary slot cannot be joined as a secondary player");
        }
        ClientRuntime runtime = ClientRuntime.INSTANCE;
        if (!LocalClientJoinPolicy.canStartJoin(deviceId, slotId)
            || runtime.clients().hasClient(slotId)
            || runtime.clients().sessionOrNull(slotId) != null
            || runtime.viewports().isPresented(slotId)) {
            throw new IllegalStateException("Local client slot " + slotId + " is not available");
        }

        boolean assigned = false;
        try {
            InputDeviceRouter.assignGamepad(deviceId, slotId);
            assigned = true;
            runtime.clients().ensureClient(minecraft, slotId);
        } catch (RuntimeException | Error error) {
            if (runtime.clients().hasClient(slotId)
                || runtime.clients().sessionOrNull(slotId) != null) {
                runtime.clients().disconnectSecondary(slotId);
            }
            if (assigned) {
                InputDeviceRouter.unassignGamepad(deviceId);
            }
            throw error;
        }
    }

    public static void presentJoinedPlayers(Minecraft minecraft, Collection<Integer> slotIds) {
        ClientRuntime runtime = ClientRuntime.INSTANCE;
        boolean changed = false;
        for (int slotId : slotIds) {
            if (!runtime.clients().hasClient(slotId)) {
                throw new IllegalStateException("Cannot present missing local client " + slotId);
            }
            if (!runtime.viewports().isPresented(slotId)) {
                runtime.viewports().present(slotId);
                changed = true;
            }
        }
        if (changed) {
            runtime.viewportResize().applyLayoutTransition(minecraft);
        }
        runtime.inputFocus().clampToSlots(runtime.viewports().presentedSlotIds());
    }

    private static void rollback(long deviceId, int slotId) {
        ClientRuntime runtime = ClientRuntime.INSTANCE;
        if (runtime.clients().hasClient(slotId)
            || runtime.clients().sessionOrNull(slotId) != null) {
            runtime.clients().disconnectSecondary(slotId);
        }
        InputDeviceRouter.unassignGamepad(deviceId);
        if (runtime.viewports().isPresented(slotId)) {
            runtime.viewports().hide(slotId);
            runtime.viewportResize().applyLayoutTransition(Minecraft.getInstance());
        }
        runtime.inputFocus().clampToSlots(runtime.viewports().presentedSlotIds());
    }
}
