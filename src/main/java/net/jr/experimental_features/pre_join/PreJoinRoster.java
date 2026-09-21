package net.jr.experimental_features.pre_join;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.jr.api.client.input.IGamepad;

/** Keeps the local-player composition while moving between worlds in one game session. */
public final class PreJoinRoster {
    private static final Map<Integer, IGamepad> LAST_PLAYERS = new LinkedHashMap<>();

    private PreJoinRoster() {
    }

    public static void remember(Map<Integer, IGamepad> players) {
        LAST_PLAYERS.clear();
        LAST_PLAYERS.putAll(players);
    }

    public static void forget(int slotId) {
        LAST_PLAYERS.remove(slotId);
    }

    /** Removes saved players whose controller was absent at a world exit. */
    public static void forgetDisconnectedDevices(List<IGamepad> connectedDevices) {
        LAST_PLAYERS.entrySet().removeIf(entry -> !connectedDevices.contains(entry.getValue()));
    }

    public static void restoreInto(IGamepad[] assignedDevices, List<IGamepad> connectedDevices) {
        Set<IGamepad> usedDevices = new HashSet<>();

        for (Map.Entry<Integer, IGamepad> entry : LAST_PLAYERS.entrySet()) {
            int slotId = entry.getKey();
            IGamepad gamepad = entry.getValue();
            if (validSecondarySlot(slotId, assignedDevices)
                && connectedDevices.contains(gamepad)
                && usedDevices.add(gamepad)) {
                assignedDevices[slotId] = gamepad;
            }
        }

        for (int slotId : LAST_PLAYERS.keySet()) {
            if (!validSecondarySlot(slotId, assignedDevices) || assignedDevices[slotId] != null) {
                continue;
            }
            IGamepad replacement = connectedDevices.stream()
                .filter(usedDevices::add)
                .findFirst()
                .orElse(null);
            if (replacement == null) {
                break;
            }
            assignedDevices[slotId] = replacement;
        }
    }

    private static boolean validSecondarySlot(int slotId, IGamepad[] assignedDevices) {
        return slotId > 0 && slotId < assignedDevices.length;
    }
}
