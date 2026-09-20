package net.jr.client.runtime.input;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.jr.client.input.gamepad.GamepadIdentity;
import net.jr.client.input.sdl.SdlGamepad;
import net.jr.client.input.simulation.InputSimulation;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;
import net.minecraft.client.Minecraft;

public final class InputDeviceRouter {
    public static final int UNASSIGNED_SLOT = -1;
    public static final int KEYBOARD_MOUSE_SLOT = 0;

    private static final Map<Long, Integer> ASSIGNED_SLOTS = new HashMap<>();
    private static final Map<Long, ReconnectionIdentity> ASSIGNED_DEVICE_IDENTITIES = new HashMap<>();
    private static final Set<Long> NEUTRALIZATION_PENDING = new HashSet<>();

    private InputDeviceRouter() {
    }

    public static int keyboardMouseSlotId() {
        return KEYBOARD_MOUSE_SLOT;
    }

    public static boolean isKeyboardMouseSlot(int slotId) {
        return slotId == KEYBOARD_MOUSE_SLOT;
    }

    public static boolean canPhysicalMouseDriveSlot(int slotId) {
        return isKeyboardMouseSlot(slotId);
    }

    public static boolean canPhysicalKeyboardDriveSlot(int slotId) {
        return isKeyboardMouseSlot(slotId);
    }

    public static List<Long> connectedGamepads() {
        return SdlGamepad.deviceIds();
    }

    public static List<GamepadDevice> devices() {
        refreshNeutralizationState();
        return SdlGamepad.deviceIds().stream()
            .map(deviceId -> new GamepadDevice(
                deviceId,
                SdlGamepad.identity(deviceId),
                assignedSlot(deviceId),
                effectiveSlot(deviceId)
            ))
            .toList();
    }

    public static List<Long> gamepadsForSlot(int slotId) {
        LocalClientSlotRegistry.requireSlotId(slotId);
        refreshNeutralizationState();
        List<Long> result = new ArrayList<>();
        boolean primaryMenuInput = slotId == KEYBOARD_MOUSE_SLOT && Minecraft.getInstance().level == null;
        for (long deviceId : SdlGamepad.deviceIds()) {
            Integer assigned = ASSIGNED_SLOTS.get(deviceId);
            if ((assigned == null
                && slotId == KEYBOARD_MOUSE_SLOT
                && !NEUTRALIZATION_PENDING.contains(deviceId))
                || (assigned != null && (assigned == slotId || primaryMenuInput))) {
                result.add(deviceId);
            }
        }
        return List.copyOf(result);
    }

    public static boolean hasGamepadForSlot(int slotId) {
        return (slotId == KEYBOARD_MOUSE_SLOT && InputSimulation.isConnected())
            || !gamepadsForSlot(slotId).isEmpty();
    }

    /**
     * Returns whether a controller still belongs to the slot but is currently
     * absent from SDL's connected-device list. The assignment is deliberately
     * retained so the controller can resume driving its original player after
     * a temporary reconnect.
     */
    public static boolean hasDisconnectedGamepadForSlot(int slotId) {
        LocalClientSlotRegistry.requireSlotId(slotId);
        return ASSIGNED_SLOTS.entrySet().stream().anyMatch(entry ->
            entry.getValue() == slotId && !SdlGamepad.isConnected(entry.getKey())
        );
    }

    @Nullable
    public static Integer assignedSlot(long deviceId) {
        return ASSIGNED_SLOTS.get(deviceId);
    }

    public static int effectiveSlot(long deviceId) {
        refreshNeutralizationState();
        if (NEUTRALIZATION_PENDING.contains(deviceId)) {
            return UNASSIGNED_SLOT;
        }
        return ASSIGNED_SLOTS.getOrDefault(deviceId, KEYBOARD_MOUSE_SLOT);
    }

    public static boolean isAwaitingNeutral(long deviceId) {
        refreshNeutralizationState();
        return NEUTRALIZATION_PENDING.contains(deviceId);
    }

    public static void assignGamepad(long deviceId, int slotId) {
        LocalClientSlotRegistry.requireSlotId(slotId);
        if (!SdlGamepad.isConnected(deviceId)) {
            throw new IllegalArgumentException("Cannot assign disconnected gamepad " + deviceId);
        }
        restoreReconnectedAssignments(SdlGamepad.deviceIds());
        NEUTRALIZATION_PENDING.remove(deviceId);
        ASSIGNED_SLOTS.put(deviceId, slotId);
        ASSIGNED_DEVICE_IDENTITIES.put(deviceId, reconnectionIdentity(deviceId));
        ClientRuntime.INSTANCE.slots().slot(slotId).inputState().markGamepadInput();
    }

    public static void unassignGamepad(long deviceId) {
        Integer removedSlot = ASSIGNED_SLOTS.remove(deviceId);
        ASSIGNED_DEVICE_IDENTITIES.remove(deviceId);
        if (removedSlot != null) {
            requireNeutralBeforePrimaryFallback(deviceId);
        }
    }

    public static void unassignSlot(int slotId) {
        LocalClientSlotRegistry.requireSlotId(slotId);
        List<Long> releasedDevices = ASSIGNED_SLOTS.entrySet().stream()
            .filter(entry -> entry.getValue() == slotId)
            .map(Map.Entry::getKey)
            .toList();
        ASSIGNED_SLOTS.entrySet().removeIf(entry -> entry.getValue() == slotId);
        releasedDevices.forEach(ASSIGNED_DEVICE_IDENTITIES::remove);
        releasedDevices.forEach(InputDeviceRouter::requireNeutralBeforePrimaryFallback);
    }

