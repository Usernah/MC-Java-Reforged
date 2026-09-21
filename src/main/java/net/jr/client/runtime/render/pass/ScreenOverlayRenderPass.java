package net.jr.client.runtime.render.pass;

import net.jr.api.client.render.gui.GuiRenderContext;
import net.jr.api.client.render.gui.GuiRenderObjects;
import net.jr.api.client.render.gui.GuiRenderPhase;
import net.jr.client.ui.cursor.CursorRenderer;
import net.jr.client.ui.overlay.ScreenOverlays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Extracts local screen overlays before the local cursor. */
public final class ScreenOverlayRenderPass {
    private ScreenOverlayRenderPass() {
    }

    public static void extract(Minecraft minecraft, Screen screen, GuiRenderContext context) {
        boolean hidesCursor = ScreenOverlays.render(
            screen, context.graphics(), context.deltaTracker().getGameTimeDeltaTicks()
        );
        ScreenOverlays.renderGamepadDisconnectedNotice(context);
        GuiRenderObjects.render(GuiRenderPhase.SCREEN_OVERLAY, context);
        if (!hidesCursor) {
            CursorRenderer.renderForCurrentClient(context.graphics(), minecraft);
        }
    }
}
