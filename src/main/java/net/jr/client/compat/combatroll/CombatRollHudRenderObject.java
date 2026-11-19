package net.jr.client.compat.combatroll;

import net.combat_roll.client.gui.HudRenderHelper;
import net.jr.api.client.render.gui.GuiRenderObjects;

public final class CombatRollHudRenderObject {
    private CombatRollHudRenderObject() {
    }

    public static void register() {
        GuiRenderObjects.registerHud(context -> HudRenderHelper.render(
            context.graphics(),
            context.deltaTracker().getGameTimeDeltaPartialTick(true)
        ));
    }
}
