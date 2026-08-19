package net.jr.client.runtime.bridge;

import javax.annotation.Nullable;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;

public final class MinecraftClientAccess {
    private MinecraftClientAccess() {
    }

    @Nullable
    public static ClientLevel level(Minecraft minecraft) {
        return slot().renderState().level();
    }

    public static void setLevel(Minecraft minecraft, @Nullable ClientLevel level) {
        slot().renderState().setLevel(level);
    }

    @Nullable
    public static LocalPlayer player(Minecraft minecraft) {
        return slot().gameplayState().player();
    }

    public static void setPlayer(Minecraft minecraft, @Nullable LocalPlayer player) {
        slot().gameplayState().setPlayer(player);
    }

    @Nullable
    public static MultiPlayerGameMode gameMode(Minecraft minecraft) {
        return slot().gameplayState().gameMode();
    }

    public static void setGameMode(Minecraft minecraft, @Nullable MultiPlayerGameMode gameMode) {
        slot().gameplayState().setGameMode(gameMode);
    }

    @Nullable
    public static Entity crosshairPickEntity(Minecraft minecraft) {
        return slot().renderState().crosshairPickEntity();
    }

    public static void setCrosshairPickEntity(Minecraft minecraft, @Nullable Entity entity) {
        slot().renderState().setCrosshairPickEntity(entity);
    }

    @Nullable
    public static HitResult hitResult(Minecraft minecraft) {
        return slot().renderState().hitResult();
    }

    public static void setHitResult(Minecraft minecraft, @Nullable HitResult hitResult) {
        slot().renderState().setHitResult(hitResult);
    }

    public static boolean noRender(Minecraft minecraft) {
        return slot().renderState().noRender();
    }

    @Nullable
    public static Entity cameraEntity(Minecraft minecraft) {
        Entity cameraEntity = liveEntityOrNull(slot().renderState().cameraEntity());
        return cameraEntity != null ? cameraEntity : liveEntityOrNull(slot().gameplayState().player());
    }

    public static void setCameraEntity(Minecraft minecraft, @Nullable Entity entity) {
        slot().renderState().setCameraEntity(entity);
        minecraft.setCameraEntity(entity);
    }

    private static LocalClientSlot slot() {
        Integer slotId = SlotScope.idOrNull();
        return ClientRuntime.INSTANCE.slots().slot(slotId != null ? slotId : 0);
    }

    @Nullable
    private static Entity liveEntityOrNull(@Nullable Entity entity) {
        return entity != null && !entity.isRemoved() ? entity : null;
    }
}
