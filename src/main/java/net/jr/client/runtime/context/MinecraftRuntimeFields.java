package net.jr.client.runtime.context;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;

public final class MinecraftRuntimeFields {
    private MinecraftRuntimeFields() {
    }

    public static LocalPlayer player(Minecraft minecraft) {
        return LocalClientAcces.player(minecraft);
    }

    public static ClientLevel level(Minecraft minecraft) {
        return LocalClientAcces.level(minecraft);
    }

    public static MultiPlayerGameMode gameMode(Minecraft minecraft) {
        return LocalClientAcces.gameMode(minecraft);
    }

    public static Entity cameraEntity(Minecraft minecraft) {
        return LocalClientAcces.cameraEntity(minecraft);
    }

    public static Entity crosshairPickEntity(Minecraft minecraft) {
        return LocalClientAcces.crosshairPickEntity(minecraft);
    }

    public static HitResult hitResult(Minecraft minecraft) {
        return LocalClientAcces.hitResult(minecraft);
    }

    @Nullable
    public static Screen screen(Minecraft minecraft) {
        return LocalClientAcces.screen(minecraft);
    }

    @Nullable
    public static Screen screen(Gui gui) {
        return LocalClientAcces.screen(gui);
    }

    public static boolean noRender(Minecraft minecraft) {
        return LocalClientAcces.noRender(minecraft);
    }
}
