package net.jr.client.runtime.input;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.jr.api.client.input.IGamepad;
import net.jr.api.client.input.IPreJoin;
import net.jr.api.client.split.SplitScreen;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;
import net.minecraft.client.Minecraft;

/** Coordinates candidates, slot assignment and local-client creation. */
public final class LocalJoinService {
    private final InputDeviceRegistry devices;
    private final InputAssignment assignments;

    LocalJoinService(InputDeviceRegistry devices, InputAssignment assignments) {
        this.devices = devices;
        this.assignments = assignments;
    }

    public List<IPreJoin> preJoinCandidates() {
        return this.devices.gamepads().stream()
            .filter(gamepad -> {
                Integer slotId = this.assignments.assignedSlot(gamepad);
                return slotId == null || !ClientRuntime.INSTANCE.clients().hasClient(slotId);
            })
            .map(gamepad -> (IPreJoin) new LocalJoinCandidate(gamepad))
            .toList();
    }

    public boolean canStartJoin(IGamepad gamepad) {
        return gamepad.connected()
            && this.devices.gamepads().contains(gamepad)
            && this.assignments.assignedSlot(gamepad) == null
            && !this.assignments.awaitingNeutral(gamepad)
            && this.nextFreeSecondarySlot() != null;
    }

    /** Allows prejoin to recreate the same secondary slot in the next world. */
    public boolean canStartJoin(IGamepad gamepad, int slotId) {
        LocalClientSlotRegistry.requireSlotId(slotId);
        Integer assigned = this.assignments.assignedSlot(gamepad);
        return slotId != 0
            && gamepad.connected()
            && this.devices.gamepads().contains(gamepad)
            && (assigned == null || assigned == slotId)
            && !this.assignments.awaitingNeutral(gamepad)
            && this.isFreeSecondarySlot(slotId);
    }

    public boolean shouldShowJoinPrompt() {
        return SplitScreen.hasRequiredControllerCountForJoin()
            && this.nextFreeSecondarySlot() != null
            && this.devices.gamepads().stream().anyMatch(gamepad ->
                this.assignments.assignedSlot(gamepad) == null
                    && !this.assignments.awaitingNeutral(gamepad));
    }

    public boolean hasFreeSecondarySlot() {
        return this.nextFreeSecondarySlot() != null;
    }

    @Nullable
    public Integer nextFreeSecondarySlot() {
        for (int slotId = 1; slotId < LocalClientSlotRegistry.MAX_SLOTS; slotId++) {
            if (this.isFreeSecondarySlot(slotId)) {
                return slotId;
            }
        }
        return null;
    }

    public int join(Minecraft minecraft, IGamepad gamepad) {
        if (!this.canStartJoin(gamepad)) {
            throw new IllegalStateException("No local client slot is available for this gamepad");
        }
        Integer slotId = this.nextFreeSecondarySlot();
        if (slotId == null) {
            throw new IllegalStateException("No secondary local client slot is available");
        }

        try {
            this.joinWithoutPresenting(minecraft, gamepad, slotId);
            this.presentJoinedPlayers(minecraft, List.of(slotId));
            return slotId;
        } catch (RuntimeException | Error error) {
            this.rollback(gamepad, slotId);
            throw error;
        }
    }

    public void joinWithoutPresenting(Minecraft minecraft, IGamepad gamepad, int slotId) {
        LocalClientSlotRegistry.requireSlotId(slotId);
        if (slotId == 0) {
            throw new IllegalArgumentException("Primary slot cannot be joined as a secondary player");
        }
        ClientRuntime runtime = ClientRuntime.INSTANCE;
        if (!this.canStartJoin(gamepad, slotId)
            || runtime.clients().hasClient(slotId)
            || runtime.clients().sessionOrNull(slotId) != null
            || runtime.viewports().isPresented(slotId)) {
            throw new IllegalStateException("Local client slot " + slotId + " is not available");
        }

        boolean assigned = false;
        try {
            this.assignments.assign(gamepad, slotId);
            assigned = true;
            runtime.slots().slot(slotId).inputState().markGamepadInput();
            runtime.clients().ensureClient(minecraft, slotId);
        } catch (RuntimeException | Error error) {
            if (runtime.clients().hasClient(slotId)
                || runtime.clients().sessionOrNull(slotId) != null) {
                runtime.clients().disconnectSecondary(slotId);
            }
            if (assigned) {
                this.assignments.unassign(gamepad);
            }
            throw error;
        }
    }

    public void presentJoinedPlayers(Minecraft minecraft, Collection<Integer> slotIds) {
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

    private boolean isFreeSecondarySlot(int slotId) {
        ClientRuntime runtime = ClientRuntime.INSTANCE;
        return !runtime.clients().hasClient(slotId)
            && runtime.clients().sessionOrNull(slotId) == null
            && !runtime.viewports().isPresented(slotId);
    }

    private void rollback(IGamepad gamepad, int slotId) {
        ClientRuntime runtime = ClientRuntime.INSTANCE;
        if (runtime.clients().hasClient(slotId)
            || runtime.clients().sessionOrNull(slotId) != null) {
            runtime.clients().disconnectSecondary(slotId);
        }
        this.assignments.unassign(gamepad);
        if (runtime.viewports().isPresented(slotId)) {
            runtime.viewports().hide(slotId);
            runtime.viewportResize().applyLayoutTransition(Minecraft.getInstance());
        }
        runtime.inputFocus().clampToSlots(runtime.viewports().presentedSlotIds());
    }
}
