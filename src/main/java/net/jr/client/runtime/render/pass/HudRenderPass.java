package net.jr.client.runtime.render.pass;

import net.jr.api.client.split.SplitScreen;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.client.LocalClientReadinessPolicy;
import net.jr.client.runtime.context.LocalClientExecution;
import net.jr.client.runtime.context.LocalClientScope;
import net.jr.client.runtime.render.state.HudStateScope;
import net.jr.client.runtime.ui.LocalChatRouter;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.runtime.viewport.ViewportRenderScope;
import net.jr.client.ui.hud.MinimapHudOverlay;
import net.jr.client.ui.hud.PlayerNameHudOverlay;
import net.jr.mixin.accessors.HudAccessor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;

public final class HudRenderPass {
    private HudRenderPass() {
    }

    public static void tick(Gui gui) {
        Hud hud = gui.hud;
        LocalClientExecution.runPrimary(() -> {
            try (HudStateScope ignoredHud = HudStateScope.enter(hud)) {
                gui.tick();
            }
        });

        for (LocalClient client : ClientRuntime.INSTANCE.clients().all()) {
            if (client.slotId() == 0 || !LocalClientReadinessPolicy.worldBound(client)) {
                continue;
            }
            LocalClientExecution.runForClient(client.slotId(), () -> {
                try (HudStateScope ignoredHud = HudStateScope.enter(hud)) {
                    gui.chatListener().tick();
                    hud.tick(Minecraft.getInstance().isPaused());
                }
            });
        }
    }

    public static void tickChat(ChatComponent chat) {
        LocalChatRouter.tick(chat, Minecraft.getInstance().gui.hud.getGuiTicks());
    }

    public static void extractCurrent(
        Hud hud,
        GuiGraphicsExtractor graphics,
        DeltaTracker deltaTracker
    ) {
        LocalClient client = LocalClientScope.currentClient();
        if (!LocalClientReadinessPolicy.gameplayReady(client)) {
            return;
        }
        Screen screen = client.screen();
        if (screen != null) {
            try (HudStateScope ignoredHud = HudStateScope.enter(hud)) {
                ((HudAccessor)hud).javaReforged$extractCameraEffects(
                        graphics,
                        deltaTracker
                );
            }
            return;
        }

        try (HudStateScope ignoredHud = HudStateScope.enter(hud)) {
            hud.extractRenderState(graphics, deltaTracker);
            // MinimapHudOverlay.extract(graphics);
            ControlHintRenderPass.renderHud(graphics);
            if (SplitScreen.isActive()) {
                PlayerNameHudOverlay.render(graphics);
            }
        }
    }

    public static Integer guiWidthOrNull() {
        ViewportArea viewport = ViewportRenderScope.activeViewportOrNull();
        return viewport == null ? null : viewport.guiWidth();
    }

    public static Integer guiHeightOrNull() {
        ViewportArea viewport = ViewportRenderScope.activeViewportOrNull();
        return viewport == null ? null : viewport.guiHeight();
    }
}
