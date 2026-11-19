package net.jr.client.runtime.render;

import java.util.ArrayList;
import java.util.List;
import net.jr.api.client.render.gui.GlobalRenderContext;
import net.jr.api.client.render.gui.GuiRenderContext;
import net.jr.api.client.render.gui.GuiRenderObjects;
import net.jr.api.client.render.gui.GuiRenderPhase;
import net.jr.api.client.render.gui.HudRenderContext;
import net.jr.api.client.ui.FramebufferScreen;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.ClientInstance;
import net.jr.client.runtime.context.InstanceExecution;
import net.jr.client.runtime.context.SlotExecution;
import net.jr.client.runtime.render.pass.HudPass;
import net.jr.client.runtime.render.pass.ControlHintPass;
import net.jr.client.runtime.render.pass.ScreenOverlayRenderPass;
import net.jr.client.runtime.render.pass.ScreenCompositionPass;
import net.jr.client.ui.dsl.ScreenEditorHooks;
import net.jr.client.runtime.render.pass.ScreenPass;
import net.jr.client.runtime.render.pass.ToastRenderPass;
import net.jr.client.runtime.render.pass.SystemUiRenderPass;
import net.jr.client.runtime.slot.Slots;
import net.jr.client.runtime.viewport.GuiViewportScope;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.runtime.viewport.ViewportGuiScale;
import net.jr.api.client.systemui.SystemUI;
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
        guiRenderState.isHudHidden = gui.hud.isHidden();

        boolean systemUiOwnsGlobalPointer = SystemUI.blocksAllSlots();
        GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(
                minecraft,
                guiRenderState,
                systemUiOwnsGlobalPointer ? Integer.MIN_VALUE : mouseX,
                systemUiOwnsGlobalPointer ? Integer.MIN_VALUE : mouseY
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
            SystemUiRenderPass.extract(globalContext);
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
                        HudPass.extractCurrent(hud, frame.hudContext());
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
                true,
                frame -> {
                    GuiRenderContext context = frame.context();
                    Screen configuredScreen = context.slot().screenState().screen();
                    if (configuredScreen instanceof FramebufferScreen) {
                        Screen screen = ScreenCompositionPass.extract(
                            minecraft,
                            configuredScreen,
                            context,
                            context.deltaTracker().getGameTimeDeltaTicks()
                        );
                        ControlHintPass.renderScreen(screen, context.graphics());
                        return;
                    }
                    float screenScale = ViewportGuiScale.screenScale(context.viewport());
                    Screen screen;
                    ViewportGuiScale.beginScreen();
                    context.graphics().pose().pushMatrix();
                    context.graphics().pose().scale(screenScale, screenScale);
                    try {
                        // Non-FramebufferScreen menus share the same editor hooks.
                        // Keep their native viewport transform; no editor scale/FBO.
                        if (configuredScreen != null) {
                            ScreenEditorHooks.before(configuredScreen, context.graphics());
                        }
                        screen = ScreenPass.extract(
                            minecraft,
                            context.graphics(),
                            context.deltaTracker().getGameTimeDeltaTicks()
                        );
                        if (screen != null) {
                            GuiRenderObjects.render(GuiRenderPhase.SCREEN, context);
                            ScreenOverlayRenderPass.extract(minecraft, screen, context);
                            ScreenEditorHooks.after(screen, context.graphics());
                        }
                    } finally {
                        context.graphics().pose().popMatrix();
                        ViewportGuiScale.endScreen();
                    }
                    if (screen != null) {
                        ControlHintPass.renderScreen(screen, context.graphics());
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
                        HudPass.extractTail(hud, frame.hudContext());
                    }
                }
        );
    }

    private static void forEachPresentedSlot(
            Minecraft minecraft,
            List<ViewFrame> views,
            SlotPhase phase
    ) {
        forEachPresentedSlot(minecraft, views, false, phase);
    }

    private static void forEachPresentedSlot(
            Minecraft minecraft,
            List<ViewFrame> views,
            boolean screenPhase,
            SlotPhase phase
    ) {
        boolean hasFullWindowEditor = screenPhase && views.stream().anyMatch(frame ->
            ScreenEditorHooks.fullWindowFor(frame.context().slot().screenState().screen())
        );
        for (ViewFrame frame : views) {
            GuiRenderContext context = frame.context();
            Screen configuredScreen = context.slot().screenState().screen();
            boolean fullWindowEditor = screenPhase
                && ScreenEditorHooks.fullWindowFor(configuredScreen);
            if (hasFullWindowEditor && !fullWindowEditor) {
                continue;
            }
            Runnable extraction = () -> {
                try (
                        GuiViewportScope ignoredViewport = GuiViewportScope.enter(
                            context.graphics(), context.viewport()
                        )
                ) {
                    phase.extract(frame);
                }
            };

            if (frame.client() != null) {
                InstanceExecution.runForClient(
                        context.slot().id(),
                        context.viewport(),
                        extraction
                );
            } else {
                SlotExecution.runForSlot(
                        minecraft,
                        context.slot(),
                        context.viewport(),
                        extraction
                );
            }
        }
    }

    private static List<ViewFrame> collectViews(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        ClientRuntime runtime = ClientRuntime.INSTANCE;
        List<ViewFrame> views = new ArrayList<>();
        for (var drawable : runtime.viewports().drawableViewports()) {
            Slots slot = drawable.slot();
            ViewportArea viewport = drawable.viewport();
            ClientInstance client = runtime.clients().clientOrNull(slot.id());
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

    private record ViewFrame(GuiRenderContext context, ClientInstance client, HudRenderContext hudContext) {
    }

    private record LocalFrameContext(
            Slots slot, ViewportArea viewport, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker
    ) implements GuiRenderContext {
    }

    private record HudFrameContext(
            Slots slot, ViewportArea viewport, GuiGraphicsExtractor graphics,
            DeltaTracker deltaTracker, ClientInstance client
    ) implements HudRenderContext {
    }

    private record GlobalFrameContext(
        GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, int guiWidth, int guiHeight
    ) implements GlobalRenderContext {
    }
}
