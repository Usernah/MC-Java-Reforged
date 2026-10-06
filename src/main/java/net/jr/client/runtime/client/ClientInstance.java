package net.jr.client.runtime.client;

import javax.annotation.Nullable;
import net.jr.client.runtime.input.SlotCursorView;
import net.jr.api.client.input.ISlotInput;
import net.jr.client.runtime.slot.Slots;
import net.jr.client.runtime.state.*;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.phys.HitResult;

public final class ClientInstance {
    private final Slots slot;

    ClientInstance(Slots slot) {
        this.slot = slot;
    }

    public int slotId() {
        return this.slot.id();
    }

    public ClientRenderState render() {
        return this.slot.renderState();
    }

    public ChatState chat() {
        return this.slot.chatState();
    }

    public ToastState toasts() {
        return this.slot.toastState();
    }

    public MinimapState minimap() {
        return this.render().minimap();
    }

    public GameplayState gameplay() {
        return this.slot.gameplayState();
    }

    public ScreenState ui() {
        return this.slot.screenState();
    }

    public ISlotInput input() {
        return this.slot.input();
    }

    public InputState inputState() {
        return this.slot.inputState();
    }

    public OptionsState options() {
        return this.slot.optionsState();
    }

    public SlotCursorView cursor() {
        return this.slot.cursor();
    }

    @Nullable
    public ClientLevel level() {
        return this.render().level();
    }

    public Camera camera() {
        return this.render().camera();
    }

    @Nullable
    public Frustum frustum() {
        return this.render().frustum();
    }

    @Nullable
    public Entity cameraEntity() {
        return this.render().cameraEntity();
    }

    @Nullable
    public Entity crosshairPickEntity() {
        return this.render().crosshairPickEntity();
    }

    @Nullable
    public HitResult hitResult() {
        return this.render().hitResult();
    }

    @Nullable
    public LocalPlayer player() {
        return this.gameplay().player();
    }

    @Nullable
    public MultiPlayerGameMode gameMode() {
        return this.gameplay().gameMode();
    }

    @Nullable
    public Screen screen() {
        return this.ui().screen();
    }

    @Nullable
    public AbstractContainerMenu menu() {
        AbstractContainerMenu menu = this.ui().menu();
        if (menu != null) {
            return menu;
        }
        LocalPlayer player = this.player();
        return player == null ? null : player.containerMenu;
    }

    public Slots slot() {
        return this.slot;
    }

    public boolean isPrimary() {
        return this.slot.isPrimary();
    }

    public boolean isSecondary() {
        return this.slot.isSecondary();
    }
}
