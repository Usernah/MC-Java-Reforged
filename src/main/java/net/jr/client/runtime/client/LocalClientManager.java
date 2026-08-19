package net.jr.client.runtime.client;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.jr.client.runtime.context.LocalClientExecution;
import net.jr.client.runtime.network.ConnectionSlotRegistry;
import net.jr.client.runtime.player.LocalPlayerPositionSync;
import net.jr.client.runtime.player.SecondaryPlayerProfileFactory;
import net.jr.client.runtime.render.state.WorldEngineStateScope;
import net.jr.client.runtime.session.LocalClientSession;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;
import net.jr.mixin.runtime.MinecraftActionSSAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;

public final class LocalClientManager {
    private final LocalClientSlotRegistry slots;
    private final ConnectionSlotRegistry connections;
    private final LocalClient[] clients = new LocalClient[LocalClientSlotRegistry.MAX_SLOTS];
    private final LocalClientSession[] sessions = new LocalClientSession[LocalClientSlotRegistry.MAX_SLOTS];

    public LocalClientManager(LocalClientSlotRegistry slots, ConnectionSlotRegistry connections) {
        this.slots = slots;
        this.connections = connections;
        this.clients[0] = new LocalClient(slots.primary());
        this.sessions[0] = new LocalClientSession(0, null);
    }

    public LocalClient client(int slotId) {
        LocalClient client = this.clientOrNull(slotId);
        if (client == null) {
            throw new IllegalStateException("No local client exists for slot " + slotId);
        }
        return client;
    }

    @Nullable
    public LocalClient clientOrNull(int slotId) {
        LocalClientSlotRegistry.requireSlotId(slotId);
        return this.clients[slotId];
    }

    public boolean hasClient(int slotId) {
        return this.clientOrNull(slotId) != null;
    }

    public int count() {
        int count = 0;
        for (LocalClient client : this.clients) {
            if (client != null) {
                count++;
            }
        }
        return count;
    }

    public List<LocalClient> all() {
        return Arrays.stream(this.clients).filter(Objects::nonNull).toList();
    }

    public LocalClientSession session(int slotId) {
        LocalClientSession session = this.sessionOrNull(slotId);
        if (session == null) {
            throw new IllegalStateException("No local client session exists for slot " + slotId);
        }
        return session;
    }

    @Nullable
    public LocalClientSession sessionOrNull(int slotId) {
        LocalClientSlotRegistry.requireSlotId(slotId);
        return this.sessions[slotId];
    }

    public boolean isJoining(int slotId) {
        LocalClientSession session = this.sessionOrNull(slotId);
        return session != null && session.isJoiningInProgress();
    }

    public void ensureClient(Minecraft minecraft, int slotId) {
        LocalClientSlotRegistry.requireSlotId(slotId);
        if (slotId == 0) {
            this.session(0).ensureConnected(minecraft, this.connections);
            return;
        }

        LocalPlayer primaryPlayer = this.slots.primary().gameplayState().player();
        if (primaryPlayer == null) {
            throw new IllegalStateException("Cannot create a secondary local client before slot 0 has a LocalPlayer");
        }

        boolean createdClient = false;
        boolean createdSession = false;

        if (this.clients[slotId] == null) {
            this.clients[slotId] = new LocalClient(this.slots.slot(slotId));
            createdClient = true;
        }

        if (this.sessions[slotId] == null) {
            this.sessions[slotId] = new LocalClientSession(
                slotId,
                SecondaryPlayerProfileFactory.create(primaryPlayer.getGameProfile(), slotId + 1)
            );
            createdSession = true;
        }

        try {
            this.sessions[slotId].ensureConnected(minecraft, this.connections);
        } catch (RuntimeException | Error error) {
            if (createdSession) {
                this.sessions[slotId].disconnect(this.connections);
                this.sessions[slotId] = null;
            }
            if (createdClient) {
                this.slots.slot(slotId).clearWorldBinding();
                this.clients[slotId] = null;
            }
            throw error;
        }
    }

    public void disconnectSecondary(int slotId) {
        if (slotId <= 0 || slotId >= LocalClientSlotRegistry.MAX_SLOTS) {
            throw new IllegalArgumentException("Only secondary local clients can be disconnected here: " + slotId);
        }

        LocalClientSession session = this.sessions[slotId];
        if (session != null) {
            session.disconnect(this.connections);
        }

        this.slots.slot(slotId).clearWorldBinding();
        this.sessions[slotId] = null;
        this.clients[slotId] = null;
    }

    public void disconnectAllSecondary() {
        for (int slotId = 1; slotId < LocalClientSlotRegistry.MAX_SLOTS; slotId++) {
            if (this.clients[slotId] != null || this.sessions[slotId] != null) {
                this.disconnectSecondary(slotId);
            }
        }
    }

