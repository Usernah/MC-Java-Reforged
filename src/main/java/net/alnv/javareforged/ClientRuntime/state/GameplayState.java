package net.alnv.javareforged.ClientRuntime.state;

import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;

public final class GameplayState {
    @Nullable
    private LocalPlayer player;
    @Nullable
    private MultiPlayerGameMode gameMode;
    private int rightClickDelay;
    private int missTime;

    public void setPlayer(@Nullable LocalPlayer player) {
        this.player = player;
    }

    public void bindPlayer(@Nullable LocalPlayer player) {
        this.setPlayer(player);
    }

    public void setGameMode(@Nullable MultiPlayerGameMode gameMode) {
        this.gameMode = gameMode;
    }

    public void bindGameMode(@Nullable MultiPlayerGameMode gameMode) {
        this.setGameMode(gameMode);
    }

    public void setRightClickDelay(int rightClickDelay) {
        this.rightClickDelay = rightClickDelay;
    }

    public void setMissTime(int missTime) {
        this.missTime = missTime;
    }

    @Nullable
    public LocalPlayer player() {
        return this.player;
    }

    @Nullable
    public MultiPlayerGameMode gameMode() {
        return this.gameMode;
    }

    public int rightClickDelay() {
        return this.rightClickDelay;
    }

    public int missTime() {
        return this.missTime;
    }

    public void clearWorldBinding() {
        this.player = null;
        this.gameMode = null;
        this.rightClickDelay = 0;
        this.missTime = 0;
    }
}
