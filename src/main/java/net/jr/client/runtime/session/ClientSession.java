package net.jr.client.runtime.session;

import com.mojang.authlib.GameProfile;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.network.ConnectionSlotRegistry;
import net.jr.client.runtime.network.SecondaryClientConnector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;

public final class ClientSession {
    private static final AtomicLong NEXT_SESSION_ID = new AtomicLong();
    private static final long RECONNECT_DELAY_MS = 1000L;

    private final int slotId;
    private final long sessionId = NEXT_SESSION_ID.incrementAndGet();
    @Nullable
    private final GameProfile profile;
    @Nullable
    private volatile Connection connection;
    private volatile boolean connectionAttemptInFlight;
    private volatile long connectionAttemptGeneration;
    private volatile long lastConnectAttemptMs;
    private volatile boolean joining;
    private volatile boolean joinedWorldOnce;
    private volatile boolean positionSynchronized;
    private volatile int worldReadyValidationTicks;
    private volatile long generation;

    public ClientSession(int slotId, @Nullable GameProfile profile) {
        this.slotId = slotId;
        this.profile = profile;
        this.joining = this.isSecondary();
        this.joinedWorldOnce = !this.isSecondary();
        this.positionSynchronized = !this.isSecondary();
        this.worldReadyValidationTicks = 0;
    }

    public void ensureConnected(Minecraft minecraft, ConnectionSlotRegistry connections) {
        if (!this.isSecondary()) {
            Connection primaryConnection = requirePrimaryConnection(minecraft);
            this.replaceConnection(primaryConnection);
            connections.bind(primaryConnection, this.slotId);
            return;
        }
        if (this.profile == null) {
            throw new IllegalStateException("Secondary session " + this.slotId + " has no profile");
        }
        if (this.connection != null) {
            if (this.connection.isConnected()) {
                return;
            }
            this.disconnect(connections);
        }
        if (this.connectionAttemptInFlight) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - this.lastConnectAttemptMs < RECONNECT_DELAY_MS) {
            return;
        }
        this.lastConnectAttemptMs = now;

        if (minecraft.getSingleplayerServer() != null) {
            this.beginSecondaryConnectionAttempt();
            Connection openedConnection = SecondaryClientConnector.connectToIntegratedServer(
                minecraft,
                connections,
                this.slotId,
                this.profile
            );
            this.installSecondaryConnection(openedConnection);
            return;
        }

        if (minecraft.getCurrentServer() == null) {
            this.setJoiningInProgress(true);
            return;
        }

