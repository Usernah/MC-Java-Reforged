package net.jr.experimental_features.minigame.battle;

import net.jr.Java_reforged;
import net.jr.experimental_features.minigame.MapPoints;
import net.jr.experimental_features.minigame.PointGroup;
import net.jr.experimental_features.minigame.game.GameModeContext;
import net.jr.experimental_features.minigame.game.GameModeState;
import net.jr.experimental_features.minigame.network.BattleNetwork;
import net.jr.registry.ModSounds;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = Java_reforged.MODID)
public final class BattleRuntime {

    private static final Map<UUID, BattlePlayerState> PLAYER_STATES = new HashMap<>();
    private static final Map<UUID, GameType> PENDING_GAME_MODE_RESTORE = new HashMap<>();
    private static final int MIN_PLAYERS_TO_START = 2;
    private static final int STARTING_LIVES = 3;
    private static final int REFILL_INTERVAL_TICKS = 600 ;//1_800
    private static final String BATTLE_NAMETAG_TEAM = "javareforged_battle";

    @Nullable
    private static BattlePhase currentPhase;
    @Nullable
    private static UUID winnerId;
    private static int phaseTicksRemaining;
    private static int phaseTicksElapsed;
    private static final Vec3 START_LOOK = Vec3.atCenterOf(new BlockPos(219, 70, -220));

    private BattleRuntime() {
    }

    public static boolean startEngineMinigame(MinecraftServer server) {
        if (GameModeContext.isBattle()) {
            return false;
        }

        resetSession();
        GameModeContext.setCurrentMode(GameModeState.BATTLE);
        startBattle(server);
        return true;
    }

    public static boolean stopEngineMinigame(MinecraftServer server) {
        if (!GameModeContext.isBattle()) {
            return false;
        }

        GameModeContext.reset();
        cleanupInactiveBattle(server);
        return true;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (!GameModeContext.isBattle()) {
            cleanupInactiveBattle(server);
            return;
        }

        if (currentPhase == null) {
            startBattle(server);
            return;
        }

        switch (currentPhase) {
            case WAIT_FOR_PLAYERS -> tickWaitingForPlayers(server);
            case COUNTDOWN -> tickCountdown(server);
            case START, INMUNITY_COUNTDOWN, BATTLE, SHOWDOWN, WINNER_ANOUNCEMENT, FINISH -> tickTimedPhase(server);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GameType originalMode = PENDING_GAME_MODE_RESTORE.remove(player.getUUID());
            if (originalMode != null) {
                player.setCamera(player);
                player.setGameMode(originalMode);
            }
            BattleNetwork.sendState(player, currentPhase, phaseTicksRemaining, phaseTicksElapsed);
        }
    }

