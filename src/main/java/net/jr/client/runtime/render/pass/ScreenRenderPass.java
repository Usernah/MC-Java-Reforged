package net.jr.client.runtime.render.pass;

import java.util.Stack;
import net.jr.client.input.InputApi;
import net.jr.client.input.cursor.CursorRenderer;
import net.jr.client.input.runtime.GamepadInputProcessor;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.input.LocalScreenInput;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.jr.client.runtime.ui.SecondaryWorldLoadingScreen;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.runtime.viewport.ViewportGuiScale;
import net.jr.client.ui.hint.ControlHintPipeline;
import net.jr.client.ui.navigation.UiInputModeController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.ClientHooks;

public final class ScreenRenderPass {
    private ScreenRenderPass() {
    }

    public static void extract(
        Minecraft minecraft,
        GuiGraphicsExtractor graphics,
        float partialTick
    ) {
        int slotId = SlotScope.requireId();
        ClientRuntime runtime = ClientRuntime.INSTANCE;
        if (minecraft.gui.overlay() != null || !runtime.viewports().hasViewport(slotId)) {
            return;
        }

        UiInputModeController.updateCurrentClientFrame(minecraft);
        LocalClientSlot slot = runtime.slots().slot(slotId);
        ViewportArea viewport = runtime.viewports().viewport(slotId);
        Screen screen = slot.screenState().screen();

        if (screen == null && SecondaryWorldLoadingScreen.shouldPresent(slot)) {
            screen = SecondaryWorldLoadingScreen.forSlot(slotId);
            int width = ViewportGuiScale.logicalWidth(viewport);
            int height = ViewportGuiScale.logicalHeight(viewport);
            if (screen.width != width || screen.height != height) {
                screen.init(width, height);
            }
        }

        if (screen == null) {
            return;
        }

        Screen renderScreen = screen;
        int mouseX = logicalMouseX(minecraft, slotId, viewport);
        int mouseY = logicalMouseY(minecraft, slotId, viewport);
        LocalScreenInput.runEvent(
            () -> ClientHooks.extractScreen(
                renderScreen,
                new Stack<>(),
                graphics,
                mouseX,
                mouseY,
                partialTick
            ),
            "Extracting slot screen render state",
            renderScreen.getClass().getCanonicalName()
        );
        ControlHintPipeline.renderScreen(renderScreen, graphics);
        CursorRenderer.renderForCurrentClient(graphics, minecraft);
    }

    private static int logicalMouseX(
        Minecraft minecraft,
        int slotId,
        ViewportArea viewport
    ) {
        if (GamepadInputProcessor.isControllerCursorActive(slotId)) {
            return GamepadInputProcessor.resolveScreenMouseX(
                slotId,
                (int)Math.round(GamepadInputProcessor.cursorX(slotId))
            );
        }
        if (!InputApi.canPhysicalMouseDriveSlot(slotId)) {
            return Integer.MIN_VALUE;
        }
        int localGuiX = GamepadInputProcessor.resolveScreenMouseX(
            slotId,
            (int)viewport.windowMouseToLocalGuiX(minecraft.mouseHandler.xpos())
        );
        return ViewportGuiScale.logicalMouseX(viewport, localGuiX);
    }

    private static int logicalMouseY(
        Minecraft minecraft,
        int slotId,
        ViewportArea viewport
    ) {
        if (GamepadInputProcessor.isControllerCursorActive(slotId)) {
            return GamepadInputProcessor.resolveScreenMouseY(
                slotId,
                (int)Math.round(GamepadInputProcessor.cursorY(slotId))
            );
        }
        if (!InputApi.canPhysicalMouseDriveSlot(slotId)) {
            return Integer.MIN_VALUE;
        }
        int localGuiY = GamepadInputProcessor.resolveScreenMouseY(
            slotId,
            (int)viewport.windowMouseToLocalGuiY(minecraft.mouseHandler.ypos())
        );
        return ViewportGuiScale.logicalMouseY(viewport, localGuiY);
    }
}
