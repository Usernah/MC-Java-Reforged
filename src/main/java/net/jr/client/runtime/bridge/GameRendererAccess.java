package net.jr.client.runtime.bridge;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotScope;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;

public final class GameRendererAccess {
    private GameRendererAccess() {
    }

    public static Camera mainCamera(GameRenderer gameRenderer) {
        Integer slotId = SlotScope.idOrNull();
        return ClientRuntime.INSTANCE.slots().slot(slotId != null ? slotId : 0).renderState().camera();
    }
}
