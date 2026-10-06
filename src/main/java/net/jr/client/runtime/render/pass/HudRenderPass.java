package net.jr.client.runtime.render.pass;

import net.jr.api.client.local.Client;
import net.jr.api.client.render.gui.BottomHudRenderContext;
import net.jr.api.client.render.gui.GuiRenderObjects;
import net.jr.api.client.render.gui.HudRenderContext;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.ClientInstance;
import net.jr.client.runtime.client.ReadinessPolicy;
import net.jr.client.runtime.context.InstanceExecution;
import net.jr.client.runtime.render.state.HudStateScope;
import net.jr.client.runtime.ui.ChatRouter;
import net.jr.screens.SecondaryWorldLoadingScreen;
import net.jr.client.runtime.viewport.ViewportHudLayout;
import net.jr.mixin.accessors.HudAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.debug.DebugOptionsScreen;
import net.minecraft.client.player.LocalPlayer;

public final class HudRenderPass {
    private HudRenderPass() {
    }

    public static void tick(Gui gui) {
        Hud hud = gui.hud;
        InstanceExecution.runPrimary(() -> {
            try (HudStateScope ignoredHud = HudStateScope.enter(hud)) {
                gui.tick();
            }
        });

        for (ClientInstance client : ClientRuntime.INSTANCE.clients().all()) {
            if (client.isPrimary() || !ReadinessPolicy.worldBound(client)) {
                continue;
            }
            InstanceExecution.runForClient(client.slotId(), () -> {
                try (HudStateScope ignoredHud = HudStateScope.enter(hud)) {
                    gui.chatListener().tick();
                    hud.tick(Minecraft.getInstance().isPaused());
                    LocalPlayer player = client.player();

                    if (client.screen() == null
                            && player != null
                            && player.isDeadOrDying()) {
                        gui.setScreen(null);
                    }
                }
            });
        }
    }

    public static void tickChat(ChatComponent chat) {
        ChatRouter.tick(chat, Minecraft.getInstance().gui.hud.getGuiTicks());
    }

    public static void extractCurrent(
            Hud hud,
            HudRenderContext context
    ) {
        if (!ReadinessPolicy.currentGameplayReady()) {
            return;
        }
        GuiGraphicsExtractor graphics = context.graphics();
        var deltaTracker = context.deltaTracker();
        Screen screen = Client.current().screen();
        if (screen != null) {
            try (HudStateScope ignoredHud = HudStateScope.enter(hud)) {
                ((HudAccessor) hud).javaReforged$extractCameraEffects(
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
            try (
                    ViewportHudLayout.BottomInsetScope ignoredInset =
                            ViewportHudLayout.pushBottomInset(bottomInset)
            ) {
                GuiRenderObjects.renderHud(context);
                int availableHeight = Math.max(
                        0,
                        Math.round(context.viewport().guiHeight() - bottomInset)
                );
                BottomHudRenderContext bottomContext =
                        BottomHudRenderContext.from(context, availableHeight);
                ViewportHudLayout.pushBottomScaleTransform(
                        graphics,
                        availableHeight
                );
                try {
                    GuiRenderObjects.renderBottomHud(bottomContext);
                } finally {
                    ViewportHudLayout.popBottomTransform(graphics);
                }
            }
        }
    }

    public static void extractTail(Hud hud, HudRenderContext context) {
        if (SecondaryWorldLoadingScreen.shouldPresent(context.slot())) {
            return;
        }

        if (!(Client.current().screen() instanceof DebugOptionsScreen)) {
            hud.extractDebugOverlay(context.graphics());
            GuiRenderObjects.renderHudTail(context);
        }

        hud.extractDeferredSubtitles();
    }
}
