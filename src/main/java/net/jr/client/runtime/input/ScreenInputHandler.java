package net.jr.client.runtime.input;

import net.jr.api.client.local.Client;
import net.jr.client.input.InputApi;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotExecution;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Minecraft;
import net.jr.client.runtime.client.ClientInstance;

public final class ScreenInputHandler {
    private ScreenInputHandler() {
    }

    public static void releaseMouse(MouseHandler mouseHandler) {
        int slotId = activeSlotId();
        if (!InputApi.canPhysicalMouseDriveSlot(slotId)) {
            return;
        }
        mouseHandler.releaseMouse();
        if (ClientRuntime.INSTANCE.viewports().presentedCount() > 1) {
            GamepadInputHandler.centerPhysicalCursorForScreen(slotId);
        }
    }

    public static void grabMouse(MouseHandler mouseHandler) {
        if (InputApi.canPhysicalMouseDriveSlot(activeSlotId())) {
            mouseHandler.grabMouse();
        }
    }

    public static boolean isGameplayCaptured(Minecraft minecraft, MouseHandler mouseHandler) {
        ClientInstance client = Client.currentOrNull();
        if (client != null && client.slotId() > 0 && ClientRuntime.INSTANCE.clients().count() > 1) {
            return minecraft.isWindowActive();
        }
        return mouseHandler.isMouseGrabbed();
    }

    private static int activeSlotId() {
        return SlotExecution.currentSlotId();
    }
}
