package net.jr.client.runtime.state;

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
    private int legacyFlightSprintTicks;
    private boolean legacyPlacementActive;
    private double legacyPlacementX;
    private double legacyPlacementY;
    private double legacyPlacementZ;

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

    public int legacyFlightSprintTicks() {
        return this.legacyFlightSprintTicks;
    }

    public void setLegacyFlightSprintTicks(int ticks) {
        this.legacyFlightSprintTicks = ticks;
    }

    public boolean legacyPlacementActive() {
        return this.legacyPlacementActive;
    }

    public double legacyPlacementX() { return this.legacyPlacementX; }

    public double legacyPlacementY() { return this.legacyPlacementY; }

    public double legacyPlacementZ() { return this.legacyPlacementZ; }

    public void beginLegacyPlacement(double x, double y, double z) {
        this.legacyPlacementActive = true;
        this.legacyPlacementX = x;
        this.legacyPlacementY = y;
        this.legacyPlacementZ = z;
    }

    public void advanceLegacyPlacement(double x, double y, double z) {
        if (!this.legacyPlacementActive) {
            this.beginLegacyPlacement(x, y, z);
            return;
        }
        this.legacyPlacementX += wholeDisplacement(x - this.legacyPlacementX);
        this.legacyPlacementY += wholeDisplacement(y - this.legacyPlacementY);
        this.legacyPlacementZ += wholeDisplacement(z - this.legacyPlacementZ);
    }

    public void resetLegacyPlacement() {
        this.legacyPlacementActive = false;
    }

    public void clearWorldBinding() {
        this.player = null;
        this.gameMode = null;
        this.rightClickDelay = 0;
        this.missTime = 0;
        this.legacyFlightSprintTicks = 0;
        this.resetLegacyPlacement();
    }

    private static int wholeDisplacement(double value) {
        return value < 0.0D ? (int)Math.ceil(value) : (int)Math.floor(value);
    }
}
