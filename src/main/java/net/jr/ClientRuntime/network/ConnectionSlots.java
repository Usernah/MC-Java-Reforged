package net.jr.ClientRuntime.network;

import java.util.IdentityHashMap;
import java.util.Map;
import net.jr.ClientRuntime.slot.PlayerSlots;
import net.minecraft.network.Connection;

public final class ConnectionSlots {
    private final Map<Connection, Integer> slotIdsByConnection = new IdentityHashMap<>();

    public void bind(Connection connection, int slotId) {
        requireSlot(slotId);
        if (connection == null) {
            throw new IllegalArgumentException("connection cannot be null");
        }
        this.slotIdsByConnection.put(connection, slotId);
    }

    public void unbind(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("connection cannot be null");
        }
        this.slotIdsByConnection.remove(connection);
    }

    public int requireSlot(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("connection cannot be null");
        }
        Integer slotId = this.slotIdsByConnection.get(connection);
        if (slotId == null) {
            throw new IllegalStateException("No player slot is bound to connection " + connection);
        }
        return slotId;
    }

    public Integer slotOrNull(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("connection cannot be null");
        }
        return this.slotIdsByConnection.get(connection);
    }

    public boolean has(Connection connection) {
        return this.slotIdsByConnection.containsKey(connection);
    }

    public boolean isEmpty() {
        return this.slotIdsByConnection.isEmpty();
    }

    private static void requireSlot(int slotId) {
        if (slotId < 0 || slotId >= PlayerSlots.MAX_SLOTS) {
            throw new IndexOutOfBoundsException("Invalid player slot id " + slotId);
        }
    }
}
