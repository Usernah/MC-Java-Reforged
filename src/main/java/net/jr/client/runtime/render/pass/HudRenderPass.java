package net.jr.client.runtime.render.pass;

import net.jr.api.client.local.Client;
import net.jr.api.client.render.gui.GuiRenderObjects;
import net.jr.api.client.render.gui.HudRenderContext;
import net.jr.client.runtime.client.ReadinessPolicy;
import net.jr.client.runtime.context.InstanceExecution;
import net.jr.client.runtime.render.state.HudCameraOverlayScope;
import net.jr.client.runtime.render.state.HudStateScope;
import net.jr.client.runtime.ui.ChatRouter;
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

        for (var client : Client.all()) {
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
        Screen screen = Client.current().screen();
        ControlHintRenderPass.PreparedHud preparedHints =
            screen == null && !hud.isHidden()
                ? ControlHintRenderPass.prepareHud(context.graphics())
                : null;
        HudFramebufferPass.extract(context, compositionContext ->
            extractFramebufferContents(
                hud,
                compositionContext,
                screen,
                preparedHints
            )
        );
        if (screen == null && !hud.isHidden()) {
            FullHdHudFramebufferPass.extract(
                context,
                GuiRenderObjects::renderHudFullHdFramebuffer
            );
        }
        if (preparedHints != null) {
            ControlHintRenderPass.renderHud(
                preparedHints,
                context.graphics(),
                hudOpacity(context)
            );
        }
    }

    private static void extractFramebufferContents(
        Hud hud,
        HudRenderContext context,
        Screen screen,
        ControlHintRenderPass.PreparedHud preparedHints
    ) {
        if (hud.isHidden()) {
            return;
        }

        if (screen != null) {
            extractCameraEffects(
                hud,
                context.graphics(),
                context.deltaTracker()
            );
            return;
        }

        extractCameraEffects(
            hud,
            context.graphics(),
            context.deltaTracker()
        );
        extractHudContents(hud, context, preparedHints);
    }

    private static void extractCameraEffects(
            Hud hud,
            GuiGraphicsExtractor graphics,
            net.minecraft.client.DeltaTracker deltaTracker
    ) {
        try (HudStateScope ignoredHud = HudStateScope.enter(hud)) {
            ((HudAccessor) hud).javaReforged$extractCameraEffects(
                    graphics,
                    deltaTracker
            );
        }
    }

    private static void extractHudContents(
        Hud hud,
        HudRenderContext context,
        ControlHintRenderPass.PreparedHud preparedHints
    ) {
        HudTransparencyFramebufferPass.extract(
            context,
            transparentContext -> extractTransparentHudContents(
                hud,
                transparentContext,
                preparedHints
            )
        );
        GuiRenderObjects.renderHudFramebuffer(context);
    }

    private static void extractTransparentHudContents(
        Hud hud,
        HudRenderContext context,
        ControlHintRenderPass.PreparedHud preparedHints
    ) {
        GuiGraphicsExtractor graphics = context.graphics();
        var deltaTracker = context.deltaTracker();
        try (HudStateScope ignoredHud = HudStateScope.enter(hud)) {
            float bottomInset = preparedHints != null
                    ? preparedHints.bottomInset()
                    : 0.0F;
            try (
                    ViewportHudLayout.BottomInsetScope ignoredInset =
                            ViewportHudLayout.pushBottomInset(bottomInset);
                    HudCameraOverlayScope ignoredCameraEffects =
                            HudCameraOverlayScope.suppress();
                    BottomHudFramebufferPass.SuppressionScope ignoredBottomHud =
                            BottomHudFramebufferPass.suppressVanillaExtraction()
            ) {
                hud.extractRenderState(graphics, deltaTracker);
            }
            try (
                    ViewportHudLayout.BottomInsetScope ignoredInset =
                            ViewportHudLayout.pushBottomInset(bottomInset)
            ) {
                GuiRenderObjects.renderHud(context);
            }
            BottomHudFramebufferPass.extract(hud, context, bottomInset);
        }
    }

    private static float hudOpacity(HudRenderContext context) {
        double transparency = context.slot()
            .optionsState()
            .hudTransparency()
            .get();
        return 1.0F - (float)Math.clamp(transparency, 0.0D, 1.0D);
    }

    public static void extractTail(Hud hud, HudRenderContext context) {
        if (!ReadinessPolicy.currentGameplayReady()) {
            return;
        }

        if (!(Client.current().screen() instanceof DebugOptionsScreen)) {
            hud.extractDebugOverlay(context.graphics());
            if (!hud.isHidden()) {
                HudFramebufferPass.extractTail(
                    context,
                    framebufferContext -> HudTransparencyFramebufferPass.extractTail(
                        framebufferContext,
                        GuiRenderObjects::renderHudTail
                    )
                );
            }
        }

        hud.extractDeferredSubtitles();
    }
}
