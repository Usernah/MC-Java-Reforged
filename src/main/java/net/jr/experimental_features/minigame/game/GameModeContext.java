package net.jr.experimental_features.minigame.game;

import java.util.Objects;


public final class GameModeContext {

    private static volatile GameModeState currentMode = GameModeState.VANILLA;

    private GameModeContext() {
    }

    public static GameModeState getCurrentMode() {
        return currentMode;
    }

    public static void setCurrentMode(GameModeState mode) {
        currentMode = Objects.requireNonNull(mode, "mode");
    }

    public static void reset() {
        currentMode = GameModeState.VANILLA;
    }

    public static boolean isVanilla() {
        return currentMode.isVanilla();
    }

    public static boolean isBattle() {
        return currentMode.isBattle();
    }
}
