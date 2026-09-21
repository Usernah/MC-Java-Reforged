package net.jr.experimental_features.minigame.game.client;

import net.jr.client.sound.music.MusicPlaybackPolicy;
import net.jr.experimental_features.minigame.battle.BattlePhase;
import net.jr.experimental_features.minigame.client.BattleClientState;
import net.jr.registry.ModSounds;
import net.minecraft.sounds.Music;

public final class GameModeMusicController {

    private static final Music BATTLE_MUSIC = new Music(ModSounds.MINIGAMES_MUSIC_BATTLE_VANILLA, 20, Integer.MAX_VALUE, true);

    private GameModeMusicController() {
    }

    public static void resetForWorldExit() {
        MusicPlaybackPolicy.useVanilla();
    }

    public static void syncBattleState() {
        if (!BattleClientState.isBattle()) {
            MusicPlaybackPolicy.useVanilla();
            return;
        }

        BattlePhase phase = BattleClientState.phase();
        if (phase == BattlePhase.START
            || phase == BattlePhase.INMUNITY_COUNTDOWN
            || phase == BattlePhase.BATTLE
            || phase == BattlePhase.SHOWDOWN
            || phase == BattlePhase.WINNER_ANOUNCEMENT
            || phase == BattlePhase.FINISH) {
            MusicPlaybackPolicy.overrideWith(BATTLE_MUSIC);
            return;
        }

        MusicPlaybackPolicy.silence();
    }
}
