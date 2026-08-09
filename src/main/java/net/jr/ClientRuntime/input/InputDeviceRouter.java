package net.jr.ClientRuntime.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.jr.ClientRuntime.runtime.Client;
import net.jr.client.input.gamepad.GamepadDigitalInput;
import net.jr.client.input.gamepad.GamepadIdentity;
import net.jr.client.input.sdl.SdlGamepad;
import net.jr.client.input.simulation.InputSimulation;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Central device-to-client registry.
 *
 * <p>An unassigned gamepad is deliberately routed to client 0. Assignment is
 * only created after that physical device holds Start + Select for three
 * seconds. This is also the API the future controller-selection screen will
 * use to reassign devices without teaching UI code about slots or scopes.</p>
 */
public final class InputDeviceRouter {
    public static final int UNASSIGNED_CLIENT = -1;
    public static final int KEYBOARD_MOUSE_CLIENT = 0;
    public static final long JOIN_HOLD_MILLIS = 3_000L;

    private static final Map<Long, Integer> ASSIGNED_CLIENTS = new HashMap<>();
    private static final Map<Long, Long> JOIN_STARTED_AT = new HashMap<>();
    private static final Map<Long, Boolean> JOIN_LATCHED = new HashMap<>();

    private InputDeviceRouter() {
    }

    public static int keyboardMouseClientId() {
        return KEYBOARD_MOUSE_CLIENT;
    }

    public static boolean isKeyboardMouseClient(int clientId) {
        return clientId == KEYBOARD_MOUSE_CLIENT;
    }

    public static boolean canPhysicalMouseDriveClient(int clientId) {
        return isKeyboardMouseClient(clientId);
    }

    public static boolean canPhysicalKeyboardDriveClient(int clientId) {
        return isKeyboardMouseClient(clientId);
    }

    public static List<Long> connectedGamepads() {
        return SdlGamepad.deviceIds();
    }

    public static List<GamepadDevice> devices() {
        return SdlGamepad.deviceIds().stream()
            .map(deviceId -> new GamepadDevice(deviceId, SdlGamepad.identity(deviceId), assignedClient(deviceId), effectiveClient(deviceId)))
            .toList();
    }

    public static List<Long> gamepadsForClient(int clientId) {
        validateClientId(clientId);
        List<Long> result = new ArrayList<>();
        for (long deviceId : SdlGamepad.deviceIds()) {
            Integer assigned = ASSIGNED_CLIENTS.get(deviceId);
            if ((assigned == null && clientId == KEYBOARD_MOUSE_CLIENT)
                || (assigned != null && assigned == clientId)) {
                result.add(deviceId);
            }
        }
        return List.copyOf(result);
    }

    public static boolean hasGamepadForClient(int clientId) {
        return (clientId == KEYBOARD_MOUSE_CLIENT && InputSimulation.isConnected()) || !gamepadsForClient(clientId).isEmpty();
    }

    @Nullable
    public static Integer assignedClient(long deviceId) {
        return ASSIGNED_CLIENTS.get(deviceId);
    }

    public static int effectiveClient(long deviceId) {
        return ASSIGNED_CLIENTS.getOrDefault(deviceId, KEYBOARD_MOUSE_CLIENT);
    }

    public static void assignGamepad(long deviceId, int clientId) {
        validateClientId(clientId);
        if (!SdlGamepad.isConnected(deviceId)) {
            throw new IllegalArgumentException("Cannot assign disconnected gamepad " + deviceId);
        }
        if (!Client.connected(clientId)) {
            throw new IllegalStateException("Cannot assign gamepad to disconnected client " + clientId);
        }
        ASSIGNED_CLIENTS.put(deviceId, clientId);
        Client.input(clientId).markGamepadInput();
        JOIN_STARTED_AT.remove(deviceId);
        JOIN_LATCHED.put(deviceId, true);
    }

    public static void unassignGamepad(long deviceId) {
        ASSIGNED_CLIENTS.remove(deviceId);
        JOIN_STARTED_AT.remove(deviceId);
        JOIN_LATCHED.remove(deviceId);
    }

    public static boolean suppressForJoinChord(long deviceId, GamepadDigitalInput input) {
        return (JOIN_STARTED_AT.containsKey(deviceId) || Boolean.TRUE.equals(JOIN_LATCHED.get(deviceId)))
            && (input == GamepadDigitalInput.BUTTON_START
                || input == GamepadDigitalInput.BUTTON_SELECT);
    }

    public static void tickGamepadJoin(Minecraft minecraft) {
        List<Long> connected = SdlGamepad.deviceIds();
        ASSIGNED_CLIENTS.keySet().removeIf(deviceId -> !connected.contains(deviceId));
        ASSIGNED_CLIENTS.entrySet().removeIf(entry -> !Client.connected(entry.getValue()));
        JOIN_STARTED_AT.keySet().removeIf(deviceId -> !connected.contains(deviceId));
        JOIN_LATCHED.keySet().removeIf(deviceId -> !connected.contains(deviceId));

        long now = System.currentTimeMillis();
        for (long deviceId : connected) {
            boolean chordDown = SdlGamepad.input(deviceId, GamepadDigitalInput.BUTTON_START, 0.2F)
                && SdlGamepad.input(deviceId, GamepadDigitalInput.BUTTON_SELECT, 0.2F);

            if (!chordDown) {
                JOIN_STARTED_AT.remove(deviceId);
                JOIN_LATCHED.put(deviceId, false);
                continue;
            }
            if (Boolean.TRUE.equals(JOIN_LATCHED.get(deviceId)) || ASSIGNED_CLIENTS.containsKey(deviceId)) {
                continue;
            }

            Long startedAt = JOIN_STARTED_AT.putIfAbsent(deviceId, now);
            if (startedAt == null) {
                show(minecraft, "Mantén Start + Select 3 segundos para unir otro jugador");
                continue;
            }
            if (now - startedAt < JOIN_HOLD_MILLIS) {
                continue;
            }

            if (Client.connectedCount() >= Client.MAX_CLIENTS) {
                JOIN_LATCHED.put(deviceId, true);
                show(minecraft, "Ya hay 4 jugadores locales");
                continue;
            }

            try {
                int newClientId = Client.joinNext(minecraft);
                assignGamepad(deviceId, newClientId);
                show(minecraft, "Mando asignado al jugador " + (newClientId + 1));
            } catch (RuntimeException exception) {
                JOIN_LATCHED.put(deviceId, true);
                show(minecraft, "No se pudo unir jugador local: " + exception.getMessage());
            }
        }
    }

    public static void resetGamepadAssignment() {
        ASSIGNED_CLIENTS.clear();
        JOIN_STARTED_AT.clear();
        JOIN_LATCHED.clear();
    }

    private static void validateClientId(int clientId) {
        if (clientId < 0 || clientId >= Client.MAX_CLIENTS) {
            throw new IllegalArgumentException("clientId must be between 0 and " + (Client.MAX_CLIENTS - 1));
        }
    }

    private static void show(Minecraft minecraft, String message) {
        if (minecraft.gui != null) {
            minecraft.gui.hud.setOverlayMessage(Component.literal(message), false);
        }
    }

    /** Read model for the future J1-only assignment menu. */
    public record GamepadDevice(long deviceId, @Nullable GamepadIdentity identity, @Nullable Integer assignedClient, int effectiveClient) {
    }
}
