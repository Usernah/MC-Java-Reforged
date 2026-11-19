package net.jr.client.compat.xaero;

import net.jr.api.client.local.Client;
import net.jr.api.client.render.gui.GuiRenderObjects;
import net.minecraft.client.Minecraft;
import xaero.common.HudMod;
import xaero.common.anim.MultiplyAnimationHelper;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;

public final class XaeroMinimapHudRenderObject {
    private XaeroMinimapHudRenderObject() {
    }

    public static void register() {
        GuiRenderObjects.registerHud(context -> {
            if (Client.current().isPrimary()) {
                MultiplyAnimationHelper.tick();
            }
            XaeroMinimapRuntimeState.run(() -> {
                MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
                if (session == null) {
                    return;
                }
                HudMod.INSTANCE.getHudRenderer().render(
                    HudMod.INSTANCE.getHud(),
                    context.graphics(),
                    context.deltaTracker().getGameTimeDeltaPartialTick(true)
                );
                HudMod.INSTANCE.getMinimap().getWaypointMapRenderer().drawSetChange(
                    session,
                    context.graphics(),
                    Minecraft.getInstance().getWindow()
                );
            });
        });
    }
}
