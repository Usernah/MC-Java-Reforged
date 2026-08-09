package net.jr.ClientRuntime.runtime;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;

public final class RuntimeFields {
    private RuntimeFields() {
    }

    public static LocalPlayer player(Minecraft minecraft) {
        return Client.player(minecraft);
    }

    public static ClientLevel level(Minecraft minecraft) {
        return Client.level(minecraft);
    }

    public static MultiPlayerGameMode gameMode(Minecraft minecraft) {
        return Client.gameMode(minecraft);
    }

    public static Entity cameraEntity(Minecraft minecraft) {
        return Client.cameraEntity(minecraft);
    }

    public static Entity crosshairPickEntity(Minecraft minecraft) {
        return Client.crosshairPickEntity(minecraft);
    }

    public static HitResult hitResult(Minecraft minecraft) {
        return Client.hitResult(minecraft);
    }

    @Nullable
    public static Screen screen(Minecraft minecraft) {
        return Client.screen(minecraft);
    }

    @Nullable
    public static Screen screen(Gui gui) {
        return Client.screen(gui);
    }

    public static boolean noRender(Minecraft minecraft) {
        return Client.noRender(minecraft);
    }
}
