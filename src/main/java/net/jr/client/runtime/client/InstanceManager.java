package net.jr.client.runtime.client;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.jr.client.compat.create.CreateFlywheelLifecycle;
import net.jr.client.runtime.context.InstanceExecution;
import net.jr.client.compat.create.CreateRuntimeState;
import net.jr.client.runtime.audio.SlotSoundRouter;
import net.jr.client.runtime.network.ConnectionSlotRegistry;
import net.jr.client.runtime.player.PositionSync;
import net.jr.client.runtime.player.SecondaryProfile;
import net.jr.client.runtime.render.state.WorldEngineStateScope;
import net.jr.client.runtime.session.ClientSession;
import net.jr.client.runtime.slot.ClientSlots;
import net.jr.client.runtime.slot.Slots;
import net.jr.mixin.accessors.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.neoforged.neoforge.event.EventHooks;

public final class InstanceManager {
    private final ClientSlots slots;
    private final ConnectionSlotRegistry connections;
    private final ClientInstance[] clients = new ClientInstance[ClientSlots.MAX_SLOTS];
    private final ClientSession[] sessions = new ClientSession[ClientSlots.MAX_SLOTS];
    private int tickOwnerSlotId = -1;

    public InstanceManager(ClientSlots slots, ConnectionSlotRegistry connections) {
        this.slots = slots;
        this.connections = connections;
        this.clients[0] = new ClientInstance(slots.primary());
        this.sessions[0] = new ClientSession(0, null);
    }

    public ClientInstance client(int slotId) {
        ClientInstance client = this.clientOrNull(slotId);
        if (client == null) {
            throw new IllegalStateException("No local client exists for slot " + slotId);
        }
        return client;
    }

    @Nullable
    public ClientInstance clientOrNull(int slotId) {
        ClientSlots.requireSlotId(slotId);
        return this.clients[slotId];
    }

    public boolean hasClient(int slotId) {
        return this.clientOrNull(slotId) != null;
    }

    public int count() {
        int count = 0;
        for (ClientInstance client : this.clients) {
            if (client != null) {
                count++;
            }
        }
        return count;
    }

    public List<ClientInstance> all() {
        return Arrays.stream(this.clients).filter(Objects::nonNull).toList();
    }

    public ClientSession session(int slotId) {
        ClientSession session = this.sessionOrNull(slotId);
        if (session == null) {
            throw new IllegalStateException("No local client session exists for slot " + slotId);
        }
        return session;
    }

    @Nullable
    public ClientSession sessionOrNull(int slotId) {
        ClientSlots.requireSlotId(slotId);
        return this.sessions[slotId];
    }

    public boolean isJoining(int slotId) {
        ClientSession session = this.sessionOrNull(slotId);
        return session != null && session.isJoiningInProgress();
    }

    public void ensureClient(Minecraft minecraft, int slotId) {
        ClientSlots.requireSlotId(slotId);
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
            this.clients[slotId] = new ClientInstance(this.slots.slot(slotId));
            createdClient = true;
        }

