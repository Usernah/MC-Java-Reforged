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

public final class InputDeviceRouter {
    public static final int UNASSIGNED_SLOT = -1;
    public static final int KEYBOARD_MOUSE_SLOT = 0;

    private static final Map<Long, Integer> ASSIGNED_SLOTS = new HashMap<>();
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
        for (long deviceId : SdlGamepad.deviceIds()) {
            Integer assigned = ASSIGNED_SLOTS.get(deviceId);
            if ((assigned == null
                && slotId == KEYBOARD_MOUSE_SLOT
                && !NEUTRALIZATION_PENDING.contains(deviceId))
                || (assigned != null && assigned == slotId)) {
                result.add(deviceId);
            }
        }
        return List.copyOf(result);
    }

    public static boolean hasGamepadForSlot(int slotId) {
        return (slotId == KEYBOARD_MOUSE_SLOT && InputSimulation.isConnected())
            || !gamepadsForSlot(slotId).isEmpty();
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
        NEUTRALIZATION_PENDING.remove(deviceId);
        ASSIGNED_SLOTS.put(deviceId, slotId);
        ClientRuntime.INSTANCE.slots().slot(slotId).inputState().markGamepadInput();
    }

    public static void unassignGamepad(long deviceId) {
        if (ASSIGNED_SLOTS.remove(deviceId) != null) {
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
        releasedDevices.forEach(InputDeviceRouter::requireNeutralBeforePrimaryFallback);
    }

    public static void pruneDisconnectedDevices() {
        List<Long> connected = SdlGamepad.deviceIds();
        ASSIGNED_SLOTS.keySet().removeIf(deviceId -> !connected.contains(deviceId));
        NEUTRALIZATION_PENDING.removeIf(deviceId -> !connected.contains(deviceId));
        refreshNeutralizationState();
    }

    public static void resetGamepadAssignment() {
        ASSIGNED_SLOTS.clear();
        NEUTRALIZATION_PENDING.clear();
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

    public record GamepadDevice(
        long deviceId,
        @Nullable GamepadIdentity identity,
        @Nullable Integer assignedSlot,
        int effectiveSlot
    ) {
    }
}
