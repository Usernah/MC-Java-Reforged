package net.jr.client.ui.overlay;

import net.jr.api.client.render.gui.GuiRenderContext;
import net.jr.client.input.InputApi;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.runtime.viewport.ViewportGuiScale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;


public final class ScreenOverlays {
    private static final GamepadDisconnectedScreenOverlay GAMEPAD_DISCONNECTED =
        new GamepadDisconnectedScreenOverlay();

    private ScreenOverlays() {
    }

    public static ScreenOverlayManager current() {
        return ClientRuntime.INSTANCE.slots()
            .slot(InputApi.currentSlotId())
            .inputState()
            .screenOverlays();
    }

    public static boolean ownsInput(Screen screen) {
        return current().ownsInput(screen);
    }

    public static boolean tick(Screen screen) {
        return current().tick(screen);
    }

    public static boolean render(
        Screen screen,
        GuiGraphicsExtractor graphics,
        float partialTick
    ) {
        ViewportArea viewport = ClientRuntime.INSTANCE.viewports().viewport(InputApi.currentSlotId());
        return current().render(
            screen,
            graphics,
            ViewportGuiScale.screenLogicalWidth(viewport),
            ViewportGuiScale.screenLogicalHeight(viewport),
            partialTick
        );
    }


    public static void renderGamepadDisconnectedNotice(GuiGraphicsExtractor graphics) {
        int slotId = InputApi.currentSlotId();
        renderGamepadDisconnectedNotice(
            graphics, slotId, ClientRuntime.INSTANCE.viewports().viewport(slotId)
        );
    }

    public static void renderGamepadDisconnectedNotice(GuiRenderContext context) {
        renderGamepadDisconnectedNotice(
            context.graphics(), context.slot().id(), context.viewport()
        );
    }

    private static void renderGamepadDisconnectedNotice(
        GuiGraphicsExtractor graphics, int slotId, ViewportArea viewport
    ) {
        if (slotId == 0 || !ClientRuntime.INSTANCE.slots().slot(slotId).input().hasDisconnectedGamepad()) {
            return;
        }

        GAMEPAD_DISCONNECTED.render(
            graphics,
            ViewportGuiScale.logicalWidth(viewport),
            ViewportGuiScale.logicalHeight(viewport),
            0.0F
        );
    }
}
