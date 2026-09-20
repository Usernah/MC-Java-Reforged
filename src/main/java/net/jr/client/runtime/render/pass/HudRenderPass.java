package net.jr.client.runtime.render.pass;

import net.jr.api.client.render.gui.GuiRenderObjects;
import net.jr.api.client.render.gui.HudRenderContext;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.client.LocalClientReadinessPolicy;
import net.jr.client.runtime.context.LocalClientExecution;
import net.jr.client.runtime.render.state.HudStateScope;
import net.jr.client.runtime.ui.LocalChatRouter;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.runtime.viewport.ViewportHudLayout;
import net.jr.client.runtime.viewport.ViewportRenderScope;
import net.jr.mixin.accessors.HudAccessor;
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
        HudRenderContext context
    ) {
        LocalClient client = context.client();
        if (!LocalClientReadinessPolicy.gameplayReady(client)) {
            return;
        }
        GuiGraphicsExtractor graphics = context.graphics();
        var deltaTracker = context.deltaTracker();
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
            ControlHintRenderPass.PreparedHud preparedHints =
                ControlHintRenderPass.prepareHud(graphics);
            float bottomInset = preparedHints != null
                ? preparedHints.bottomInset()
                : 0.0F;
            try (
                ViewportHudLayout.BottomInsetScope ignoredInset =
                    ViewportHudLayout.pushBottomInset(bottomInset)
            ) {
                hud.extractRenderState(graphics, deltaTracker);
            }
            if (preparedHints != null) {
                ControlHintRenderPass.renderHud(preparedHints, graphics);
            }
            GuiRenderObjects.renderHud(context);
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
