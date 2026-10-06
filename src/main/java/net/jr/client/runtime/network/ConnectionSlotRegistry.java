package net.jr.client.runtime.network;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import net.jr.client.runtime.slot.ClientSlots;
import net.minecraft.network.Connection;

public final class ConnectionSlotRegistry {
    private static final ThreadLocal<Creation> ACTIVE_CREATION = new ThreadLocal<>();

    private final Map<Connection, Integer> slotIdsByConnection = new IdentityHashMap<>();

    public synchronized void bind(Connection connection, int slotId) {
        ClientSlots.requireSlotId(slotId);
        if (connection == null) {
            throw new IllegalArgumentException("connection cannot be null");
        }
        Integer currentSlotId = this.slotIdsByConnection.get(connection);
        if (currentSlotId != null && currentSlotId != slotId) {
            throw new IllegalStateException(
                "Connection is already owned by local client slot " + currentSlotId
                    + " and cannot be rebound to slot " + slotId
            );
        }
        this.slotIdsByConnection.put(connection, slotId);
    }

    public Connection connectOwned(int slotId, Supplier<Connection> connector) {
        ClientSlots.requireSlotId(slotId);
        Objects.requireNonNull(connector, "connector");
        if (ACTIVE_CREATION.get() != null) {
            throw new IllegalStateException("A local client connection is already being created on this thread");
        }

        Creation creation = new Creation(this, slotId);
        ACTIVE_CREATION.set(creation);
        try {
            Connection connection = Objects.requireNonNull(connector.get(), "connector returned null");
            int ownerSlotId = this.requireSlot(connection);
            if (ownerSlotId != slotId) {
                throw new IllegalStateException(
                    "New connection belongs to slot " + ownerSlotId + " instead of slot " + slotId
                );
            }
            return connection;
        } catch (RuntimeException | Error error) {
            if (creation.connection != null) {
                this.unbind(creation.connection);
            }
            throw error;
        } finally {
            ACTIVE_CREATION.remove();
        }
    }

    public static void bindConstructed(Connection connection) {
        Creation creation = ACTIVE_CREATION.get();
        if (creation != null) {
            creation.bind(connection);
        }
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

    private static final class Creation {
        private final ConnectionSlotRegistry registry;
        private final int slotId;
        private Connection connection;

        private Creation(ConnectionSlotRegistry registry, int slotId) {
            this.registry = registry;
            this.slotId = slotId;
        }

        private void bind(Connection connection) {
            if (this.connection != null) {
                throw new IllegalStateException("More than one connection was constructed for a local client");
            }
            this.registry.bind(connection, this.slotId);
            this.connection = connection;
        }
    }
}
