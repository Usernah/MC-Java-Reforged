package net.jr.client.compat.jade;

import net.jr.api.client.render.gui.GuiRenderObjects;
import snownee.jade.util.ClientProxy;

public final class JadeHudRenderObject {
    private JadeHudRenderObject() {
    }

    public static void register() {
        GuiRenderObjects.registerHud(context -> JadeHudRenderRoute.run(() ->
            ClientProxy.onRenderTick(
                context.graphics(),
                context.deltaTracker().getRealtimeDeltaTicks()
            )
        ));
    }
}
