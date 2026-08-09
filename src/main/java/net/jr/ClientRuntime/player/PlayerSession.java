package net.jr.ClientRuntime.player;

import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;
import net.jr.ClientRuntime.network.LocalJoiner;
import net.jr.ClientRuntime.runtime.LocalPlayers;
import net.jr.ClientRuntime.runtime.ActiveSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;

public final class PlayerSession {
    private static final AtomicLong NEXT_SESSION_ID = new AtomicLong();

    private final int slotId;
    private final long sessionId = NEXT_SESSION_ID.incrementAndGet();
    @Nullable
    private final GameProfile profile;
    @Nullable
    private Connection connection;
    private boolean joining;
    private boolean joinedWorldOnce;
    private long generation;

    public PlayerSession(int slotId, @Nullable GameProfile profile) {
        this.slotId = slotId;
        this.profile = profile;
        this.joining = this.isSecondary();
        this.joinedWorldOnce = !this.isSecondary();
    }

    public static GameProfile createSecondaryProfile(LocalPlayer primaryPlayer, int ordinal) {
        String name = primaryPlayer.getGameProfile().name()  + "(" + ordinal + ")";
        UUID uuid = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        return new GameProfile(uuid, name);
    }

    public void ensureConnected(Minecraft minecraft, LocalPlayers players) {
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
        this.replaceConnection(LocalJoiner.connectToIntegratedServer(minecraft, players, this.slotId, this.profile));
        this.joining = true;
    }

    public void disconnect(LocalPlayers players) {
        if (this.connection != null) {
            Connection closingConnection = this.connection;
            ActiveSlot.run(this.slotId, () -> {
                closingConnection.disconnect(Component.literal("Closing split test player session"));
            });
            players.connections().unbind(closingConnection);
            this.connection = null;
            this.generation++;
        }
        if (this.isSecondary()) {
            players.slots().slot(this.slotId).clearWorldBinding();
        }
        this.joining = this.isSecondary();
        this.joinedWorldOnce = !this.isSecondary();
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

    public void markWorldCleared() {
        if (this.joinedWorldOnce) {
            this.generation++;
        }
        this.joinedWorldOnce = false;
        if (this.isSecondary()) {
            this.joining = true;
        }
    }

    private void replaceConnection(Connection connection) {
        if (this.connection != connection) {
            this.connection = connection;
            this.generation++;
        }
    }
}