    @SubscribeEvent
    public static void onContainerOpened(PlayerContainerEvent.Open event) {
        if (GameModeContext.isBattle() && event.getEntity() instanceof ServerPlayer player) {
            if (player.level() == resolveBattleLevel(player.level().getServer())) {
                BattleChestLoot.markOpened(event, player.level());
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        GameModeContext.reset();
        cleanupInactiveBattle(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        GameModeContext.reset();
        resetSession();
        PENDING_GAME_MODE_RESTORE.clear();
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!GameModeContext.isBattle() || !isMovementLocked()) {
            return;
        }

        if (event.getEntity().level().isClientSide()) {
            return;
        }

        event.getEntity().setDeltaMovement(0.0D, event.getEntity().getDeltaMovement().y, 0.0D);
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!GameModeContext.isBattle() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.level().isClientSide()) {
            return;
        }

        BattlePhase phase = currentPhase;
        if (phase == null || !isTrackedParticipant(player.getUUID())) {
            return;
        }

        if (phase == BattlePhase.WAIT_FOR_PLAYERS
            || phase == BattlePhase.COUNTDOWN
            || phase == BattlePhase.START
            || phase == BattlePhase.INMUNITY_COUNTDOWN
            || phase == BattlePhase.WINNER_ANOUNCEMENT
            || phase == BattlePhase.FINISH
            || player.isSpectator()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!GameModeContext.isBattle() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.level().isClientSide()) {
            return;
        }

        BattlePlayerState state = PLAYER_STATES.get(player.getUUID());
        if (state == null || currentPhase == null || player.isSpectator()) {
            return;
        }

        handleBattleDeath(player, state, event.getSource());
        broadcastCurrentState(player.level().getServer());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!GameModeContext.isBattle() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        BattlePlayerState state = PLAYER_STATES.get(player.getUUID());
        if (state == null) {
            return;
        }

        if (currentPhase == BattlePhase.WINNER_ANOUNCEMENT || currentPhase == BattlePhase.FINISH) {
            if (player.getUUID().equals(winnerId)) {
                player.setGameMode(GameType.SURVIVAL);
            } else {
                player.setGameMode(GameType.SPECTATOR);
                ServerPlayer winner = winnerId == null ? null : player.level().getServer().getPlayerList().getPlayer(winnerId);
                if (winner != null) {
                    player.setCamera(winner);
                }
            }
            broadcastCurrentState(player.level().getServer());
            return;
        }

        if (state.lives > 0) {
            resetPlayerForRound(player, resolveRespawnPoint(player));
            broadcastCurrentState(player.level().getServer());
            return;
        }

        player.setGameMode(GameType.SPECTATOR);
        FoodData foodData = player.getFoodData();
        foodData.setFoodLevel(20);
        foodData.setSaturation(5.0F);
        broadcastCurrentState(player.level().getServer());
    }

    public static boolean isActive() {
        return GameModeContext.isBattle() && currentPhase != null;
    }

    @Nullable
    public static BattlePhase getCurrentPhase() {
        return currentPhase;
    }

    @Nullable
    public static UUID getWinnerId() {
        return winnerId;
    }

    public static boolean isParticipant(UUID playerId) {
        return PLAYER_STATES.containsKey(playerId);
    }

    public static int getRemainingLives(UUID playerId) {
        BattlePlayerState state = PLAYER_STATES.get(playerId);
        return state == null ? 0 : state.lives;
    }

    public static int getPhaseTicksRemaining() {
        return phaseTicksRemaining;
    }

    public static int getPhaseTicksElapsed() {
        return phaseTicksElapsed;
    }

    public static int getRemainingSecondsCeil() {
        return (phaseTicksRemaining + 19) / 20;
    }

    public static boolean isMovementLocked() {
        return currentPhase != null && currentPhase.blocksHorizontalMovement();
    }

    public static void resetSession() {
        currentPhase = null;
        winnerId = null;
        phaseTicksRemaining = 0;
        phaseTicksElapsed = 0;
        PLAYER_STATES.clear();
        BattleChestLoot.reset();
    }

    private static void startBattle(MinecraftServer server) {
        winnerId = null;
        enterPhase(server, BattlePhase.WAIT_FOR_PLAYERS);
        stageWaitingPlayers(server);
    }

    private static void tickWaitingForPlayers(MinecraftServer server) {
        stageWaitingPlayers(server);
        if (server.getTickCount() % 20 == 0) {
            BattleNetwork.broadcastState(server, currentPhase, phaseTicksRemaining, phaseTicksElapsed);
        }
        if (countReadyPlayers(server) >= MIN_PLAYERS_TO_START) {
            enterPhase(server, BattlePhase.COUNTDOWN);
        }
    }

    private static void tickCountdown(MinecraftServer server) {
        stageWaitingPlayers(server);
        if (countReadyPlayers(server) < MIN_PLAYERS_TO_START) {
            enterPhase(server, BattlePhase.WAIT_FOR_PLAYERS);
            return;
        }

        tickPhaseClock(server);
        if (phaseTicksRemaining <= 0) {
            advancePhase(server);
        }
    }

    private static void tickTimedPhase(MinecraftServer server) {
        if (currentPhase == null) {
            return;
        }

        if (shouldReturnToWaiting(server)) {
            enterPhase(server, BattlePhase.WAIT_FOR_PLAYERS);
            return;
        }

        if (currentPhase == BattlePhase.START || currentPhase == BattlePhase.INMUNITY_COUNTDOWN
            || currentPhase == BattlePhase.BATTLE || currentPhase == BattlePhase.SHOWDOWN) {
            if (tryResolveWinnerFromAlivePlayers(server)) {
                return;
            }

            if (currentPhase == BattlePhase.SHOWDOWN && phaseTicksRemaining <= 0) {
                resolveWinnerAtShowdownTimeout(server);
                return;
            }
        }

        tickPhaseClock(server);
        if (currentPhase == BattlePhase.BATTLE && phaseTicksElapsed > 0
            && phaseTicksElapsed % REFILL_INTERVAL_TICKS == 0) {
            ServerLevel level = resolveBattleLevel(server);
            if (level != null && BattleChestLoot.refillOpened(server, level) > 0) {
                BattleNetwork.broadcastChestRefill(server);
            }
        }
        if (currentPhase == BattlePhase.WINNER_ANOUNCEMENT || currentPhase == BattlePhase.FINISH) {
            enforceWinnerCamera(server);
        }
        if (phaseTicksRemaining <= 0) {
            advancePhase(server);
        }
    }

    private static void tickPhaseClock(MinecraftServer server) {
        if (phaseTicksRemaining > 0) {
            phaseTicksRemaining--;
            phaseTicksElapsed++;
            if (phaseTicksRemaining % 20 == 0) {
                BattleNetwork.broadcastState(server, currentPhase, phaseTicksRemaining, phaseTicksElapsed);
            }
        }
    }

    private static void broadcastCurrentState(MinecraftServer server) {
        BattleNetwork.broadcastState(server, currentPhase, phaseTicksRemaining, phaseTicksElapsed);
    }

    private static boolean shouldReturnToWaiting(MinecraftServer server) {
        return (currentPhase == BattlePhase.START || currentPhase == BattlePhase.INMUNITY_COUNTDOWN)
            && countReadyPlayers(server) < MIN_PLAYERS_TO_START;
    }

    private static void advancePhase(MinecraftServer server) {
        if (currentPhase == null) {
            startBattle(server);
            return;
        }

        switch (currentPhase) {
            case WAIT_FOR_PLAYERS -> enterPhase(server, BattlePhase.COUNTDOWN);
            case COUNTDOWN -> enterPhase(server, BattlePhase.START);
            case START -> enterPhase(server, BattlePhase.INMUNITY_COUNTDOWN);
            case INMUNITY_COUNTDOWN -> enterPhase(server, BattlePhase.BATTLE);
            case BATTLE -> enterPhase(server, BattlePhase.SHOWDOWN);
            case SHOWDOWN -> resolveWinnerAtShowdownTimeout(server);
            case WINNER_ANOUNCEMENT -> enterPhase(server, BattlePhase.FINISH);
            case FINISH -> stopEngineMinigame(server);
        }
    }

    private static void enterPhase(MinecraftServer server, BattlePhase phase) {
        currentPhase = phase;
        phaseTicksRemaining = phase.durationTicks();
        phaseTicksElapsed = 0;
        BattleNetwork.broadcastState(server, phase, phaseTicksRemaining, phaseTicksElapsed);
        applyNametagVisibility(server, phase);

        if (phase == BattlePhase.WAIT_FOR_PLAYERS || phase == BattlePhase.COUNTDOWN) {
            stageWaitingPlayers(server);
        }

        if (phase == BattlePhase.START) {
            restoreAllAlivePlayers(server);
            ServerLevel level = resolveBattleLevel(server);
            if (level != null) {
                BattleChestLoot.fillInitial(server, level);
            }
        }
        if (phase == BattlePhase.WINNER_ANOUNCEMENT) {
            enforceWinnerCamera(server);
            ServerPlayer winner = winnerId == null ? null : server.getPlayerList().getPlayer(winnerId);
            if (winner != null) {
                server.getPlayerList().broadcastSystemMessage(
                    net.minecraft.network.chat.Component.literal("Ganador: " + winner.getGameProfile().name()), false
                );
            }
        }
    }

    private static void stageWaitingPlayers(MinecraftServer server) {
        ServerLevel level = resolveBattleLevel(server);
        if (level == null) {
            return;
        }

        List<ServerPlayer> players = getReadyPlayers(server);
        List<Vec3> points = resolveCentralPoints();
        Set<Integer> usedSpawns = new HashSet<>();

        for (ServerPlayer player : players) {
            BattlePlayerState state = PLAYER_STATES.get(player.getUUID());
            if (state != null && (state.spawnIndex < 0
                || state.spawnIndex >= points.size()
                || !usedSpawns.add(state.spawnIndex))) {
                state.spawnIndex = -1;
                state.positionedForRound = false;
            }
        }

        for (ServerPlayer player : players) {
            BattlePlayerState state = PLAYER_STATES.get(player.getUUID());
            if (state == null || state.spawnIndex < 0) {
                int freeIndex = findFreeSpawnIndex(usedSpawns, points.size());
                if (freeIndex < 0) {
                    continue;
                }
                if (state == null) {
                    state = new BattlePlayerState();
                    state.originalGameMode = player.gameMode.getGameModeForPlayer();
                    PLAYER_STATES.put(player.getUUID(), state);
                }
                state.spawnIndex = freeIndex;
                state.positionedForRound = false;
                usedSpawns.add(freeIndex);
                BattleNetwork.sendState(player, currentPhase, phaseTicksRemaining, phaseTicksElapsed);
            }


            if (!state.positionedForRound) {
                resetPlayerForRound(player, points.get(state.spawnIndex));
                state.positionedForRound = true;
                player.lookAt(
                        EntityAnchorArgument.Anchor.EYES,
                        START_LOOK
                );
            }
        }
    }

    private static int findFreeSpawnIndex(Set<Integer> usedSpawns, int pointCount) {
        for (int i = 0; i < pointCount; i++) {
            if (!usedSpawns.contains(i)) {
                return i;
            }
        }

        return -1;
    }

    private static void restoreAllAlivePlayers(MinecraftServer server) {
        ServerLevel level = resolveBattleLevel(server);
        if (level == null) {
            return;
        }

        List<Vec3> points = resolveCentralPoints();
        for (ServerPlayer player : getReadyPlayers(server)) {
            BattlePlayerState state = PLAYER_STATES.get(player.getUUID());
            if (state == null || state.lives <= 0) {
                continue;
            }

            int spawnIndex = Math.min(Math.max(state.spawnIndex, 0), points.size() - 1);
            resetPlayerForRound(player, points.get(spawnIndex));
        }
    }

    private static void handleBattleDeath(ServerPlayer player, BattlePlayerState state, DamageSource source) {
        ServerLevel level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.MINIGAMES_BATTLE_DEATH.get(), SoundSource.MASTER, 1.0F, 1.0F);

        ServerPlayer killer = resolveKiller(source);
        if (killer != null && killer != player) {
            BattlePlayerState killerState = PLAYER_STATES.get(killer.getUUID());
            if (killerState != null) {
                killerState.kills++;
            }
        }

        state.lives = Math.max(0, state.lives - 1);
    }

