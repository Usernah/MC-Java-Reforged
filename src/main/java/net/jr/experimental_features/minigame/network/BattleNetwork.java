package net.jr.experimental_features.minigame.network;

import net.jr.Java_reforged;
import net.jr.experimental_features.minigame.battle.BattlePhase;
import net.jr.experimental_features.minigame.battle.BattleRuntime;
import net.jr.experimental_features.minigame.battle.BattleTakeAllAction;
import net.jr.experimental_features.minigame.game.GameModeContext;
import net.jr.playerdata.PlayerProfileDataManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public final class BattleNetwork {
    private BattleNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1").optional();
        registrar.playToServer(ControlRequest.TYPE, ControlRequest.STREAM_CODEC, BattleNetwork::handleControl);
        registrar.playToServer(TakeAllRequest.TYPE, TakeAllRequest.STREAM_CODEC, BattleNetwork::handleTakeAll);
        registrar.playToClient(StateSnapshot.TYPE, StateSnapshot.STREAM_CODEC);
        registrar.playToClient(ChestRefill.TYPE, ChestRefill.STREAM_CODEC);
    }

    private static void handleControl(ControlRequest request, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            MinecraftServer server = player.level().getServer();
            if (server == null) {
                return;
            }

            if (request.toggle() && GameModeContext.isBattle()) {
                BattleRuntime.stopEngineMinigame(server);
                player.sendOverlayMessage(Component.literal("Battle stopped"));
            } else if (BattleRuntime.startEngineMinigame(server)) {
                player.sendOverlayMessage(Component.literal("Battle started"));
            } else {
                player.sendOverlayMessage(Component.literal("Battle is already running"));
            }
        });
    }

    private static void handleTakeAll(TakeAllRequest request, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                BattleTakeAllAction.takeAll(player, request.containerId());
            }
        });
    }

    public static void sendState(ServerPlayer player, BattlePhase phase, int remainingTicks, int elapsedTicks) {
        if (player.connection.hasChannel(StateSnapshot.TYPE)) {
            PacketDistributor.sendToPlayer(player, snapshot(player.level().getServer(), phase, remainingTicks, elapsedTicks));
        }
    }

    public static void broadcastState(MinecraftServer server, BattlePhase phase, int remainingTicks, int elapsedTicks) {
        StateSnapshot snapshot = snapshot(server, phase, remainingTicks, elapsedTicks);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.connection.hasChannel(StateSnapshot.TYPE)) {
                PacketDistributor.sendToPlayer(player, snapshot);
            }
        }
    }

    public static void broadcastChestRefill(MinecraftServer server) {
        ChestRefill packet = new ChestRefill(1);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.connection.hasChannel(ChestRefill.TYPE)) {
                PacketDistributor.sendToPlayer(player, packet);
            }
        }
    }

    private static StateSnapshot snapshot(MinecraftServer server, BattlePhase phase, int remainingTicks, int elapsedTicks) {
        var winnerId = BattleRuntime.getWinnerId();
        ServerPlayer winner = winnerId == null ? null : server.getPlayerList().getPlayer(winnerId);
        List<PlayerBarState> players = server.getPlayerList().getPlayers().stream()
            .filter(player -> BattleRuntime.isParticipant(player.getUUID()))
            .limit(8)
            .map(player -> {
                int lives = BattleRuntime.getRemainingLives(player.getUUID());
                boolean eliminated = lives <= 0;
                return new PlayerBarState(PlayerProfileDataManager.getLegacyColor(player), eliminated);
            })
            .toList();
        return new StateSnapshot(phase == null ? -1 : phase.ordinal(), remainingTicks, elapsedTicks,
            winnerId == null ? "" : winnerId.toString(),
            winner == null ? "" : winner.getGameProfile().name(), players);
    }

    public record ControlRequest(boolean toggle) implements CustomPacketPayload {
        public static final Type<ControlRequest> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Java_reforged.MODID, "battle_control"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ControlRequest> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ControlRequest::toggle, ControlRequest::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record TakeAllRequest(int containerId) implements CustomPacketPayload {
        public static final Type<TakeAllRequest> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Java_reforged.MODID, "battle_take_all"));
        public static final StreamCodec<RegistryFriendlyByteBuf, TakeAllRequest> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, TakeAllRequest::containerId, TakeAllRequest::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record PlayerBarState(int color, boolean eliminated) {
        public static final StreamCodec<RegistryFriendlyByteBuf, PlayerBarState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.RGB_COLOR, PlayerBarState::color,
            ByteBufCodecs.BOOL, PlayerBarState::eliminated,
            PlayerBarState::new
        );
    }

    public record StateSnapshot(
        int phaseIndex,
        int remainingTicks,
        int elapsedTicks,
        String winnerId,
        String winnerName,
        List<PlayerBarState> players
    ) implements CustomPacketPayload {
        public static final Type<StateSnapshot> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Java_reforged.MODID, "battle_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, StateSnapshot> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, StateSnapshot::phaseIndex,
            ByteBufCodecs.INT, StateSnapshot::remainingTicks,
            ByteBufCodecs.INT, StateSnapshot::elapsedTicks,
            ByteBufCodecs.STRING_UTF8, StateSnapshot::winnerId,
            ByteBufCodecs.STRING_UTF8, StateSnapshot::winnerName,
            PlayerBarState.STREAM_CODEC.apply(ByteBufCodecs.list(8)), StateSnapshot::players,
            StateSnapshot::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ChestRefill(int eventId) implements CustomPacketPayload {
        public static final Type<ChestRefill> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Java_reforged.MODID, "battle_chest_refill"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ChestRefill> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, ChestRefill::eventId, ChestRefill::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
