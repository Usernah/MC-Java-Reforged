package net.jr.client.runtime.input;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.jr.client.input.InputApi;
import net.jr.client.input.gamepad.GamepadDigitalInput;
import net.jr.client.input.sdl.SdlGamepad;
import net.jr.client.runtime.client.LocalClientJoinHandler;
import net.jr.client.runtime.client.LocalClientJoinPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class GamepadJoinTicker {
    private static final Map<Long, Boolean> JOIN_LATCHED = new HashMap<>();

    private GamepadJoinTicker() {
    }

    public static void tick(Minecraft minecraft) {
        List<Long> connected = SdlGamepad.deviceIds();
        InputDeviceRouter.pruneDisconnectedDevices();
        JOIN_LATCHED.keySet().removeIf(deviceId -> !connected.contains(deviceId));

        for (long deviceId : connected) {
            boolean startDown = SdlGamepad.input(
                deviceId,
                GamepadDigitalInput.BUTTON_START,
                InputApi.DIGITAL_PRESS_THRESHOLD
            );
            boolean selectDown = SdlGamepad.input(
                deviceId,
                GamepadDigitalInput.BUTTON_SELECT,
                InputApi.DIGITAL_PRESS_THRESHOLD
            );

            if (!startDown && !selectDown) {
                JOIN_LATCHED.put(deviceId, false);
                continue;
            }
            if (Boolean.TRUE.equals(JOIN_LATCHED.get(deviceId))) {
                continue;
            }
            if (InputDeviceRouter.assignedSlot(deviceId) != null || !startDown || !selectDown) {
                continue;
            }

            JOIN_LATCHED.put(deviceId, true);

            if (!LocalClientJoinPolicy.canStartJoin(deviceId)) {
                show(minecraft, "Ya hay 4 jugadores locales");
                continue;
            }

            try {
                int slotId = LocalClientJoinHandler.join(minecraft, deviceId);
                show(minecraft, "Mando asignado al jugador " + (slotId + 1));
            } catch (RuntimeException exception) {
                show(minecraft, "No se pudo unir jugador local: " + exception.getMessage());
            }
        }
    }

    public static boolean suppressForJoinChord(long deviceId, GamepadDigitalInput input) {
        return Boolean.TRUE.equals(JOIN_LATCHED.get(deviceId))
            && (input == GamepadDigitalInput.BUTTON_START
                || input == GamepadDigitalInput.BUTTON_SELECT);
    }

    public static void reset() {
        JOIN_LATCHED.clear();
    }

    private static void show(Minecraft minecraft, String message) {
        if (minecraft.gui != null) {
            minecraft.gui.hud.setOverlayMessage(Component.literal(message), false);
        }
    }
}