    public int onClientLevelCleared(ClientPacketListener listener) {
        int slotId = this.slotForClientPacketListener(listener);
        this.slots.slot(slotId).clearWorldBinding();
        LocalClientSession session = this.sessions[slotId];
        if (session != null) {
            session.markWorldCleared();
        }
        if (slotId == 0) {
            this.disconnectAllSecondary();
        }
        return slotId;
    }

    public void onInitialPositionSynchronized(ClientPacketListener listener) {
        int slotId = this.slotForClientPacketListener(listener);
        LocalClientSession session = this.sessions[slotId];
        if (session != null) {
            session.markPositionSynchronized();
        }
    }

    public void tickSecondarySessions(Minecraft minecraft) {
        if (!LocalClientReadinessPolicy.gameplayReady(0)) {
            return;
        }

        for (int slotId = 1; slotId < LocalClientSlotRegistry.MAX_SLOTS; slotId++) {
            LocalClient client = this.clients[slotId];
            LocalClientSession session = this.sessions[slotId];
            if (client == null || session == null) {
                continue;
            }
            session.ensureConnected(minecraft, this.connections);
            this.tickSecondarySession(minecraft, client, session);
        }
    }

    @Nullable
    public ClientPacketListener primaryPacketListener() {
        LocalPlayer player = this.slots.primary().gameplayState().player();
        return player != null ? player.connection : null;
    }

    @Nullable
    public Connection primaryConnection() {
        ClientPacketListener listener = this.primaryPacketListener();
        return listener != null ? listener.getConnection() : null;
    }

    public boolean isPrimaryPacketListener(ClientPacketListener listener) {
        return listener != null && listener == this.primaryPacketListener();
    }

    public int slotForClientPacketListener(ClientPacketListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        Connection connection = listener.getConnection();
        if (connection != null) {
            Integer mappedSlot = this.connections.slotOrNull(connection);
            if (mappedSlot != null) {
                return mappedSlot;
            }
        }
        if (this.isPrimaryPacketListener(listener)) {
            return 0;
        }
        throw new IllegalStateException("No local client slot is bound to ClientPacketListener " + listener);
    }

    private void tickSecondarySession(
        Minecraft minecraft,
        LocalClient client,
        LocalClientSession session
    ) {
        LocalClientSlot slot = client.slot();
        Connection connection = session.connection();
        if (connection != null) {
            LocalClientExecution.runForClient(client.slotId(), connection::tick);
            if (!connection.isConnected()) {
                session.disconnect(this.connections);
                return;
            }
        }

        LocalClientExecution.runForClient(client.slotId(), () -> {
            boolean gameplayBound = LocalClientReadinessPolicy.gameplayBound(client);
            LocalPlayer candidatePlayer = client.player();
            boolean connectionBound = candidatePlayer != null
                && candidatePlayer.connection != null
                && candidatePlayer.connection.getConnection() == connection;
            boolean readyCandidate = gameplayBound
                && session.hasSynchronizedPosition()
                && connection != null
                && connection.isConnected()
                && connectionBound;

            if (!session.validateWorldReadyCandidate(readyCandidate)) {
                session.setJoiningInProgress(true);
                return;
            }

            session.markWorldReady();
            session.setJoiningInProgress(false);

            try (WorldEngineStateScope ignoredEngines = WorldEngineStateScope.bind(minecraft, slot)) {
                ClientLevel level = client.level();
                MultiPlayerGameMode gameMode = client.gameMode();
                LocalPlayer player = client.player();
                if (minecraft.isPaused()) {
                    return;
                }
                level.tickRateManager().tick();
                if (client.gameplay().rightClickDelay() > 0) {
                    client.gameplay().setRightClickDelay(client.gameplay().rightClickDelay() - 1);
                }
                gameMode.tick();
                this.handleSecondaryKeybinds(minecraft, client);
                level.pollLightUpdates();
                level.getChunkSource().getLightEngine().runLightUpdates();
                level.tickEntities();
                level.tick(() -> true);
                if (level.tickRateManager().runsNormally()) {
                    level.animateTick(player.getBlockX(), player.getBlockY(), player.getBlockZ());
                    minecraft.particleEngine.tick();
                }
                LocalPlayerPositionSync.forcePosition(player);
            }
        });
    }

    private void handleSecondaryKeybinds(Minecraft minecraft, LocalClient client) {
        if (minecraft.gui.overlay() != null || client.screen() != null) {
            return;
        }
        ((MinecraftActionSSAccessor)minecraft).splitTest$handleKeybinds();
        if (client.gameplay().missTime() > 0) {
            client.gameplay().setMissTime(client.gameplay().missTime() - 1);
        }
    }
}
