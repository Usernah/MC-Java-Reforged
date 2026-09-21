package net.jr.experimental_features.minigame.battle;

public enum BattlePhase {
    WAIT_FOR_PLAYERS(5),
    COUNTDOWN(10),
    START(1),
    INMUNITY_COUNTDOWN(15),
    BATTLE(300),
    SHOWDOWN(60),
    WINNER_ANOUNCEMENT(5),
    FINISH(5);

    private final int durationSeconds;

    BattlePhase(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public int durationSeconds() {
        return this.durationSeconds;
    }

    public int durationTicks() {
        return this.durationSeconds * 20;
    }

    public boolean blocksHorizontalMovement() {
        return this == WAIT_FOR_PLAYERS || this == COUNTDOWN;
    }
}
