package net.jr.ClientRuntime.runtime;

import java.util.Objects;
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
import net.minecraft.world.phys.HitResult;

/**
 * Read-only facade for the current local player slot.
 *
 * <p>This is the single object new UI/input/render code should receive instead
 * of asking ActiveSlot, RuntimeFields, screen state, and raw Minecraft globals
 * separately.</p>
 */
public final class ClientSlotContext {
    private final PlayerSlot slot;

    ClientSlotContext(PlayerSlot slot) {
        this.slot = slot;
    }

    public static ClientSlotContext of(int slotId) {
        return of(LocalPlayers.INSTANCE.slots().slot(slotId));
    }

    public static ClientSlotContext of(PlayerSlot slot) {
        return new ClientSlotContext(Objects.requireNonNull(slot, "slot"));
    }

    public PlayerSlot rawSlot() {
        return this.slot;
    }

    public int id() {
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

    public RenderState renderState() {
        return this.slot.renderState();
    }

    public GameplayState gameplayState() {
        return this.slot.gameplayState();
    }

    public ScreenState screenState() {
        return this.slot.screenState();
    }

    public InputState inputState() {
        return this.slot.inputState();
    }

    public OptionsState optionsState() {
        return this.slot.optionsState();
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

    public boolean worldReady() {
        return LocalPlayers.INSTANCE.slotWorldReady(this.slot);
    }

    public boolean gameplayReady() {
        return LocalPlayers.INSTANCE.slotGameplayReady(this.slot);
    }
}