        if (this.sessions[slotId] == null) {
            this.sessions[slotId] = new ClientSession(
                slotId,
                SecondaryProfile.create(primaryPlayer.getGameProfile(), slotId + 1)
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
        if (slotId <= 0 || slotId >= ClientSlots.MAX_SLOTS) {
            throw new IllegalArgumentException("Only secondary local clients can be disconnected here: " + slotId);
        }

        SlotSoundRouter.stopSlotSounds(Minecraft.getInstance().getSoundManager(), slotId);

        ClientSession session = this.sessions[slotId];
        if (session != null) {
            session.markWorldCleared();
        }

        ClientInstance client = this.clients[slotId];
        CreateFlywheelLifecycle.release(client);

        if (session != null) {
            session.disconnect(this.connections);
        }

        this.slots.slot(slotId).clearWorldBinding();
        this.sessions[slotId] = null;
        this.clients[slotId] = null;
    }

    public void disconnectAllSecondary() {
        for (int slotId = 1; slotId < ClientSlots.MAX_SLOTS; slotId++) {
            if (this.clients[slotId] != null || this.sessions[slotId] != null) {
                this.disconnectSecondary(slotId);
            }
        }
    }

    public int onClientLevelCleared(ClientPacketListener listener) {
        Connection source = listener.getConnection();
        Integer mappedSlot = source != null ? this.connections.slotOrNull(source) : null;
        if (mappedSlot == null && !this.isPrimaryPacketListener(listener)) {
            return -1;
        }
        int slotId = mappedSlot != null ? mappedSlot : 0;
        ClientSession session = this.sessions[slotId];
        if (session != null && session.connection() != null && !session.owns(source)) {
            return slotId;
        }
        if (session != null) {
            session.markWorldCleared();
        }
        SlotSoundRouter.stopSlotSounds(Minecraft.getInstance().getSoundManager(), slotId);
        CreateFlywheelLifecycle.release(this.clients[slotId]);
        this.slots.slot(slotId).clearWorldBinding();
        if (slotId == 0) {
            this.disconnectAllSecondary();
        }
        return slotId;
    }

    public void onInitialPositionSynchronized(ClientPacketListener listener) {
        Connection source = listener.getConnection();
        Integer mappedSlot = source != null ? this.connections.slotOrNull(source) : null;
        if (mappedSlot == null && !this.isPrimaryPacketListener(listener)) {
            return;
        }
        int slotId = mappedSlot != null ? mappedSlot : 0;
        ClientSession session = this.sessions[slotId];
        if (session != null) {
            session.markPositionSynchronized(source);
        }
    }

    public int resolveTickOwnerSlot() {
        for (int slotId = 0; slotId < ClientSlots.MAX_SLOTS; slotId++) {
            if (ReadinessPolicy.gameplayReady(slotId)) {
                this.tickOwnerSlotId = slotId;
                return slotId;
            }
        }

        this.tickOwnerSlotId = -1;
        return -1;
    }

    public int tickOwnerSlotId() {
        return this.tickOwnerSlotId;
    }
    public void tickSecondarySessions(Minecraft minecraft) {
        int ownerSlotId = this.tickOwnerSlotId;
        if (ownerSlotId < 0) {
            return;
        }

        for (int slotId = 1; slotId < ClientSlots.MAX_SLOTS; slotId++) {
            if (slotId == ownerSlotId) {
                continue;
            }

            ClientInstance client = this.clients[slotId];
            ClientSession session = this.sessions[slotId];
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
        ClientInstance client,
        ClientSession session
    ) {
        Slots slot = client.slot();
        Connection connection = session.connection();
        if (connection != null) {
            InstanceExecution.runForClient(client.slotId(), connection::tick);
            if (!connection.isConnected()) {
                session.disconnect(this.connections);
                return;
            }
        }

        InstanceExecution.runForClient(client.slotId(), () -> {
            boolean gameplayBound = ReadinessPolicy.gameplayBound(client);
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
                level.update();
                level.tickEntities();
                level.tickBlockEntities();
                BooleanSupplier haveTime = () -> true;
                EventHooks.fireLevelTickPre(level, haveTime);
                level.tick(haveTime);
                EventHooks.fireLevelTickPost(level, haveTime);
                CreateRuntimeState.tickContraptionsAfterWorldTick(level);
                if (level.tickRateManager().runsNormally()) {
                    level.animateTick(player.getBlockX(), player.getBlockY(), player.getBlockZ());
                    minecraft.particleEngine.tick();
                }
                PositionSync.forcePosition(player);
            }
        });
    }

    private void handleSecondaryKeybinds(Minecraft minecraft, ClientInstance client) {
        if (minecraft.gui.overlay() != null || client.screen() != null) {
            return;
        }
        ((MinecraftAccessor)minecraft).javaReforged$handleKeybinds();
        if (client.gameplay().missTime() > 0) {
            client.gameplay().setMissTime(client.gameplay().missTime() - 1);
        }
    }
}