    public static void pruneDisconnectedDevices() {
        List<Long> connected = SdlGamepad.deviceIds();
        restoreReconnectedAssignments(connected);
        NEUTRALIZATION_PENDING.removeIf(deviceId -> !connected.contains(deviceId));
        refreshNeutralizationState();
    }

    /**
     * Clears only assignments whose controller is still disconnected. This is
     * a world-session boundary operation: transient disconnects during play
     * must keep their device-to-slot association for automatic reconnection.
     */
    public static void clearDisconnectedAssignments() {
        List<Long> connected = SdlGamepad.deviceIds();
        restoreReconnectedAssignments(connected);
        List<Long> releasedDevices = ASSIGNED_SLOTS.keySet().stream()
            .filter(deviceId -> !connected.contains(deviceId))
            .toList();
        releasedDevices.forEach(ASSIGNED_SLOTS::remove);
        releasedDevices.forEach(ASSIGNED_DEVICE_IDENTITIES::remove);
        NEUTRALIZATION_PENDING.removeIf(deviceId -> !connected.contains(deviceId));
        refreshNeutralizationState();
    }

    public static void resetGamepadAssignment() {
        ASSIGNED_SLOTS.clear();
        ASSIGNED_DEVICE_IDENTITIES.clear();
        NEUTRALIZATION_PENDING.clear();
    }

    /**
     * SDL assigns a new instance ID after a controller is unplugged. Move a
     * retained assignment to the new ID when its physical identity is unique.
     */
    private static void restoreReconnectedAssignments(List<Long> connected) {
        Set<Long> disconnectedAssignments = new HashSet<>();
        for (long deviceId : ASSIGNED_SLOTS.keySet()) {
            if (!connected.contains(deviceId)) {
                disconnectedAssignments.add(deviceId);
            }
        }
        if (disconnectedAssignments.isEmpty()) {
            return;
        }

        Map<Long, ReconnectionIdentity> reconnectingDevices = new HashMap<>();
        for (long deviceId : connected) {
            if (ASSIGNED_SLOTS.containsKey(deviceId)) {
                continue;
            }
            ReconnectionIdentity identity = reconnectionIdentity(deviceId);
            if (identity != null) {
                reconnectingDevices.put(deviceId, identity);
            }
        }

        var reconnecting = reconnectingDevices.entrySet().iterator();
        while (reconnecting.hasNext()) {
            Map.Entry<Long, ReconnectionIdentity> candidate = reconnecting.next();
            Long disconnectedDeviceId = matchingDisconnectedAssignment(
                candidate.getValue(),
                disconnectedAssignments
            );
            if (disconnectedDeviceId == null) {
                continue;
            }

            Integer slotId = ASSIGNED_SLOTS.remove(disconnectedDeviceId);
            ReconnectionIdentity identity = ASSIGNED_DEVICE_IDENTITIES.remove(disconnectedDeviceId);
            if (slotId != null) {
                ASSIGNED_SLOTS.put(candidate.getKey(), slotId);
                ASSIGNED_DEVICE_IDENTITIES.put(
                    candidate.getKey(),
                    identity != null ? identity : candidate.getValue()
                );
            }
            disconnectedAssignments.remove(disconnectedDeviceId);
            reconnecting.remove();
        }
    }

    @Nullable
    private static Long matchingDisconnectedAssignment(
        ReconnectionIdentity candidate,
        Set<Long> disconnectedAssignments
    ) {
        List<Long> physicalMatches = new ArrayList<>();
        for (long deviceId : disconnectedAssignments) {
            ReconnectionIdentity assigned = ASSIGNED_DEVICE_IDENTITIES.get(deviceId);
            if (assigned != null && assigned.samePhysicalController(candidate)) {
                physicalMatches.add(deviceId);
            }
        }
        if (physicalMatches.size() == 1) {
            return physicalMatches.getFirst();
        }
        if (!physicalMatches.isEmpty()) {
            return null;
        }
        return null;
    }

    @Nullable
    private static ReconnectionIdentity reconnectionIdentity(long deviceId) {
        GamepadIdentity identity = SdlGamepad.identity(deviceId);
        return identity == null
            ? null
            : new ReconnectionIdentity(identity.key(), SdlGamepad.serial(deviceId), SdlGamepad.path(deviceId));
    }

    private static void requireNeutralBeforePrimaryFallback(long deviceId) {
        if (SdlGamepad.isConnected(deviceId)) {
            NEUTRALIZATION_PENDING.add(deviceId);
        }
    }

    private static void refreshNeutralizationState() {
        NEUTRALIZATION_PENDING.removeIf(deviceId -> !SdlGamepad.isConnected(deviceId)
            || !SdlGamepad.calibrationInputActive(deviceId));
    }

    private record ReconnectionIdentity(String controllerKey, String serial, String path) {
        private boolean samePhysicalController(ReconnectionIdentity other) {
            return this.sameControllerModel(other)
                && ((!this.serial.isBlank() && !other.serial.isBlank() && this.serial.equals(other.serial))
                    || (!this.path.isBlank() && !other.path.isBlank() && this.path.equals(other.path)));
        }

        private boolean sameControllerModel(ReconnectionIdentity other) {
            return this.controllerKey.equals(other.controllerKey);
        }
    }

    public record GamepadDevice(
        long deviceId,
        @Nullable GamepadIdentity identity,
        @Nullable Integer assignedSlot,
        int effectiveSlot
    ) {
    }
}
