package net.jr.client.ui.hint;

import javax.annotation.Nullable;
import net.jr.client.input.mode.InputMode;
import net.jr.client.input.InputApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.HitResult;

public record ControlHintContext(
    Minecraft minecraft,
    @Nullable Screen screen,
    @Nullable LocalPlayer player,
    @Nullable MultiPlayerGameMode gameMode,
    @Nullable HitResult hitResult,
    InputMode inputMode
) {
    public static ControlHintContext hud(Minecraft minecraft) {
        return new ControlHintContext(
            minecraft,
            null,
            minecraft.player,
            minecraft.gameMode,
            minecraft.hitResult,
            InputApi.mode()
        );
    }

    public static ControlHintContext screen(Minecraft minecraft, Screen screen) {
        return new ControlHintContext(
            minecraft,
            screen,
            minecraft.player,
            minecraft.gameMode,
            minecraft.hitResult,
            InputApi.mode()
        );
    }

    public boolean isHud() {
        return this.screen == null;
    }

    public boolean isGamepadMode() {
        return this.inputMode == InputMode.GAMEPAD;
    }

    @Nullable
    public AbstractContainerScreen<?> containerScreen() {
        return this.screen instanceof AbstractContainerScreen<?> containerScreen ? containerScreen : null;
    }
}
