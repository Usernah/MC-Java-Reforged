package net.jr.client.runtime.input;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.jr.api.client.input.IGamepad;
import net.jr.api.client.input.GamepadDigitalInput;
import net.jr.client.runtime.ClientRuntime;
import net.jr.experimental_features.pre_join.PreJoinWorldLoad;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.jr.client.systemui.user.SignInMenu;

public final class GamepadJoinTicker {
    private static final Map<IGamepad, Boolean> JOIN_LATCHED = new HashMap<>();

    private GamepadJoinTicker() {
    }

    public static void tick(Minecraft minecraft) {
        ClientRuntime.INSTANCE.input().mutableAssignments().pruneDisconnectedDevices();
        JoinPromptAnnouncer.tick(minecraft);
        if (PreJoinWorldLoad.isActive()) {
            return;
        }
        List<IGamepad> connected = ClientRuntime.INSTANCE.input().devices().gamepads();
        JOIN_LATCHED.keySet().removeIf(gamepad -> !connected.contains(gamepad));

        for (IGamepad gamepad : connected) {
            boolean startDown = gamepad.input().pressed(GamepadDigitalInput.BUTTON_START);
            boolean selectDown = gamepad.input().pressed(GamepadDigitalInput.BUTTON_SELECT);

            if (!startDown && !selectDown) {
                JOIN_LATCHED.put(gamepad, false);
                continue;
            }
            if (Boolean.TRUE.equals(JOIN_LATCHED.get(gamepad))) {
                continue;
            }
            if (ClientRuntime.INSTANCE.input().assignments().awaitingNeutral(gamepad)) {
                continue;
            }
            if (ClientRuntime.INSTANCE.input().assignments().assignedSlot(gamepad) != null || !startDown || !selectDown) {
                continue;
            }

            JOIN_LATCHED.put(gamepad, true);

            try {
                SignInMenu.open(gamepad, user -> {
                    try {
                        int slotId = UserJoinService.select(minecraft, gamepad, user);
                        JoinPromptAnnouncer.dismiss();
                        show(minecraft, "Mando asignado a " + user.displayName()
                            + " (jugador " + (slotId + 1) + ")");
                    } catch (RuntimeException exception) {
                        show(minecraft, "No se pudo seleccionar usuario: " + exception.getMessage());
                    }
                });
            } catch (RuntimeException exception) {
                if (ClientRuntime.INSTANCE.input().join().canStartJoin(gamepad)) {
                    try {
                        int slotId = ClientRuntime.INSTANCE.input().join().join(minecraft, gamepad);
                        JoinPromptAnnouncer.dismiss();
                        show(minecraft, "Mando asignado al jugador " + (slotId + 1));
                    } catch (RuntimeException fallbackException) {
                        show(minecraft, "No se pudo unir jugador local: " + fallbackException.getMessage());
                    }
                } else {
                    show(minecraft, "No se pudo abrir el selector de usuarios: " + exception.getMessage());
                }
            }
        }
    }

    public static boolean suppressForJoinChord(IGamepad gamepad, GamepadDigitalInput input) {
        return Boolean.TRUE.equals(JOIN_LATCHED.get(gamepad))
            && (input == GamepadDigitalInput.BUTTON_START
                || input == GamepadDigitalInput.BUTTON_SELECT);
    }

    public static void reset() {
        JOIN_LATCHED.clear();
        JoinPromptAnnouncer.reset();
    }

    private static void show(Minecraft minecraft, String message) {
        if (minecraft.gui != null) {
            minecraft.gui.hud.setOverlayMessage(Component.literal(message), false);
        }
    }
}
