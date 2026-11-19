package net.jr.experimental_features.minigame.game;


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