    private static Vec3 resolveRespawnPoint(ServerPlayer player) {
        List<Vec3> points = new java.util.ArrayList<>(resolveCentralPoints());
        MapPoints.INSTANCE.getPoints(PointGroup.SPAWN_SECONDARY).stream()
            .map(point -> new Vec3(point.X() + 0.5D, point.Y(), point.Z() + 0.5D))
            .forEach(points::add);
        return points.isEmpty() ? player.position() : points.get(player.level().getRandom().nextInt(points.size()));
    }

    private static void resetPlayerForRound(ServerPlayer player, Vec3 point) {
        player.setGameMode(GameType.SURVIVAL);
        player.teleportTo(point.x, point.y, point.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.setHealth(player.getMaxHealth());
        player.clearFire();
        player.removeAllEffects();
        player.invulnerableTime = 40;

        FoodData foodData = player.getFoodData();
        foodData.setFoodLevel(20);
        foodData.setSaturation(5.0F);
    }

    private static boolean tryResolveWinnerFromAlivePlayers(MinecraftServer server) {
        if (currentPhase == BattlePhase.WAIT_FOR_PLAYERS || currentPhase == BattlePhase.COUNTDOWN) {
            return false;
        }

        List<ServerPlayer> alivePlayers = getAlivePlayers(server);
        if (alivePlayers.size() > 1) {
            return false;
        }

        if (alivePlayers.size() == 1) {
            winnerId = alivePlayers.getFirst().getUUID();
        } else {
            winnerId = resolveBestPlayer(server);
        }

        if (currentPhase != BattlePhase.WINNER_ANOUNCEMENT && currentPhase != BattlePhase.FINISH) {
            enterPhase(server, BattlePhase.WINNER_ANOUNCEMENT);
        }
        return true;
    }

    private static void resolveWinnerAtShowdownTimeout(MinecraftServer server) {
        winnerId = resolveBestPlayer(server);
        enterPhase(server, BattlePhase.WINNER_ANOUNCEMENT);
    }

    @Nullable
    private static UUID resolveBestPlayer(MinecraftServer server) {
        return getBattleParticipants(server).stream()
            .map(player -> Map.entry(player.getUUID(), PLAYER_STATES.get(player.getUUID())))
            .filter(entry -> entry.getValue() != null)
            .max(Comparator
                .comparingInt((Map.Entry<UUID, BattlePlayerState> entry) -> entry.getValue().lives)
                .thenComparingInt(entry -> entry.getValue().kills)
                .thenComparing(entry -> server.getPlayerList().getPlayer(entry.getKey()) != null
                    ? server.getPlayerList().getPlayer(entry.getKey()).getGameProfile().name()
                    : entry.getKey().toString()))
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    private static void cleanupInactiveBattle(MinecraftServer server) {
        if (currentPhase == null && PLAYER_STATES.isEmpty()) {
            return;
        }

        restoreBattlePlayersToDefault(server);
        removeBattleNametagTeam(server);
        resetSession();
        BattleNetwork.broadcastState(server, null, 0, 0);
    }

    private static void restoreBattlePlayersToDefault(MinecraftServer server) {
        for (Map.Entry<UUID, BattlePlayerState> entry : PLAYER_STATES.entrySet()) {
            if (server.getPlayerList().getPlayer(entry.getKey()) == null && entry.getValue().originalGameMode != null) {
                PENDING_GAME_MODE_RESTORE.put(entry.getKey(), entry.getValue().originalGameMode);
            }
        }
        for (ServerPlayer player : getBattleParticipants(server)) {
            player.setCamera(player);
            BattlePlayerState state = PLAYER_STATES.get(player.getUUID());
            if (state != null && state.originalGameMode != null) {
                player.setGameMode(state.originalGameMode);
            }
        }
    }

    private static void enforceWinnerCamera(MinecraftServer server) {
        ServerPlayer winner = winnerId == null ? null : server.getPlayerList().getPlayer(winnerId);
        if (winner == null) {
            return;
        }
        if (winner.isSpectator()) {
            winner.setGameMode(GameType.SURVIVAL);
        }
        for (ServerPlayer player : getBattleParticipants(server)) {
            if (player == winner) {
                continue;
            }
            if (!player.isSpectator()) {
                player.setGameMode(GameType.SPECTATOR);
            }
            if (player.getCamera() != winner) {
                player.setCamera(winner);
            } else {
                player.connection.send(new ClientboundSetCameraPacket(winner));
            }
        }
    }

    private static void applyNametagVisibility(MinecraftServer server, BattlePhase phase) {
        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(BATTLE_NAMETAG_TEAM);
        if (team == null) {
            team = scoreboard.addPlayerTeam(BATTLE_NAMETAG_TEAM);
        }

        team.setNameTagVisibility(phase == BattlePhase.SHOWDOWN || phase == BattlePhase.FINISH ? Team.Visibility.ALWAYS : Team.Visibility.NEVER);
        for (ServerPlayer player : getBattleParticipants(server)) {
            scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
        }
    }

    private static void removeBattleNametagTeam(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(BATTLE_NAMETAG_TEAM);
        if (team == null) {
            return;
        }

        for (String playerName : List.copyOf(team.getPlayers())) {
            scoreboard.removePlayerFromTeam(playerName, team);
        }
        scoreboard.removePlayerTeam(team);
    }

    private static boolean isTrackedParticipant(UUID playerId) {
        return PLAYER_STATES.containsKey(playerId);
    }

    private static int countReadyPlayers(MinecraftServer server) {
        return (int) getReadyPlayers(server).stream()
            .filter(player -> {
                BattlePlayerState state = PLAYER_STATES.get(player.getUUID());
                return state != null && state.spawnIndex >= 0;
            })
            .count();
    }

    private static List<ServerPlayer> getReadyPlayers(MinecraftServer server) {
        ServerLevel level = resolveBattleLevel(server);
        if (level == null) {
            return List.of();
        }

        return server.getPlayerList().getPlayers().stream()
            .filter(player -> player.level() == level)
            .filter(player -> !player.isSpectator())
            .sorted(Comparator.comparing(player -> player.getGameProfile().name()))
            .toList();
    }

    private static List<ServerPlayer> getAlivePlayers(MinecraftServer server) {
        return getReadyPlayers(server).stream()
            .filter(player -> {
                BattlePlayerState state = PLAYER_STATES.get(player.getUUID());
                return state != null && state.lives > 0;
            })
            .toList();
    }

    private static List<ServerPlayer> getBattleParticipants(MinecraftServer server) {
        Collection<ServerPlayer> onlinePlayers = server.getPlayerList().getPlayers();
        return PLAYER_STATES.keySet().stream()
            .map(uuid -> onlinePlayers.stream().filter(player -> player.getUUID().equals(uuid)).findFirst().orElse(null))
            .filter(Objects::nonNull)
            .toList();
    }

    @Nullable
    private static ServerPlayer resolveKiller(DamageSource source) {
        Entity attacker = source.getEntity();
        return attacker instanceof ServerPlayer player ? player : null;
    }

    @Nullable
    private static ServerLevel resolveBattleLevel(MinecraftServer server) {
        // Sistema Viejo de dimension desactivado temporalmente
        //ServerLevel minigameLevel = server.getLevel(MinigameDimension.MINIGAME_KEY);
        //if (minigameLevel != null && !minigameLevel.players().isEmpty()) {
        //    return minigameLevel;
        //}

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isSpectator()) {
                return player.level();
            }
        }

        return server.overworld();
    }

    private static List<Vec3> resolveCentralPoints() {
        return MapPoints.INSTANCE.getPoints(PointGroup.SPAWN_CENTRAL).stream()
            .map(point -> new Vec3(point.X() + 0.5D, point.Y(), point.Z() + 0.5D))
            .toList();
    }

    private static final class BattlePlayerState {
        private int lives = STARTING_LIVES;
        private int kills = 0;
        private int spawnIndex = -1;
        private boolean positionedForRound = false;
        @Nullable
        private GameType originalGameMode;
    }
}
