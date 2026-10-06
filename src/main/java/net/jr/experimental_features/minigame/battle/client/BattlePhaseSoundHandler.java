package net.jr.experimental_features.minigame.battle.client;


import net.jr.Java_reforged;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.ClientInstance;
import net.jr.client.runtime.client.ReadinessPolicy;
import net.jr.experimental_features.minigame.battle.BattlePhase;
import net.jr.experimental_features.minigame.client.BattleClientState;
import net.jr.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = Java_reforged.MODID, value = Dist.CLIENT)
public final class BattlePhaseSoundHandler {

    @Nullable
    private static BattlePhase lastPhase;
    private static int lastRemainingSeconds = -1;

    private BattlePhaseSoundHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!hasAnyActivePlayer()) {
            resetTracking();
            return;
        }

        BattlePhase phase = BattleClientState.phase();
        if (phase == null) {
            resetTracking();
            return;
        }

        int remainingSeconds = BattleClientState.remainingSecondsCeil();
        if (phase != lastPhase) {
            handlePhaseEntered(minecraft, phase);
            lastRemainingSeconds = -1;
        }

        if (remainingSeconds != lastRemainingSeconds) {
            handleRemainingSecondChanged(minecraft, phase, remainingSeconds);
            lastRemainingSeconds = remainingSeconds;
        }

        lastPhase = phase;
    }

    private static void handlePhaseEntered(Minecraft minecraft, BattlePhase phase) {
        if (phase == BattlePhase.START) {
            play(minecraft, ModSounds.MINIGAMES_BATTLE_GAMESTART.get());
        } else if (phase == BattlePhase.SHOWDOWN) {
            play(minecraft, ModSounds.MINIGAMES_BATTLE_SHOWDOWN.get());
        } else if (phase == BattlePhase.BATTLE) {
            play(minecraft, ModSounds.MINIGAMES_BATTLE_GRACEZERO.get());
        } else if (phase == BattlePhase.WINNER_ANOUNCEMENT) {
            play(minecraft, ModSounds.MINIGAMES_BATTLE_CELEBRATION2.get());
        }
    }

    private static void handleRemainingSecondChanged(Minecraft minecraft, BattlePhase phase, int remainingSeconds) {
        if (remainingSeconds <= 0) {
            return;
        }

        if (phase == BattlePhase.COUNTDOWN && remainingSeconds <= 5) {
            play(minecraft, ModSounds.MINIGAMES_LOBBY_STARTCD.get());
            return;
        }

        if (phase == BattlePhase.INMUNITY_COUNTDOWN && remainingSeconds <= 5) {
            play(minecraft, ModSounds.MINIGAMES_BATTLE_GRACE.get());
            return;
        }

        if (phase == BattlePhase.SHOWDOWN && remainingSeconds <= 10) {
            if (remainingSeconds >= 5) {
                play(minecraft, ModSounds.MINIGAMES_LOBBY_LOBBY.get());
            } else if (remainingSeconds >= 2) {
                play(minecraft, ModSounds.MINIGAMES_LOBBY_LOBBYACCENT.get());
            } else {
                play(minecraft, ModSounds.MINIGAMES_LOBBY_LOBBYZERO.get());
            }
        }
    }

    private static void play(Minecraft minecraft, SoundEvent sound) {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F, 1.0F));
    }

    private static void resetTracking() {
        lastPhase = null;
        lastRemainingSeconds = -1;
    }

    private static boolean hasAnyActivePlayer() {
        for (ClientInstance client : ClientRuntime.INSTANCE.clients().all()) {
            if (ReadinessPolicy.worldReady(client)) {
                return true;
            }
        }
        return false;
    }
}
