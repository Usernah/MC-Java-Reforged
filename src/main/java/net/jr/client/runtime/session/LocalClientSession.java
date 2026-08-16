package net.jr.client.runtime.session;

import com.mojang.authlib.GameProfile;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;
import net.jr.client.runtime.network.SecondaryClientConnector;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.ActiveClientSlot;
import net.jr.client.runtime.player.SecondaryPlayerProfileFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;

public final class LocalClientSession {
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
    private long lastConnectAttemptMs;
    private boolean joining;
    private boolean joinedWorldOnce;
    private boolean positionSynchronized;
    private int worldReadyValidationTicks;
    private long generation;

    public LocalClientSession(int slotId, @Nullable GameProfile profile) {
        this.slotId = slotId;
        this.profile = profile;
        this.joining = this.isSecondary();
        this.joinedWorldOnce = !this.isSecondary();
        this.positionSynchronized = !this.isSecondary();
        this.worldReadyValidationTicks = 0;
    }

    public static GameProfile createSecondaryProfile(LocalPlayer primaryPlayer, int ordinal) {
        return SecondaryPlayerProfileFactory.create(primaryPlayer.getGameProfile(), ordinal);
    }

    public void ensureConnected(Minecraft minecraft, ClientRuntime players) {
        if (!this.isSecondary()) {
            Connection primaryConnection = requirePrimaryConnection(minecraft);
            this.replaceConnection(primaryConnection);
            players.connections().bind(primaryConnection, this.slotId);
            return;
        }
        if (this.profile == null) {
            throw new IllegalStateException("Secondary session " + this.slotId + " has no profile");
        }
        if (this.connection != null) {
            if (this.connection.isConnected()) {
                return;
            }
            this.disconnect(players);
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
            this.replaceConnection(SecondaryClientConnector.connectToIntegratedServer(minecraft, players, this.slotId, this.profile));
            this.joining = true;
            return;
        }

        if (minecraft.getCurrentServer() == null) {
            this.joining = true;
            return;
        }

        this.connectToRemoteServerAsync(minecraft, players, minecraft.getCurrentServer());
    }

    public void disconnect(ClientRuntime players) {
        this.connectionAttemptGeneration++;
        if (this.connection != null) {
            Connection closingConnection = this.connection;
            ActiveClientSlot.run(this.slotId, () -> {
                closingConnection.disconnect(Component.literal("Closing split test player session"));
            });
            players.connections().unbind(closingConnection);
            this.connection = null;
            this.generation++;
        }
        this.connectionAttemptInFlight = false;
        if (this.isSecondary()) {
            players.slots().slot(this.slotId).clearWorldBinding();
        }
        this.joining = this.isSecondary();
        this.joinedWorldOnce = !this.isSecondary();
        this.positionSynchronized = !this.isSecondary();
        this.worldReadyValidationTicks = 0;
    }

    private static Connection requirePrimaryConnection(Minecraft minecraft) {
        if (minecraft.getConnection() == null) {
            throw new IllegalStateException("Primary player has no ClientPacketListener");
        }
        Connection connection = minecraft.getConnection().getConnection();
        if (connection == null) {
            throw new IllegalStateException("Primary player has no Connection");
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

    public void setJoiningInProgress(boolean joining) {
        this.joining = this.isSecondary() && !this.joinedWorldOnce && joining;
    }

    public void markWorldReady() {
        if (this.isSecondary()) {
            this.joinedWorldOnce = true;
            this.joining = false;
        }
    }

    public boolean validateWorldReadyCandidate(boolean readyCandidate) {
        if (!this.isSecondary() || this.joinedWorldOnce) {
            return true;
        }
        if (!readyCandidate) {
            this.worldReadyValidationTicks = 0;
            return false;
        }
        return ++this.worldReadyValidationTicks >= 2;
    }

    public boolean hasSynchronizedPosition() {
        return this.positionSynchronized;
    }

    public void markPositionSynchronized() {
        this.positionSynchronized = true;
    }

    public void markWorldCleared() {
        if (this.joinedWorldOnce) {
            this.generation++;
        }
        this.joinedWorldOnce = false;
        this.positionSynchronized = !this.isSecondary();
        this.worldReadyValidationTicks = 0;
        if (this.isSecondary()) {
            this.joining = true;
        }
    }

    private void replaceConnection(Connection connection) {
        if (this.connection != connection) {
            this.connection = connection;
            this.generation++;
            this.positionSynchronized = !this.isSecondary();
            this.worldReadyValidationTicks = 0;
        }
    }

    private void connectToRemoteServerAsync(Minecraft minecraft, ClientRuntime players, ServerData serverData) {
        this.connectionAttemptInFlight = true;
        long attemptGeneration = ++this.connectionAttemptGeneration;
        Thread connectionThread = new Thread(() -> {
            Connection openedConnection = null;
            try {
                openedConnection = SecondaryClientConnector.connectToRemoteServer(
                    minecraft,
                    players,
                    this.slotId,
                    this.profile,
                    serverData
                );
                if (attemptGeneration != this.connectionAttemptGeneration) {
                    players.connections().unbind(openedConnection);
                    openedConnection.disconnect(Component.literal("Split session was closed while connecting"));
                    return;
                }
                this.replaceConnection(openedConnection);
                this.joining = true;
            } catch (Exception exception) {
                if (openedConnection != null) {
                    players.connections().unbind(openedConnection);
                    openedConnection.disconnect(Component.literal("Failed to open remote split session"));
                }
                this.connection = null;
            } finally {
                if (attemptGeneration == this.connectionAttemptGeneration) {
                    this.connectionAttemptInFlight = false;
                }
            }
        }, "JavaReforged-RemoteSplit-" + this.slotId);
        connectionThread.setDaemon(true);
        connectionThread.start();
    }
}
