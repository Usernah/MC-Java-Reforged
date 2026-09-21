package net.jr.client.runtime.render;

import java.util.ArrayList;
import java.util.List;
import net.jr.api.client.render.gui.GlobalRenderContext;
import net.jr.api.client.render.gui.GuiRenderContext;
import net.jr.api.client.render.gui.GuiRenderObjects;
import net.jr.api.client.render.gui.GuiRenderPhase;
import net.jr.api.client.render.gui.HudRenderContext;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.context.LocalClientExecution;
import net.jr.client.runtime.context.SlotExecution;
import net.jr.client.runtime.render.pass.HudRenderPass;
import net.jr.client.runtime.render.pass.ScreenOverlayRenderPass;
import net.jr.client.runtime.render.pass.ScreenRenderPass;
import net.jr.client.runtime.render.pass.ToastRenderPass;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.jr.client.runtime.viewport.GuiViewportScope;
import net.jr.client.runtime.viewport.ViewportArea;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;

public final class GuiRenderPipeline {
    private GuiRenderPipeline() {
    }

    public static void extract(
            Gui gui,
            GuiRenderState guiRenderState,
            DeltaTracker deltaTracker,
            boolean shouldRenderLevel,
            boolean gameLoadFinished
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        int mouseX = (int)minecraft.mouseHandler.getScaledXPos(
                minecraft.getWindow()
        );
        int mouseY = (int)minecraft.mouseHandler.getScaledYPos(
                minecraft.getWindow()
        );

        ProfilerFiller profiler = Profiler.get();
        profiler.push("gui");

        guiRenderState.reset();

        GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(
                minecraft,
                guiRenderState,
                mouseX,
                mouseY
        );
        List<ViewFrame> views = collectViews(graphics, deltaTracker);
        GlobalRenderContext globalContext = new GlobalFrameContext(
            graphics, deltaTracker, graphics.guiWidth(), graphics.guiHeight()
        );

        try {
            if (shouldRenderLevel) {
                extractHudPhase(minecraft, gui.hud, views);
            }

            Overlay overlay = gui.overlay();

            if (overlay != null) {
                overlay.extractRenderState(
                        graphics,
                        mouseX,
                        mouseY,
                        deltaTracker.getGameTimeDeltaTicks()
                );
                GuiRenderObjects.renderGlobal(GuiRenderPhase.VANILLA_OVERLAY, globalContext);
            } else if (gameLoadFinished) {
                extractScreenPhase(minecraft, views);
            }

            if (gameLoadFinished) {
                ToastRenderPass.extract(
                        gui.toastManager(),
                        graphics
                );
                forEachPresentedSlot(minecraft, views, frame ->
                    GuiRenderObjects.render(GuiRenderPhase.TOAST, frame.context())
                );
                GuiRenderObjects.renderGlobal(GuiRenderPhase.TOAST, globalContext);
            }

            extractHudTailPhase(minecraft, gui.hud, views);

            GuiRenderObjects.renderGlobal(GuiRenderPhase.GLOBAL, globalContext);
        } finally {
            profiler.pop();
        }

        graphics.applyCursor(minecraft.getWindow());
    }

    private static void extractHudPhase(
            Minecraft minecraft,
            Hud hud,
            List<ViewFrame> views
    ) {
        forEachPresentedSlot(
                minecraft,
                views,
                frame -> {
                    if (frame.hudContext() != null) {
                        HudRenderPass.extractCurrent(hud, frame.hudContext());
                    }
                    GuiRenderObjects.render(GuiRenderPhase.OVERLAY, frame.context());
                }
        );
    }

    private static void extractScreenPhase(
            Minecraft minecraft,
            List<ViewFrame> views
    ) {
        forEachPresentedSlot(
                minecraft,
                views,
                frame -> {
                    GuiRenderContext context = frame.context();
                    Screen screen = ScreenRenderPass.extract(
                        minecraft,
                        context.graphics(),
                        context.deltaTracker().getGameTimeDeltaTicks()
                    );
                    if (screen != null) {
                        GuiRenderObjects.render(GuiRenderPhase.SCREEN, context);
                        ScreenOverlayRenderPass.extract(minecraft, screen, context);
                    }
                }
        );
    }

    private static void extractHudTailPhase(
            Minecraft minecraft,
            Hud hud,
            List<ViewFrame> views
    ) {
        forEachPresentedSlot(
                minecraft,
                views,
                frame -> {
                    if (frame.hudContext() != null) {
                        HudRenderPass.extractTail(hud, frame.hudContext());
                    }
                }
        );
    }

    private static void forEachPresentedSlot(
            Minecraft minecraft,
            List<ViewFrame> views,
            SlotPhase phase
    ) {
        for (ViewFrame frame : views) {
            GuiRenderContext context = frame.context();
            Runnable extraction = () -> {
                try (
                        GuiViewportScope ignoredViewport =
                                GuiViewportScope.enter(context.graphics(), context.viewport())
                ) {
                    phase.extract(frame);
                }
            };

            if (frame.client() != null) {
                LocalClientExecution.runForClient(
                        context.slot().id(),
                        extraction
                );
            } else {
                SlotExecution.runForSlot(
                        minecraft,
                        context.slot().id(),
                        extraction
                );
            }
        }
    }

    private static List<ViewFrame> collectViews(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        ClientRuntime runtime = ClientRuntime.INSTANCE;
        List<ViewFrame> views = new ArrayList<>();
        for (LocalClientSlot slot : runtime.viewports().drawableSlots()) {
            ViewportArea viewport = runtime.viewports().viewport(slot.id());
            LocalClient client = runtime.clients().clientOrNull(slot.id());
            GuiRenderContext context = new LocalFrameContext(slot, viewport, graphics, deltaTracker);
            HudRenderContext hudContext = client == null ? null
                : new HudFrameContext(slot, viewport, graphics, deltaTracker, client);
            views.add(new ViewFrame(context, client, hudContext));
        }
        return List.copyOf(views);
    }

    @FunctionalInterface
    private interface SlotPhase {
        void extract(ViewFrame frame);
    }

    private record ViewFrame(GuiRenderContext context, LocalClient client, HudRenderContext hudContext) {
    }

    private record LocalFrameContext(
        LocalClientSlot slot, ViewportArea viewport, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker
    ) implements GuiRenderContext {
    }

    private record HudFrameContext(
        LocalClientSlot slot, ViewportArea viewport, GuiGraphicsExtractor graphics,
        DeltaTracker deltaTracker, LocalClient client
    ) implements HudRenderContext {
    }

    private record GlobalFrameContext(
        GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, int guiWidth, int guiHeight
    ) implements GlobalRenderContext {
    }
}
