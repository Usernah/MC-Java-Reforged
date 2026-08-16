package net.jr.client.runtime.context;

import net.jr.client.runtime.slot.LocalClientSlot;
import net.jr.mixin.runtime.GuiRawScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;

public final class MinecraftClientStateScope implements AutoCloseable {
    private final Minecraft minecraft;
    private final ClientLevel previousLevel;
    private final LocalPlayer previousPlayer;
    private final MultiPlayerGameMode previousGameMode;
    private final Entity previousCrosshairPickEntity;
    private final HitResult previousHitResult;
    private final Screen previousScreen;
    private boolean closed;

    private MinecraftClientStateScope(Minecraft minecraft) {
        this.minecraft = minecraft;
        this.previousLevel = minecraft.level;
        this.previousPlayer = minecraft.player;
        this.previousGameMode = minecraft.gameMode;
        this.previousCrosshairPickEntity = minecraft.crosshairPickEntity;
        this.previousHitResult = minecraft.hitResult;
        this.previousScreen = ((GuiRawScreenAccessor)minecraft.gui).splitTest$getRawScreen();
    }

    public static MinecraftClientStateScope bind(Minecraft minecraft, LocalClientSlot slot) {
        MinecraftClientStateScope scope = new MinecraftClientStateScope(minecraft);
        LocalPlayer player = livePlayerOrNull(slot.gameplayState().player());
        minecraft.level = slot.renderState().level();
        minecraft.player = player;
        minecraft.gameMode = slot.gameplayState().gameMode();
        minecraft.crosshairPickEntity = slot.renderState().crosshairPickEntity();
        minecraft.hitResult = slot.renderState().hitResult();
        ((GuiRawScreenAccessor)minecraft.gui).splitTest$setRawScreen(slot.screenState().screen());
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
        this.minecraft.crosshairPickEntity = this.previousCrosshairPickEntity;
        this.minecraft.hitResult = this.previousHitResult;
        ((GuiRawScreenAccessor)this.minecraft.gui).splitTest$setRawScreen(this.previousScreen);
    }

    private static LocalPlayer livePlayerOrNull(LocalPlayer player) {
        return player != null && !player.isRemoved() ? player : null;
    }
}