        this.connectToRemoteServerAsync(minecraft, connections, minecraft.getCurrentServer());
    }

    public void disconnect(ConnectionSlotRegistry connections) {
        Connection closingConnection;
        synchronized (this) {
            this.connectionAttemptGeneration++;
            closingConnection = this.connection;
            this.connection = null;
            if (closingConnection != null) {
                this.generation++;
            }
            this.connectionAttemptInFlight = false;
            this.joining = this.isSecondary();
            this.joinedWorldOnce = !this.isSecondary();
            this.positionSynchronized = !this.isSecondary();
            this.worldReadyValidationTicks = 0;
        }
        if (closingConnection != null) {
            SlotScope.run(this.slotId, () ->
                closingConnection.disconnect(Component.literal("Closing local client session"))
            );
            connections.unbind(closingConnection);
        }
    }

    private static Connection requirePrimaryConnection(Minecraft minecraft) {
        if (minecraft.getConnection() == null) {
            throw new IllegalStateException("Primary local client has no ClientPacketListener");
        }
        Connection connection = minecraft.getConnection().getConnection();
        if (connection == null) {
            throw new IllegalStateException("Primary local client has no Connection");
        }
        return connection;
    }

    public boolean isSecondary() {
        return this.profile != null;
    }

    public int slotId() {
        return this.slotId;
    }

    public long sessionId() {
        return this.sessionId;
    }

    public long generation() {
        return this.generation;
    }

    @Nullable
    public Connection connection() {
        return this.connection;
    }

    public boolean isJoiningInProgress() {
        return this.joining;
    }

    public synchronized void setJoiningInProgress(boolean joining) {
        if (!this.isSecondary()) {
            this.joining = false;
            return;
        }
        this.joining = joining;
        if (joining) {
            this.joinedWorldOnce = false;
        }
    }

    public synchronized void markWorldReady() {
        if (this.isSecondary()) {
            this.joinedWorldOnce = true;
            this.joining = false;
        }
    }

    public synchronized boolean validateWorldReadyCandidate(boolean readyCandidate) {
        if (!this.isSecondary()) {
            return readyCandidate;
        }
        if (!readyCandidate) {
            this.worldReadyValidationTicks = 0;
            this.joinedWorldOnce = false;
            this.joining = true;
            return false;
        }
        if (this.joinedWorldOnce && !this.joining) {
            return true;
        }
        return ++this.worldReadyValidationTicks >= 2;
    }

    public boolean hasSynchronizedPosition() {
        return this.positionSynchronized;
    }

    public synchronized void markPositionSynchronized(Connection source) {
        if (this.connection == source) {
            this.positionSynchronized = true;
        }
    }

    public synchronized void markWorldCleared() {
        this.generation++;
        this.joinedWorldOnce = false;
        this.positionSynchronized = !this.isSecondary();
        this.worldReadyValidationTicks = 0;
        if (this.isSecondary()) {
            this.joining = true;
        }
    }

    public synchronized boolean owns(Connection connection) {
        return this.connection == connection;
    }

    public synchronized boolean isWorldReadyFor(Connection connection) {
        return !this.isSecondary()
            || this.connection == connection
                && connection != null
                && connection.isConnected()
                && !this.joining
                && this.joinedWorldOnce
                && this.positionSynchronized;
    }

    private synchronized void replaceConnection(Connection connection) {
        if (this.connection != connection) {
            this.generation++;
            this.joining = false;
            this.joinedWorldOnce = true;
            this.positionSynchronized = true;
            this.worldReadyValidationTicks = 0;
            this.connection = connection;
        }
    }

    private synchronized void beginSecondaryConnectionAttempt() {
        this.generation++;
        this.joining = true;
        this.joinedWorldOnce = false;
        this.positionSynchronized = false;
        this.worldReadyValidationTicks = 0;
    }

    private synchronized void installSecondaryConnection(Connection connection) {
        this.connection = connection;
    }

    private void connectToRemoteServerAsync(
        Minecraft minecraft,
        ConnectionSlotRegistry connections,
        ServerData serverData
    ) {
        long attemptGeneration;
        synchronized (this) {
            this.connectionAttemptInFlight = true;
            this.beginSecondaryConnectionAttempt();
            attemptGeneration = ++this.connectionAttemptGeneration;
        }
        Thread connectionThread = new Thread(() -> {
            Connection openedConnection = null;
            try {
                openedConnection = SecondaryClientConnector.connectToRemoteServer(
                    minecraft,
                    connections,
                    this.slotId,
                    this.profile,
                    serverData
                );
                if (!this.installConnection(attemptGeneration, openedConnection)) {
                    connections.unbind(openedConnection);
                    openedConnection.disconnect(Component.literal("Local client session was closed while connecting"));
                    return;
                }
            } catch (Exception exception) {
                if (openedConnection != null) {
                    connections.unbind(openedConnection);
                    openedConnection.disconnect(Component.literal("Failed to open remote local client session"));
                }
                synchronized (this) {
                    if (attemptGeneration == this.connectionAttemptGeneration) {
                        this.connection = null;
                        this.joining = true;
                        this.joinedWorldOnce = false;
                        this.positionSynchronized = false;
                        this.worldReadyValidationTicks = 0;
                    }
                }
            } finally {
                synchronized (this) {
                    if (attemptGeneration == this.connectionAttemptGeneration) {
                        this.connectionAttemptInFlight = false;
                    }
                }
            }
        }, "JavaReforged-RemoteLocalClient-" + this.slotId);
        connectionThread.setDaemon(true);
        connectionThread.start();
    }

    private synchronized boolean installConnection(long attemptGeneration, Connection connection) {
        if (attemptGeneration != this.connectionAttemptGeneration) {
            return false;
        }
        this.installSecondaryConnection(connection);
        return true;
    }
}
