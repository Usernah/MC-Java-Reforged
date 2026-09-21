package net.jr.experimental_features.minigame.client;

import javax.annotation.Nullable;
import net.jr.Java_reforged;
import net.jr.experimental_features.minigame.battle.BattlePhase;
import net.jr.experimental_features.minigame.game.client.GameModeMusicController;
import net.jr.experimental_features.minigame.network.BattleNetwork;
import net.jr.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

import java.util.List;

@EventBusSubscriber(modid = Java_reforged.MODID, value = Dist.CLIENT)
public final class BattleClientState {
    private static volatile BattleNetwork.StateSnapshot snapshot = emptySnapshot();
    private static volatile long refillVisibleUntilMs;
    private static long lastRefillPacketMs;

    private BattleClientState() {
    }

    @SubscribeEvent
    public static void registerPayloadHandler(RegisterClientPayloadHandlersEvent event) {
        event.register(BattleNetwork.StateSnapshot.TYPE,
            (received, context) -> context.enqueueWork(() -> {
                snapshot = received;
                GameModeMusicController.syncBattleState();
            }));
        event.register(BattleNetwork.ChestRefill.TYPE,
            (received, context) -> context.enqueueWork(() -> {
                long now = Util.getMillis();
                if (now - lastRefillPacketMs < 200L) {
                    return;
                }
                lastRefillPacketMs = now;
                refillVisibleUntilMs = now + 1_800L;
                Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(ModSounds.MINIGAMES_BATTLE_CHEST2.get(), 1.0F, 1.0F)
                );
            }));
    }

    public static void resetForWorldExit() {
        snapshot = emptySnapshot();
        refillVisibleUntilMs = 0L;
        lastRefillPacketMs = 0L;
        GameModeMusicController.resetForWorldExit();
    }

    @Nullable
    public static BattlePhase phase() {
        int index = snapshot.phaseIndex();
        BattlePhase[] phases = BattlePhase.values();
        return index >= 0 && index < phases.length ? phases[index] : null;
    }

    public static boolean isBattle() {
        return phase() != null;
    }

    public static boolean isMovementLocked() {
        BattlePhase phase = phase();
        return phase != null && phase.blocksHorizontalMovement();
    }

    public static int remainingSecondsCeil() {
        return (snapshot.remainingTicks() + 19) / 20;
    }

    public static int elapsedTicks() {
        return snapshot.elapsedTicks();
    }

    public static boolean showChestRefill() {
        return isBattle() && Util.getMillis() < refillVisibleUntilMs;
    }

    public static long chestRefillStartedAtMs() {
        return lastRefillPacketMs;
    }

    public static boolean isWinner(String playerId) {
        return !snapshot.winnerId().isEmpty() && snapshot.winnerId().equals(playerId);
    }

    public static String winnerName() {
        return snapshot.winnerName();
    }

    public static List<BattleNetwork.PlayerBarState> players() {
        return snapshot.players();
    }

    private static BattleNetwork.StateSnapshot emptySnapshot() {
        return new BattleNetwork.StateSnapshot(-1, 0, 0, "", "", List.of());
    }
}
