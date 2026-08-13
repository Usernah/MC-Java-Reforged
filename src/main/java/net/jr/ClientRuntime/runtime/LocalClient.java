package net.jr.ClientRuntime.runtime;

import javax.annotation.Nullable;
import net.jr.ClientRuntime.slot.PlayerSlot;
import net.jr.ClientRuntime.state.GameplayState;
import net.jr.ClientRuntime.state.InputState;
import net.jr.ClientRuntime.state.OptionsState;
import net.jr.ClientRuntime.state.RenderState;
import net.jr.ClientRuntime.state.ScreenState;
import net.jr.ClientRuntime.viewport.ViewportArea;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.phys.HitResult;

/**
 * The local Minecraft client as seen by one runtime slot.
 *
 * <p>New UI/input/render code should receive this object from the high-level
 * scope and ask it for player, screen, cursor, and state. That keeps the
 * "which slot am I?" decision at the entry point instead of spreading it
 * through every subsystem.</p>
 */
public final class LocalClient {
    private final PlayerSlot slot;
    private final ClientSlotContext context;
    private final LocalClientCursor cursor;

    LocalClient(PlayerSlot slot) {
        this.slot = slot;
        this.context = ClientSlotContext.of(slot);
        this.cursor = new LocalClientCursor(slot.id());
    }

    public int slotId() {
        return this.slot.id();
    }

    public boolean connected() {
        return this.slot.connected();
    }

    public boolean visible() {
        return this.slot.visible();
    }

    public boolean drawable() {
        return this.slot.drawable();
    }

    public boolean worldReady() {
        return this.context.worldReady();
    }

    public boolean gameplayReady() {
        return this.context.gameplayReady();
    }

    public RenderState render() {
        return this.slot.renderState();
    }

    public GameplayState gameplay() {
        return this.slot.gameplayState();
    }

    public ScreenState ui() {
        return this.slot.screenState();
    }

    public InputState input() {
        return this.slot.inputState();
    }

    public OptionsState options() {
        return this.slot.optionsState();
    }

    public LocalClientCursor cursor() {
        return this.cursor;
    }

    public boolean hasViewport() {
        return this.slot.hasViewport();
    }

    @Nullable
    public ViewportArea viewportOrNull() {
        return this.slot.hasViewport() ? this.slot.viewport() : null;
    }

    public ViewportArea viewport() {
        return this.slot.viewport();
    }

    @Nullable
    public ClientLevel level() {
        return this.slot.renderState().level();
    }

    public Camera camera() {
        return this.slot.renderState().camera();
    }

    @Nullable
    public Frustum frustum() {
        return this.slot.renderState().frustum();
    }

    @Nullable
    public Entity cameraEntity() {
        return this.slot.renderState().cameraEntity();
    }

    @Nullable
    public Entity crosshairPickEntity() {
        return this.slot.renderState().crosshairPickEntity();
    }

    @Nullable
    public HitResult hitResult() {
        return this.slot.renderState().hitResult();
    }

    @Nullable
    public LocalPlayer player() {
        return this.slot.gameplayState().player();
    }

    @Nullable
    public MultiPlayerGameMode gameMode() {
        return this.slot.gameplayState().gameMode();
    }

    @Nullable
    public Screen screen() {
        return this.slot.screenState().screen();
    }

    @Nullable
    public AbstractContainerMenu menu() {
        AbstractContainerMenu menu = this.slot.screenState().menu();
        if (menu != null) {
            return menu;
        }
        LocalPlayer player = this.player();
        return player == null ? null : player.containerMenu;
    }

    PlayerSlot rawSlot() {
        return this.slot;
    }

    ClientSlotContext context() {
        return this.context;
    }
}
