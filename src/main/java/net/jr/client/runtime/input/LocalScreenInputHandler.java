package net.jr.client.runtime.input;

import net.jr.client.input.InputApi;
import net.jr.client.input.runtime.GamepadInputProcessor;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotScope;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Minecraft;
import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.context.LocalClientScope;

public final class LocalScreenInputHandler {
    private LocalScreenInputHandler() {
    }

    public static void releaseMouse(MouseHandler mouseHandler) {
        int slotId = activeSlotId();
        if (!InputApi.canPhysicalMouseDriveSlot(slotId)) {
            return;
        }
        mouseHandler.releaseMouse();
        if (ClientRuntime.INSTANCE.viewports().presentedCount() > 1) {
            GamepadInputProcessor.centerPhysicalCursorForScreen(slotId);
        }
    }

    public static void grabMouse(MouseHandler mouseHandler) {
        if (InputApi.canPhysicalMouseDriveSlot(activeSlotId())) {
            mouseHandler.grabMouse();
        }
    }

    public static boolean isGameplayCaptured(Minecraft minecraft, MouseHandler mouseHandler) {
        LocalClient client = LocalClientScope.currentClientOrNull();
        if (client != null && client.slotId() > 0 && ClientRuntime.INSTANCE.clients().count() > 1) {
            return minecraft.isWindowActive();
        }
        return mouseHandler.isMouseGrabbed();
    }

    private static int activeSlotId() {
        Integer slotId = SlotScope.idOrNull();
        return slotId != null ? slotId : 0;
    }
}
