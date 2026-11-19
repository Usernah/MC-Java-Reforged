package net.jr.client.runtime.render;

import net.jr.api.client.render.gui.GuiRenderObjects;
import net.jr.api.client.render.gui.GuiRenderPhase;
import net.jr.client.runtime.render.pass.JoinPromptRenderPass;
import net.jr.client.ui.hud.PlayerModelHudOverlay;
import net.jr.client.ui.hud.PlayerNameHudOverlay;
import net.jr.client.ui.overlay.ScreenOverlays;
import net.jr.experimental_features.minigame.MinigameHudOverlay;
import net.jr.experimental_features.minigame.PlayerInforBar;
import net.jr.experimental_features.minigame.battle.client.BattleChestRefillOverlay;


public final class BuiltInGuiRenderObjects {
    private static boolean registered;

    private BuiltInGuiRenderObjects() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        GuiRenderObjects.registerHud(new MinigameHudOverlay());
        GuiRenderObjects.registerHudFramebuffer(new PlayerModelHudOverlay());
        GuiRenderObjects.registerBottomHud(new PlayerInforBar());
        GuiRenderObjects.registerHudFullHdFramebuffer(new PlayerNameHudOverlay());
        GuiRenderObjects.registerHud(ScreenOverlays::renderGamepadDisconnectedNotice);
        GuiRenderObjects.registerGlobal(GuiRenderPhase.GLOBAL, JoinPromptRenderPass::render);
        GuiRenderObjects.registerGlobal(GuiRenderPhase.GLOBAL, new BattleChestRefillOverlay());
        CompatGuiRenderObjects.register();
    }
}
