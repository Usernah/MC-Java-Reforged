package net.jr.client.compat.rpgseries;

import net.jr.api.client.render.gui.GuiRenderObjects;
import net.spell_engine.client.gui.HudRenderHelper;

public final class SpellEngineHudRenderObject {
    private SpellEngineHudRenderObject() {
    }

    public static void register() {
        GuiRenderObjects.registerHud(context -> HudRenderHelper.renderHudElement(
            context.graphics(),
            context.deltaTracker().getGameTimeDeltaPartialTick(true)
        ));
    }
}
