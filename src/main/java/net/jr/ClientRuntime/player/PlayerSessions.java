package net.jr.ClientRuntime.player;

import net.jr.mixin.SSM.MinecraftActionSSAccessor;
import net.jr.client.input.InputApi;
import net.jr.ClientRuntime.runtime.ClientBoundary;
import net.jr.ClientRuntime.runtime.LocalPlayerChunkPressure;
import net.jr.ClientRuntime.runtime.LocalPlayers;
import net.jr.ClientRuntime.runtime.WorldEngineStateScope;
import net.jr.ClientRuntime.slot.PlayerSlot;
import net.jr.ClientRuntime.slot.PlayerSlots;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;

public final class PlayerSessions {
    private final PlayerSession[] sessions = new PlayerSession[PlayerSlots.MAX_SLOTS];

    public PlayerSessions() {
        this.sessions[0] = new PlayerSession(0, null);
    }

    public void ensurePlayerCount(Minecraft minecraft, LocalPlayers players, int playerCount) {
        if (playerCount < 1 || playerCount > PlayerSlots.MAX_SLOTS) {
            throw new IllegalArgumentException("playerCount must be between 1 and " + PlayerSlots.MAX_SLOTS);
        }
        this.session(0).ensureConnected(minecraft, players);
        LocalPlayer primaryPlayer = players.slots().slot(0).gameplayState().player();
        if (primaryPlayer == null) {
            throw new IllegalStateException("Cannot create secondary players before slot 0 has a LocalPlayer");
        }
        for (int slotId = 1; slotId < playerCount; slotId++) {
            if (this.sessions[slotId] == null) {
                this.sessions[slotId] = new PlayerSession(slotId, PlayerSession.createSecondaryProfile(primaryPlayer, slotId + 1));
            }
            this.sessions[slotId].ensureConnected(minecraft, players);
        }
        for (int slotId = playerCount; slotId < PlayerSlots.MAX_SLOTS; slotId++) {
            if (this.sessions[slotId] != null) {
                this.sessions[slotId].disconnect(players);
                this.sessions[slotId] = null;
            }
        }
    }

    public PlayerSession session(int slotId) {
        PlayerSession session = this.sessions[slotId];
        if (session == null) {
            throw new IllegalStateException("No session exists for slot " + slotId);
        }
        return session;
    }

    public PlayerSession sessionOrNull(int slotId) {
        return this.sessions[slotId];
    }

    public void ensureClient(Minecraft minecraft, LocalPlayers players, int slotId) {
        if (slotId < 0 || slotId >= PlayerSlots.MAX_SLOTS) {
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
            this.sessions[slotId] = new PlayerSession(slotId, PlayerSession.createSecondaryProfile(primaryPlayer, slotId + 1));
        }
        this.sessions[slotId].ensureConnected(minecraft, players);
    }

    public void disconnectSecondaryClient(LocalPlayers players, int slotId) {
        if (slotId <= 0 || slotId >= PlayerSlots.MAX_SLOTS) {
            throw new IllegalArgumentException("Only secondary local clients can be disconnected here: " + slotId);
        }
        PlayerSession session = this.sessions[slotId];
        if (session != null) {
            session.disconnect(players);
        }
    }

    public boolean isJoining(int slotId) {
        PlayerSession session = this.sessions[slotId];
        return session != null && session.isJoiningInProgress();
    }

    public void onClientLevelCleared(ClientPacketListener listener, LocalPlayers players) {
        int slotId = players.slotForClientPacketListener(listener);
        players.slots().slot(slotId).clearWorldBinding();
        if (slotId == 0) {
            this.session(0).markWorldCleared();
            this.disconnectSecondarySessions(players);
            this.clearAllScreens(players);
            players.returnToPrimaryOnly();
        } else {
            PlayerSession session = this.sessions[slotId];
            if (session != null) {
                session.markWorldCleared();
            }
        }
    }

    public void onInitialPositionSynchronized(ClientPacketListener listener, LocalPlayers players) {
        int slotId = players.slotForClientPacketListener(listener);
        PlayerSession session = this.sessions[slotId];
        if (session != null) {
            session.markPositionSynchronized();
        }
    }

    public void disconnectSecondarySessions(LocalPlayers players) {
        for (int slotId = 1; slotId < PlayerSlots.MAX_SLOTS; slotId++) {
            PlayerSession session = this.sessions[slotId];
            if (session != null) {
                session.disconnect(players);
            }
        }
    }

    private void clearAllScreens(LocalPlayers players) {
        for (int slotId = 0; slotId < PlayerSlots.MAX_SLOTS; slotId++) {
            players.slots().slot(slotId).screenState().bindScreen(null);
        }
    }

    public void tickSecondarySessions(Minecraft minecraft, LocalPlayers players) {
        if (!players.slotGameplayReady(players.primarySlot())) {
            return;
        }
        for (int slotId = 1; slotId < PlayerSlots.MAX_SLOTS; slotId++) {
            PlayerSession session = this.sessions[slotId];
            if (session == null || !players.slots().slot(slotId).connected()) {
                continue;
            }
            session.ensureConnected(minecraft, players);
            this.tickSecondarySession(minecraft, players, session);
        }
    }

    private void tickSecondarySession(Minecraft minecraft, LocalPlayers players, PlayerSession session) {
        PlayerSlot slot = players.slots().slot(session.slotId());
        Connection connection = session.connection();
        if (connection != null) {
            ClientBoundary.runForSlot(slot, connection::tick);
            if (!connection.isConnected()) {
                session.disconnect(players);
                return;
            }
        }

        ClientBoundary.runForSlot(slot, () -> {
            boolean gameplayBound = players.slotGameplayBound(slot);
            if (!gameplayBound) {
                session.setJoiningInProgress(true);
                return;
            }
            if (!session.hasSynchronizedPosition()) {
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
                InputApi.tickRightClickDelay();
                gameMode.tick();
                this.handleSecondaryKeybinds(minecraft, slot);
                this.handleSecondaryContinuousAttack(minecraft);
                level.pollLightUpdates();
                level.getChunkSource().getLightEngine().runLightUpdates();
                level.tickEntities();
                level.tick(() -> true);
                if (level.tickRateManager().runsNormally()) {
                    level.animateTick(player.getBlockX(), player.getBlockY(), player.getBlockZ());
                    minecraft.particleEngine.tick();
                }
                LocalPlayerChunkPressure.forcePosition(player);
            }
        });
    }

    private void handleSecondaryKeybinds(Minecraft minecraft, PlayerSlot slot) {
        if (minecraft.gui.overlay() != null || slot.screenState().screen() != null) {
            return;
        }

        ((MinecraftActionSSAccessor)minecraft).splitTest$handleKeybinds();
        InputApi.tickMissTime();
    }

    private void handleSecondaryContinuousAttack(Minecraft minecraft) {
        if (!minecraft.options.keyAttack.isDown()) {
            return;
        }

        ((MinecraftActionSSAccessor)minecraft).splitTest$continueAttack(true);
    }

}
