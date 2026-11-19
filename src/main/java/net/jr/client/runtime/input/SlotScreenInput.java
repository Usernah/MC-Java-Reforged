package net.jr.client.runtime.input;

import net.jr.api.client.input.IScreenInput;
import net.jr.api.client.ui.Viewport;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.ui.render.composition.ScreenFramebufferSpace;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;


final class SlotScreenInput implements IScreenInput {
    private final int slotId;

    SlotScreenInput(int slotId) {
        this.slotId = slotId;
    }

    @Override
    public double localWindowX(MouseHandler mouse) {
        ViewportArea viewport = viewport();
        if (GamepadInputHandler.isVirtualCursorActive(this.slotId)) {
            return GamepadInputHandler.cursorX(this.slotId)
                * viewport.windowWidth() / guiWidth();
        }
        return mouse.xpos() - viewport.windowX();
    }

    @Override
    public double localWindowY(MouseHandler mouse) {
        ViewportArea viewport = viewport();
        if (GamepadInputHandler.isVirtualCursorActive(this.slotId)) {
            return GamepadInputHandler.cursorY(this.slotId)
                * viewport.windowHeight() / guiHeight();
        }
        return mouse.ypos() - viewport.windowY();
    }

    @Override
    public int guiWidth() {
        ViewportArea viewport = viewport();
        return ScreenFramebufferSpace.width(
            screen(), viewport, viewport.logicalGuiWidth()
        );
    }

    @Override
    public int guiHeight() {
        ViewportArea viewport = viewport();
        return ScreenFramebufferSpace.height(
            screen(), viewport, viewport.logicalGuiHeight()
        );
    }

    @Override
    public int windowWidth() {
        return viewport().windowWidth();
    }

    @Override
    public int windowHeight() {
        return viewport().windowHeight();
    }

    @Override
    public int framebufferWidth() {
        return viewport().width();
    }

    @Override
    public int framebufferHeight() {
        return viewport().height();
    }

    private ViewportArea viewport() {
        return Viewport.current();
    }

    private Screen screen() {
        return ClientRuntime.INSTANCE.slots().slot(this.slotId).screenState().screen();
    }
}
