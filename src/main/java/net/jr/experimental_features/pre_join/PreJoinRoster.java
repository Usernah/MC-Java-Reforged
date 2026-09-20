package net.jr.experimental_features.pre_join;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Keeps the local-player composition while moving between worlds in one game session. */
public final class PreJoinRoster {
    private static final Map<Integer, Long> LAST_PLAYERS = new LinkedHashMap<>();

    private PreJoinRoster() {
    }

    public static void remember(Map<Integer, Long> players) {
        LAST_PLAYERS.clear();
        LAST_PLAYERS.putAll(players);
    }

    public static void forget(int slotId) {
        LAST_PLAYERS.remove(slotId);
    }

    /** Removes saved players whose controller was absent at a world exit. */
    public static void forgetDisconnectedDevices(List<Long> connectedDevices) {
        LAST_PLAYERS.entrySet().removeIf(entry -> !connectedDevices.contains(entry.getValue()));
    }

    public static void restoreInto(Long[] assignedDevices, List<Long> connectedDevices) {
        Set<Long> usedDevices = new HashSet<>();

        for (Map.Entry<Integer, Long> entry : LAST_PLAYERS.entrySet()) {
            int slotId = entry.getKey();
            long deviceId = entry.getValue();
            if (validSecondarySlot(slotId, assignedDevices)
                && connectedDevices.contains(deviceId)
                && usedDevices.add(deviceId)) {
                assignedDevices[slotId] = deviceId;
            }
        }

        for (int slotId : LAST_PLAYERS.keySet()) {
            if (!validSecondarySlot(slotId, assignedDevices) || assignedDevices[slotId] != null) {
                continue;
            }
            Long replacement = connectedDevices.stream()
                .filter(usedDevices::add)
                .findFirst()
                .orElse(null);
            if (replacement == null) {
                break;
            }
            assignedDevices[slotId] = replacement;
        }
    }

    private static boolean validSecondarySlot(int slotId, Long[] assignedDevices) {
        return slotId > 0 && slotId < assignedDevices.length;
    }
}
