package net.jr.client.runtime.bridge;

import net.jr.client.runtime.context.SlotExecution;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;

public final class GameRendererAccess {
    private GameRendererAccess() {
    }

    public static Camera mainCamera(GameRenderer gameRenderer) {
        return SlotExecution.currentSlot().renderState().camera();
    }
}
