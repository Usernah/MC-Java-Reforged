package net.jr.client.runtime.state;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.AbstractContainerMenu;

import javax.annotation.Nullable;

public final class ScreenState {
    @Nullable
    private Screen screen;
    @Nullable
    private AbstractContainerMenu menu;

    @Nullable
    public Screen screen() {
        return this.screen;
    }

    @Nullable
    public AbstractContainerMenu menu() {
        return this.menu;
    }

    public void setScreen(@Nullable Screen screen) {
        this.screen = screen;
    }

    public void bindScreen(@Nullable Screen screen) {
        this.setScreen(screen);
    }

    public void setMenu(@Nullable AbstractContainerMenu menu) {
        this.menu = menu;
    }

    public void clear() {
        this.screen = null;
        this.menu = null;
    }
}
