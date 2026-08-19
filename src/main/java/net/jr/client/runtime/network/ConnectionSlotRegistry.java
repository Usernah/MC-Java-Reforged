package net.jr.client.runtime.network;

import java.util.IdentityHashMap;
import java.util.Map;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;
import net.minecraft.network.Connection;

public final class ConnectionSlotRegistry {
    private final Map<Connection, Integer> slotIdsByConnection = new IdentityHashMap<>();

    public synchronized void bind(Connection connection, int slotId) {
        LocalClientSlotRegistry.requireSlotId(slotId);
        if (connection == null) {
            throw new IllegalArgumentException("connection cannot be null");
        }
        this.slotIdsByConnection.put(connection, slotId);
    }

    public synchronized void unbind(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("connection cannot be null");
        }
        this.slotIdsByConnection.remove(connection);
    }

    public synchronized int requireSlot(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("connection cannot be null");
        }
        Integer slotId = this.slotIdsByConnection.get(connection);
        if (slotId == null) {
            throw new IllegalStateException("No local client slot is bound to connection " + connection);
        }
        return slotId;
    }

    public synchronized Integer slotOrNull(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("connection cannot be null");
        }
        return this.slotIdsByConnection.get(connection);
    }

    public synchronized boolean has(Connection connection) {
        return this.slotIdsByConnection.containsKey(connection);
    }

    public synchronized boolean isEmpty() {
        return this.slotIdsByConnection.isEmpty();
    }
}
