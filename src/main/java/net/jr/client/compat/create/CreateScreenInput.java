package net.jr.client.compat.create;

import net.jr.api.client.ui.Viewport;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.input.LocalGamepadInputHandler;
import net.jr.client.runtime.viewport.ViewportArea;
import net.minecraft.client.MouseHandler;

public final class CreateScreenInput {
    private CreateScreenInput() {
    }

    public static double localWindowX(MouseHandler mouseHandler) {
        int slotId = slotId();
        if (LocalGamepadInputHandler.isVirtualCursorActive(slotId)) {
            return LocalGamepadInputHandler.cursorX(slotId) * viewport().windowWidth() / Viewport.width(slotId);
        }
        return mouseHandler.xpos() - viewport().windowX();
    }

    public static double localWindowY(MouseHandler mouseHandler) {
        int slotId = slotId();
        if (LocalGamepadInputHandler.isVirtualCursorActive(slotId)) {
            return LocalGamepadInputHandler.cursorY(slotId) * viewport().windowHeight() / Viewport.height(slotId);
        }
        return mouseHandler.ypos() - viewport().windowY();
    }

    public static int guiWidth() {
        return Viewport.width(slotId());
    }

    public static int guiHeight() {
        return Viewport.height(slotId());
    }

    public static int screenWidth() {
        return viewport().windowWidth();
    }

    public static int screenHeight() {
        return viewport().windowHeight();
    }

    public static int framebufferWidth() {
        return Viewport.pixelWidth(slotId());
    }

    public static int framebufferHeight() {
        return Viewport.pixelHeight(slotId());
    }

    private static int slotId() {
        Integer slotId = SlotScope.idOrNull();
        return slotId != null ? slotId : 0;
    }

    private static ViewportArea viewport() {
        return ClientRuntime.INSTANCE.viewports().viewport(slotId());
    }
}
