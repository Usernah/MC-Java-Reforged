package net.jr.client.runtime.input;

import net.jr.api.client.input.IScreenInput;
import net.jr.api.client.ui.Viewport;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.viewport.ViewportArea;
import net.minecraft.client.MouseHandler;

/** Converts the current pointer and dimensions into one slot's screen space. */
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
        return Viewport.get(this.slotId).logicalGuiWidth();
    }

    @Override
    public int guiHeight() {
        return Viewport.get(this.slotId).logicalGuiHeight();
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
        return Viewport.get(this.slotId).width();
    }

    @Override
    public int framebufferHeight() {
        return Viewport.get(this.slotId).height();
    }

    private ViewportArea viewport() {
        return ClientRuntime.INSTANCE.viewports().viewport(this.slotId);
    }
}
