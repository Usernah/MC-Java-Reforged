package net.jr.client.runtime.ui;

import javax.annotation.Nullable;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotExecution;
import net.jr.client.runtime.context.SlotResolver;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.slot.Slots;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.runtime.viewport.ViewportGuiScale;
import net.jr.api.client.ui.PresentationRebuildableScreen;
import net.jr.client.ui.render.composition.ScreenFramebufferSpace;
import net.jr.screens.PauseScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class SlotScreenManager {
    private SlotScreenManager() {
    }

    public static void bootstrapPrimary(Minecraft minecraft, Screen screen) {
        SlotExecution.runPrimary(minecraft, () -> minecraft.gui.setScreen(screen));
    }

    public static void initialize(
        Screen screen,
        Minecraft minecraft,
        int vanillaWidth,
        int vanillaHeight
    ) {
        Integer activeSlotId = SlotScope.idOrNull();
        int slotId = activeSlotId != null ? activeSlotId : 0;
        ViewportArea viewport = ClientRuntime.INSTANCE.viewports().viewportOrNull(slotId);
        int width = viewport != null
            ? ScreenFramebufferSpace.width(
                screen, viewport, ViewportGuiScale.screenLogicalWidth(viewport)
            )
            : vanillaWidth;
        int height = viewport != null
            ? ScreenFramebufferSpace.height(
                screen, viewport, ViewportGuiScale.screenLogicalHeight(viewport)
            )
            : vanillaHeight;
        screen.init(width, height);
    }

    public static void resizeAll(Minecraft minecraft) {
        resizeAll(minecraft, false);
    }

    public static void resizeAll(Minecraft minecraft, boolean layoutChanged) {
        ClientRuntime runtime = ClientRuntime.INSTANCE;
        if (!runtime.viewports().hasWindowMetrics()) {
            return;
        }

        for (Slots slot : runtime.viewports().drawableSlots()) {
            Screen screen = slot.screenState().screen();
            if (screen == null) {
                continue;
            }

            if (layoutChanged && screen instanceof PresentationRebuildableScreen rebuildable) {
                Screen rebuilt = rebuildable.rebuildForPresentation();
                SlotExecution.runForSlot(
                    minecraft,
                    slot.id(),
                    () -> minecraft.gui.setScreen(rebuilt)
                );
                continue;
            }

            ViewportArea viewport = runtime.viewports().viewport(slot.id());
            int width = ScreenFramebufferSpace.width(
                screen, viewport, ViewportGuiScale.screenLogicalWidth(viewport)
            );
            int height = ScreenFramebufferSpace.height(
                screen, viewport, ViewportGuiScale.screenLogicalHeight(viewport)
            );
            if (!layoutChanged && screen.width == width && screen.height == height) {
                continue;
            }

            SlotExecution.runForSlot(
                minecraft,
                slot.id(),
                () -> ScreenEventRunner.run(
                        screen,
                        "Resizing screen",
                    () -> screen.resize(width, height)
                )
            );
        }
    }

    public static void tickSecondary() {
        Minecraft minecraft = Minecraft.getInstance();
        for (Slots slot : ClientRuntime.INSTANCE.viewports().presentedSlots()) {
            if (slot.id() == 0) {
                continue;
            }

            SlotExecution.runForSlot(
                minecraft,
                slot.id(),
                () -> {
                    SleepScreenController.synchronize(minecraft);
                    Screen screen = slot.screenState().screen();
                    if (screen != null) {
                        ScreenEventRunner.run(
                                screen,
                                "Ticking screen",
                            screen::tick
                        );
                    }
                }
            );
        }
    }

    public static void openLocalPause(Minecraft minecraft, boolean pauseOnly) {
        if (activeScreenOrNull() != null) {
            return;
        }
        minecraft.gui.setScreen(new PauseScreen(!pauseOnly));
    }

    @Nullable
    public static Screen activeScreenOrNull() {
        Integer activeSlotId = SlotScope.idOrNull();
        Slots slot = activeSlotId != null
            ? ClientRuntime.INSTANCE.slots().slot(activeSlotId)
            : SlotResolver.requirePrimary();
        return slot.screenState().screen();
    }

}
