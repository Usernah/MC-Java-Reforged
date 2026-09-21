package net.jr.client.runtime.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.jr.api.client.input.IGamepad;
import net.jr.api.client.input.IMutableInputAssignment;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;

/** Owns controller assignment, neutralization and unique physical reconnects. */
public final class InputAssignment implements IMutableInputAssignment {
    public static final int UNASSIGNED_SLOT = -1;
    public static final int KEYBOARD_MOUSE_SLOT = 0;

    private final ManagedInputDeviceRegistry devices;
    private final BooleanSupplier primaryMenuInput;
    private final BooleanSupplier simulationConnected;
    private final Map<IGamepad, Integer> assignedSlots = new HashMap<>();
    private final Map<IGamepad, PhysicalGamepadIdentity> assignedIdentities = new HashMap<>();
    private final Set<IGamepad> neutralizationPending = new HashSet<>();

    InputAssignment(
        ManagedInputDeviceRegistry devices,
        BooleanSupplier primaryMenuInput,
        BooleanSupplier simulationConnected
    ) {
        this.devices = devices;
        this.primaryMenuInput = primaryMenuInput;
        this.simulationConnected = simulationConnected;
    }

    @Nullable
    @Override
    public Integer assignedSlot(IGamepad gamepad) {
        return this.assignedSlots.get(gamepad);
    }

    @Override
    public int effectiveSlot(IGamepad gamepad) {
        this.refreshNeutralizationState();
        return this.neutralizationPending.contains(gamepad)
            ? UNASSIGNED_SLOT
            : this.assignedSlots.getOrDefault(gamepad, KEYBOARD_MOUSE_SLOT);
    }

    @Override
    public boolean awaitingNeutral(IGamepad gamepad) {
        this.refreshNeutralizationState();
        return this.neutralizationPending.contains(gamepad);
    }

    @Override
    public List<IGamepad> gamepadsForSlot(int slotId) {
        LocalClientSlotRegistry.requireSlotId(slotId);
        this.refreshNeutralizationState();
        List<IGamepad> result = new ArrayList<>();
        boolean primaryMenuInput = slotId == KEYBOARD_MOUSE_SLOT
            && this.primaryMenuInput.getAsBoolean();
        for (IGamepad gamepad : this.devices.gamepads()) {
            Integer assigned = this.assignedSlots.get(gamepad);
            if ((assigned == null && slotId == KEYBOARD_MOUSE_SLOT
                && !this.neutralizationPending.contains(gamepad))
                || (assigned != null && (assigned == slotId || primaryMenuInput))) {
                result.add(gamepad);
            }
        }
        return List.copyOf(result);
    }

    @Override
    public boolean hasGamepadForSlot(int slotId) {
        return (slotId == KEYBOARD_MOUSE_SLOT && this.simulationConnected.getAsBoolean())
            || !this.gamepadsForSlot(slotId).isEmpty();
    }

    @Override
    public boolean hasDisconnectedGamepadForSlot(int slotId) {
        LocalClientSlotRegistry.requireSlotId(slotId);
        return this.assignedSlots.entrySet().stream().anyMatch(entry ->
            entry.getValue() == slotId && !entry.getKey().connected());
    }

    @Override
    public void assign(IGamepad gamepad, int slotId) {
        LocalClientSlotRegistry.requireSlotId(slotId);
        if (!gamepad.connected()) {
            throw new IllegalArgumentException("Cannot assign disconnected gamepad");
        }
        this.restoreReconnectedAssignments(this.devices.gamepads());
        this.neutralizationPending.remove(gamepad);
        this.assignedSlots.put(gamepad, slotId);
        PhysicalGamepadIdentity identity = physicalIdentity(gamepad);
        if (identity != null) {
            this.assignedIdentities.put(gamepad, identity);
        } else {
            this.assignedIdentities.remove(gamepad);
        }
    }

    @Override
    public void unassign(IGamepad gamepad) {
        Integer removedSlot = this.assignedSlots.remove(gamepad);
        this.assignedIdentities.remove(gamepad);
        if (removedSlot != null) {
            this.requireNeutralBeforePrimaryFallback(gamepad);
        }
    }

