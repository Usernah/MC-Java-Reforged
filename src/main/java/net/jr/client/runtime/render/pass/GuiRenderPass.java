package net.jr.client.runtime.render.pass;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.context.LocalClientExecution;
import net.jr.client.runtime.context.SlotExecution;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.jr.client.runtime.ui.SecondaryWorldLoadingScreen;
import net.jr.client.runtime.viewport.GuiViewportScope;
import net.jr.client.runtime.viewport.ViewportArea;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.debug.DebugOptionsScreen;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;

public final class GuiRenderPass {
    private GuiRenderPass() {
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

        try {
            if (shouldRenderLevel) {
                extractHudPhase(
                        minecraft,
                        gui.hud,
                        graphics,
                        deltaTracker
                );
            }

            Overlay overlay = gui.overlay();

            if (overlay != null) {
                overlay.extractRenderState(
                        graphics,
                        mouseX,
                        mouseY,
                        deltaTracker.getGameTimeDeltaTicks()
                );
            } else if (gameLoadFinished) {
                extractScreenPhase(
                        minecraft,
                        graphics,
                        deltaTracker
                );
            }

            if (shouldRenderLevel) {
                extractSavingPhase(
                        minecraft,
                        gui.hud,
                        graphics,
                        deltaTracker
                );
            }

            if (gameLoadFinished) {
                ToastRenderPass.extract(
                        gui.toastManager(),
                        graphics
                );
            }

            extractHudTailPhase(
                    minecraft,
                    gui.hud,
                    graphics
            );
        } finally {
            profiler.pop();
        }

        graphics.applyCursor(minecraft.getWindow());
    }

    private static void extractHudPhase(
            Minecraft minecraft,
            Hud hud,
            GuiGraphicsExtractor graphics,
            DeltaTracker deltaTracker
    ) {
        forEachPresentedSlot(
                minecraft,
                graphics,
                (slot, hasClient) -> {
                    if (hasClient) {
                        HudRenderPass.extractCurrent(
                                hud,
                                graphics,
                                deltaTracker
                        );
                    }
                }
        );
    }

    private static void extractScreenPhase(
            Minecraft minecraft,
            GuiGraphicsExtractor graphics,
            DeltaTracker deltaTracker
    ) {
        forEachPresentedSlot(
                minecraft,
                graphics,
                (slot, hasClient) ->
                        ScreenRenderPass.extract(
                                minecraft,
                                graphics,
                                deltaTracker.getGameTimeDeltaTicks()
                        )
        );
    }

    private static void extractSavingPhase(
            Minecraft minecraft,
            Hud hud,
            GuiGraphicsExtractor graphics,
            DeltaTracker deltaTracker
    ) {
        forEachPresentedSlot(
                minecraft,
                graphics,
                (slot, hasClient) -> {
                    if (
                            hasClient
                                    && !SecondaryWorldLoadingScreen.shouldPresent(slot)
                    ) {
                        hud.extractSavingIndicator(
                                graphics,
                                deltaTracker
                        );
                    }
                }
        );
    }

    private static void extractHudTailPhase(
            Minecraft minecraft,
            Hud hud,
            GuiGraphicsExtractor graphics
    ) {
        forEachPresentedSlot(
                minecraft,
                graphics,
                (slot, hasClient) -> {
                    if (
                            !hasClient
                                    || SecondaryWorldLoadingScreen.shouldPresent(slot)
                    ) {
                        return;
                    }

                    Screen screen = slot.screenState().screen();

                    if (!(screen instanceof DebugOptionsScreen)) {
                        hud.extractDebugOverlay(graphics);
                    }

                    hud.extractDeferredSubtitles();
                }
        );
    }

    private static void forEachPresentedSlot(
            Minecraft minecraft,
            GuiGraphicsExtractor graphics,
            SlotPhase phase
    ) {
        ClientRuntime runtime = ClientRuntime.INSTANCE;

        for (LocalClientSlot slot : runtime.viewports().drawableSlots()) {
            LocalClient client =
                    runtime.clients().clientOrNull(slot.id());

            Runnable extraction = () -> {
                ViewportArea viewport =
                        runtime.viewports().viewport(slot.id());

                try (
                        GuiViewportScope ignoredViewport =
                                GuiViewportScope.enter(graphics, viewport)
                ) {
                    phase.extract(slot, client != null);
                }
            };

            if (client != null) {
                LocalClientExecution.runForClient(
                        slot.id(),
                        extraction
                );
            } else {
                SlotExecution.runForSlot(
                        minecraft,
                        slot.id(),
                        extraction
                );
            }
        }
    }

    @FunctionalInterface
    private interface SlotPhase {
        void extract(
                LocalClientSlot slot,
                boolean hasClient
        );
    }
}