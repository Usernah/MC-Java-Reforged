package net.jr.experimental_features.minigame.game;

/**
 * Estado general del juego actual.
 * Las fases internas de cada minijuego se manejan aparte.
 */
public enum GameModeState {
    VANILLA,
    BATTLE;

    public boolean isVanilla() {
        return this == VANILLA;
    }

    public boolean isBattle() {
        return this == BATTLE;
    }
}
