package net.jr.client.runtime.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.jr.client.runtime.context.LocalClientAcces;
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
 * created when that physical device presses Start + Select. The chord consumes
 * both buttons until they are released so neither individual action can run.
 * This is also the API the future controller-selection screen will use to
 * reassign devices without teaching UI code about slots or scopes.</p>
 */
public final class InputDeviceRouter {
    public static final int UNASSIGNED_CLIENT = -1;
    public static final int KEYBOARD_MOUSE_CLIENT = 0;

    private static final Map<Long, Integer> ASSIGNED_CLIENTS = new HashMap<>();
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
        if (!LocalClientAcces.connected(clientId)) {
            throw new IllegalStateException("Cannot assign gamepad to disconnected client " + clientId);
        }
        ASSIGNED_CLIENTS.put(deviceId, clientId);
        LocalClientAcces.input(clientId).markGamepadInput();
    }

    public static void unassignGamepad(long deviceId) {
        ASSIGNED_CLIENTS.remove(deviceId);
        JOIN_LATCHED.remove(deviceId);
    }

    public static boolean suppressForJoinChord(long deviceId, GamepadDigitalInput input) {
        return Boolean.TRUE.equals(JOIN_LATCHED.get(deviceId))
            && (input == GamepadDigitalInput.BUTTON_START
                || input == GamepadDigitalInput.BUTTON_SELECT);
    }

    public static void tickGamepadJoin(Minecraft minecraft) {
        List<Long> connected = SdlGamepad.deviceIds();
        ASSIGNED_CLIENTS.keySet().removeIf(deviceId -> !connected.contains(deviceId));
        ASSIGNED_CLIENTS.entrySet().removeIf(entry -> !LocalClientAcces.connected(entry.getValue()));
        JOIN_LATCHED.keySet().removeIf(deviceId -> !connected.contains(deviceId));

        for (long deviceId : connected) {
            boolean startDown = SdlGamepad.input(deviceId, GamepadDigitalInput.BUTTON_START, 0.2F);
            boolean selectDown = SdlGamepad.input(deviceId, GamepadDigitalInput.BUTTON_SELECT, 0.2F);

            if (!startDown && !selectDown) {
                JOIN_LATCHED.put(deviceId, false);
                continue;
            }
            if (Boolean.TRUE.equals(JOIN_LATCHED.get(deviceId))) {
                continue;
            }
            if (ASSIGNED_CLIENTS.containsKey(deviceId) || !startDown || !selectDown) {
                continue;
            }

            // Consume the complete chord before joining. The latch remains active
            // until both buttons are up, including after the device changes slots.
            JOIN_LATCHED.put(deviceId, true);

            if (LocalClientAcces.connectedCount() >= LocalClientAcces.MAX_CLIENTS) {
                show(minecraft, "Ya hay 4 jugadores locales");
                continue;
            }

            try {
                int newClientId = LocalClientAcces.joinNext(minecraft);
                assignGamepad(deviceId, newClientId);
                show(minecraft, "Mando asignado al jugador " + (newClientId + 1));
            } catch (RuntimeException exception) {
                show(minecraft, "No se pudo unir jugador local: " + exception.getMessage());
            }
        }
    }

    public static void resetGamepadAssignment() {
        ASSIGNED_CLIENTS.clear();
        JOIN_LATCHED.clear();
    }

    private static void validateClientId(int clientId) {
        if (clientId < 0 || clientId >= LocalClientAcces.MAX_CLIENTS) {
            throw new IllegalArgumentException("clientId must be between 0 and " + (LocalClientAcces.MAX_CLIENTS - 1));
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
