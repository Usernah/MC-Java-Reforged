package net.alnv.javareforged.ClientRuntime.runtime;

import net.alnv.javareforged.ClientRuntime.slot.PlayerSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;

public final class RawMinecraftStateScope implements AutoCloseable {
    private final Minecraft minecraft;
    private final ClientLevel previousLevel;
    private final LocalPlayer previousPlayer;
    private final MultiPlayerGameMode previousGameMode;
    private final Entity previousCameraEntity;
    private final Entity previousCrosshairPickEntity;
    private final HitResult previousHitResult;
    private final boolean previousNoRender;
    private boolean closed;

    private RawMinecraftStateScope(Minecraft minecraft) {
        this.minecraft = minecraft;
        this.previousLevel = minecraft.level;
        this.previousPlayer = minecraft.player;
        this.previousGameMode = minecraft.gameMode;
        this.previousCameraEntity = minecraft.cameraEntity;
        this.previousCrosshairPickEntity = minecraft.crosshairPickEntity;
        this.previousHitResult = minecraft.hitResult;
        this.previousNoRender = minecraft.noRender;
    }

    public static RawMinecraftStateScope bind(Minecraft minecraft, PlayerSlot slot) {
        RawMinecraftStateScope scope = new RawMinecraftStateScope(minecraft);
        Entity cameraEntity = liveEntityOrNull(slot.renderState().cameraEntity());
        LocalPlayer player = livePlayerOrNull(slot.gameplayState().player());
        minecraft.level = slot.renderState().level();
        minecraft.player = player;
        minecraft.gameMode = slot.gameplayState().gameMode();
        minecraft.cameraEntity = cameraEntity != null ? cameraEntity : player;
        minecraft.crosshairPickEntity = slot.renderState().crosshairPickEntity();
        minecraft.hitResult = slot.renderState().hitResult();
        minecraft.noRender = slot.renderState().noRender();
        TerrainDebug.recordRawStateCheck("rawState.bind", slot.id(), minecraft);
        return scope;
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        this.minecraft.level = this.previousLevel;
        this.minecraft.player = this.previousPlayer;
        this.minecraft.gameMode = this.previousGameMode;
        this.minecraft.cameraEntity = this.previousCameraEntity;
        this.minecraft.crosshairPickEntity = this.previousCrosshairPickEntity;
        this.minecraft.hitResult = this.previousHitResult;
        this.minecraft.noRender = this.previousNoRender;
    }

    private static Entity liveEntityOrNull(Entity entity) {
        return entity != null && !entity.isRemoved() ? entity : null;
    }

    private static LocalPlayer livePlayerOrNull(LocalPlayer player) {
        return player != null && !player.isRemoved() ? player : null;
    }
}
