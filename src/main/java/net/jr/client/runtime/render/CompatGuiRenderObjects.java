package net.jr.client.runtime.render;

import net.neoforged.fml.ModList;

/** Optional HUD integrations registered into the same pipeline as native HUD objects. */
final class CompatGuiRenderObjects {
    private CompatGuiRenderObjects() {
    }

    static void register() {
        ModList mods = ModList.get();
        if (mods.isLoaded("combat_roll")) {
            net.jr.client.compat.combatroll.CombatRollHudRenderObject.register();
        }
        if (mods.isLoaded("spell_engine")) {
            net.jr.client.compat.rpgseries.SpellEngineHudRenderObject.register();
        }
        if (mods.isLoaded("jade")) {
            net.jr.client.compat.jade.JadeHudRenderObject.register();
        }
        if (mods.isLoaded("xaerominimap")) {
            net.jr.client.compat.xaero.XaeroMinimapHudRenderObject.register();
        }
    }
}
