package net.jr.client.runtime.ui;

import javax.annotation.Nullable;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotExecution;
import net.jr.client.runtime.context.SlotResolver;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.input.LocalScreenInput;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.runtime.viewport.ViewportGuiScale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;

public final class LocalScreenManager {
    private LocalScreenManager() {
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
        int width = viewport != null ? ViewportGuiScale.logicalWidth(viewport) : vanillaWidth;
        int height = viewport != null ? ViewportGuiScale.logicalHeight(viewport) : vanillaHeight;
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

        for (LocalClientSlot slot : runtime.viewports().drawableSlots()) {
            Screen screen = slot.screenState().screen();
            if (screen == null) {
                continue;
            }

            ViewportArea viewport = runtime.viewports().viewport(slot.id());
            int width = ViewportGuiScale.logicalWidth(viewport);
            int height = ViewportGuiScale.logicalHeight(viewport);
            if (!layoutChanged && screen.width == width && screen.height == height) {
                continue;
            }

            SlotExecution.runForSlot(
                minecraft,
                slot.id(),
                () -> LocalScreenInput.runEvent(
                    () -> screen.resize(width, height),
                    "Resizing screen",
                    screen.getClass().getCanonicalName()
                )
            );
        }
    }

    public static void tickSecondary() {
        Minecraft minecraft = Minecraft.getInstance();
        for (LocalClientSlot slot : ClientRuntime.INSTANCE.viewports().presentedSlots()) {
            if (slot.id() == 0) {
                continue;
            }

            Screen screen = slot.screenState().screen();
            if (screen == null) {
                continue;
            }

            SlotExecution.runForSlot(
                minecraft,
                slot.id(),
                () -> LocalScreenInput.runEvent(
                    screen::tick,
                    "Ticking screen",
                    screen.getClass().getCanonicalName()
                )
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
        LocalClientSlot slot = activeSlotId != null
            ? ClientRuntime.INSTANCE.slots().slot(activeSlotId)
            : SlotResolver.requirePrimary();
        return slot.screenState().screen();
    }
}