    @Override
    public void unassignSlot(int slotId) {
        LocalClientSlotRegistry.requireSlotId(slotId);
        List<IGamepad> released = this.assignedSlots.entrySet().stream()
            .filter(entry -> entry.getValue() == slotId)
            .map(Map.Entry::getKey).toList();
        released.forEach(this.assignedSlots::remove);
        released.forEach(this.assignedIdentities::remove);
        released.forEach(this::requireNeutralBeforePrimaryFallback);
    }

    @Override
    public void pruneDisconnectedDevices() {
        List<IGamepad> connected = this.devices.gamepads();
        this.restoreReconnectedAssignments(connected);
        this.neutralizationPending.removeIf(gamepad -> !gamepad.connected());
        this.refreshNeutralizationState();
        this.devices.evictDisconnectedExcept(this.assignedSlots.keySet());
    }

    @Override
    public void clearDisconnectedAssignments() {
        List<IGamepad> connected = this.devices.gamepads();
        this.restoreReconnectedAssignments(connected);
        List<IGamepad> released = this.assignedSlots.keySet().stream()
            .filter(gamepad -> !gamepad.connected()).toList();
        released.forEach(this.assignedSlots::remove);
        released.forEach(this.assignedIdentities::remove);
        this.neutralizationPending.removeIf(gamepad -> !gamepad.connected());
        this.refreshNeutralizationState();
        this.devices.evictDisconnectedExcept(this.assignedSlots.keySet());
    }

    @Override
    public void reset() {
        this.assignedSlots.clear();
        this.assignedIdentities.clear();
        this.neutralizationPending.clear();
        this.devices.evictDisconnectedExcept(Set.of());
    }

    private void restoreReconnectedAssignments(List<IGamepad> connected) {
        Set<IGamepad> disconnected = new HashSet<>();
        for (IGamepad gamepad : this.assignedSlots.keySet()) {
            if (!gamepad.connected()) {
                disconnected.add(gamepad);
            }
        }
        if (disconnected.isEmpty()) {
            return;
        }

        Map<IGamepad, PhysicalGamepadIdentity> reconnecting = new HashMap<>();
        for (IGamepad gamepad : connected) {
            if (!this.assignedSlots.containsKey(gamepad)) {
                PhysicalGamepadIdentity identity = physicalIdentity(gamepad);
                if (identity != null) {
                    reconnecting.put(gamepad, identity);
                }
            }
        }

        for (Map.Entry<IGamepad, PhysicalGamepadIdentity> candidate : reconnecting.entrySet()) {
            long candidateMatches = reconnecting.values().stream()
                .filter(identity -> identity.sameController(candidate.getValue()))
                .count();
            if (candidateMatches != 1L) {
                continue;
            }
            IGamepad previous = this.matchingDisconnectedAssignment(candidate.getValue(), disconnected);
            if (previous == null) {
                continue;
            }
            Integer slotId = this.assignedSlots.remove(previous);
            PhysicalGamepadIdentity identity = this.assignedIdentities.remove(previous);
            if (slotId != null) {
                this.assignedSlots.put(candidate.getKey(), slotId);
                this.assignedIdentities.put(candidate.getKey(), identity != null ? identity : candidate.getValue());
            }
            disconnected.remove(previous);
        }
    }

    @Nullable
    private IGamepad matchingDisconnectedAssignment(
        PhysicalGamepadIdentity candidate, Set<IGamepad> disconnected
    ) {
        IGamepad match = null;
        for (IGamepad gamepad : disconnected) {
            PhysicalGamepadIdentity assigned = this.assignedIdentities.get(gamepad);
            if (assigned != null && assigned.sameController(candidate)) {
                if (match != null) {
                    return null;
                }
                match = gamepad;
            }
        }
        return match;
    }

    @Nullable
    private static PhysicalGamepadIdentity physicalIdentity(IGamepad gamepad) {
        return gamepad instanceof ReconnectableGamepad reconnectable
            ? reconnectable.physicalIdentity() : null;
    }

    private void requireNeutralBeforePrimaryFallback(IGamepad gamepad) {
        if (gamepad.connected()) {
            this.neutralizationPending.add(gamepad);
        }
    }

    private void refreshNeutralizationState() {
        this.neutralizationPending.removeIf(gamepad -> !gamepad.connected()
            || !gamepad.input().calibrationInputActive());
    }

}
