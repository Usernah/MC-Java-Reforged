package net.jr.client.runtime.session;

import net.jr.mixin.runtime.MinecraftActionSSAccessor;
import net.jr.client.runtime.context.LocalClientExecution;
import net.jr.client.runtime.player.LocalPlayerPositionSync;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.render.state.WorldEngineStateScope;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.jr.client.runtime.slot.LocalClientSlots;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;

public final class LocalClientSessions {
    private final LocalClientSession[] sessions = new LocalClientSession[LocalClientSlots.MAX_SLOTS];

    public LocalClientSessions() {
        this.sessions[0] = new LocalClientSession(0, null);
    }

    public void ensurePlayerCount(Minecraft minecraft, ClientRuntime players, int playerCount) {
        if (playerCount < 1 || playerCount > LocalClientSlots.MAX_SLOTS) {
            throw new IllegalArgumentException("playerCount must be between 1 and " + LocalClientSlots.MAX_SLOTS);
        }
        this.session(0).ensureConnected(minecraft, players);
        LocalPlayer primaryPlayer = players.slots().slot(0).gameplayState().player();
        if (primaryPlayer == null) {
            throw new IllegalStateException("Cannot create secondary players before slot 0 has a LocalPlayer");
        }
        for (int slotId = 1; slotId < playerCount; slotId++) {
            if (this.sessions[slotId] == null) {
                this.sessions[slotId] = new LocalClientSession(slotId, LocalClientSession.createSecondaryProfile(primaryPlayer, slotId + 1));
            }
            this.sessions[slotId].ensureConnected(minecraft, players);
        }
        for (int slotId = playerCount; slotId < LocalClientSlots.MAX_SLOTS; slotId++) {
            if (this.sessions[slotId] != null) {
                this.sessions[slotId].disconnect(players);
                this.sessions[slotId] = null;
            }
        }
    }

    public LocalClientSession session(int slotId) {
        LocalClientSession session = this.sessions[slotId];
        if (session == null) {
            throw new IllegalStateException("No session exists for slot " + slotId);
        }
        return session;
    }

    public LocalClientSession sessionOrNull(int slotId) {
        return this.sessions[slotId];
    }

    public void ensureClient(Minecraft minecraft, ClientRuntime players, int slotId) {
        if (slotId < 0 || slotId >= LocalClientSlots.MAX_SLOTS) {
            throw new IllegalArgumentException("Invalid local client " + slotId);
        }
        if (slotId == 0) {
            this.session(0).ensureConnected(minecraft, players);
            return;
        }

        LocalPlayer primaryPlayer = players.slots().slot(0).gameplayState().player();
        if (primaryPlayer == null) {
            throw new IllegalStateException("Cannot create secondary players before slot 0 has a LocalPlayer");
        }
        if (this.sessions[slotId] == null) {
            this.sessions[slotId] = new LocalClientSession(slotId, LocalClientSession.createSecondaryProfile(primaryPlayer, slotId + 1));
        }
        this.sessions[slotId].ensureConnected(minecraft, players);
    }

    public void disconnectSecondaryClient(ClientRuntime players, int slotId) {
        if (slotId <= 0 || slotId >= LocalClientSlots.MAX_SLOTS) {
            throw new IllegalArgumentException("Only secondary local clients can be disconnected here: " + slotId);
        }
        LocalClientSession session = this.sessions[slotId];
        if (session != null) {
            session.disconnect(players);
        }
    }

    public boolean isJoining(int slotId) {
        LocalClientSession session = this.sessions[slotId];
        return session != null && session.isJoiningInProgress();
    }

    public void onClientLevelCleared(ClientPacketListener listener, ClientRuntime players) {
        int slotId = players.slotForClientPacketListener(listener);
        players.slots().slot(slotId).clearWorldBinding();
        if (slotId == 0) {
            this.session(0).markWorldCleared();
            this.disconnectSecondarySessions(players);
            this.clearAllScreens(players);
            players.returnToPrimaryOnly(Minecraft.getInstance());
        } else {
            LocalClientSession session = this.sessions[slotId];
            if (session != null) {
                session.markWorldCleared();
            }
        }
    }

    public void onInitialPositionSynchronized(ClientPacketListener listener, ClientRuntime players) {
        int slotId = players.slotForClientPacketListener(listener);
        LocalClientSession session = this.sessions[slotId];
        if (session != null) {
            session.markPositionSynchronized();
        }
    }

    public void disconnectSecondarySessions(ClientRuntime players) {
        for (int slotId = 1; slotId < LocalClientSlots.MAX_SLOTS; slotId++) {
            LocalClientSession session = this.sessions[slotId];
            if (session != null) {
                session.disconnect(players);
            }
        }
    }

    private void clearAllScreens(ClientRuntime players) {
        for (int slotId = 0; slotId < LocalClientSlots.MAX_SLOTS; slotId++) {
            players.slots().slot(slotId).screenState().bindScreen(null);
        }
    }

    public void tickSecondarySessions(Minecraft minecraft, ClientRuntime players) {
        if (!players.slotGameplayReady(players.primarySlot())) {
            return;
        }
        for (int slotId = 1; slotId < LocalClientSlots.MAX_SLOTS; slotId++) {
            LocalClientSession session = this.sessions[slotId];
            if (session == null || !players.slots().slot(slotId).connected()) {
                continue;
            }
            session.ensureConnected(minecraft, players);
            this.tickSecondarySession(minecraft, players, session);
        }
    }

    private void tickSecondarySession(Minecraft minecraft, ClientRuntime players, LocalClientSession session) {
        LocalClientSlot slot = players.slots().slot(session.slotId());
        Connection connection = session.connection();
        if (connection != null) {
            LocalClientExecution.runForSlot(slot, connection::tick);
            if (!connection.isConnected()) {
                session.disconnect(players);
                return;
            }
        }

        LocalClientExecution.runForSlot(slot, () -> {
            boolean gameplayBound = players.slotGameplayBound(slot);
            LocalPlayer candidatePlayer = slot.gameplayState().player();
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
                ClientLevel level = slot.renderState().level();
                MultiPlayerGameMode gameMode = slot.gameplayState().gameMode();
                LocalPlayer player = slot.gameplayState().player();
                if (minecraft.isPaused()) {
                    return;
                }
                level.tickRateManager().tick();
                if (slot.gameplayState().rightClickDelay() > 0) {
                    slot.gameplayState().setRightClickDelay(slot.gameplayState().rightClickDelay() - 1);
                }
                gameMode.tick();
                this.handleSecondaryKeybinds(minecraft, slot);
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

    private void handleSecondaryKeybinds(Minecraft minecraft, LocalClientSlot slot) {
        if (minecraft.gui.overlay() != null || slot.screenState().screen() != null) {
            return;
        }

        ((MinecraftActionSSAccessor)minecraft).splitTest$handleKeybinds();
        if (slot.gameplayState().missTime() > 0) {
            slot.gameplayState().setMissTime(slot.gameplayState().missTime() - 1);
        }
    }

}
