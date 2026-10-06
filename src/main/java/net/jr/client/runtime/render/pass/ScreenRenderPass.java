package net.jr.client.runtime.render.pass;

import java.util.Stack;
import net.jr.client.input.InputApi;
import net.jr.client.runtime.input.GamepadInputHandler;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.slot.Slots;
import net.jr.client.runtime.ui.ScreenEventRunner;
import net.jr.screens.SecondaryWorldLoadingScreen;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.runtime.viewport.ViewportGuiScale;
import net.jr.client.ui.navigation.UiInputModeController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.client.ClientHooks;

public final class ScreenRenderPass {
    private ScreenRenderPass() {
    }

    @Nullable
    public static Screen extract(
        Minecraft minecraft,
        GuiGraphicsExtractor graphics,
        float partialTick
    ) {
        int slotId = SlotScope.requireId();
        ClientRuntime runtime = ClientRuntime.INSTANCE;
        if (minecraft.gui.overlay() != null || !runtime.viewports().hasViewport(slotId)) {
            return null;
        }

        UiInputModeController.updateCurrentClientFrame(minecraft);
        Slots slot = runtime.slots().slot(slotId);
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
            return null;
        }

        Screen renderScreen = screen;
        int mouseX = logicalMouseX(minecraft, slotId, viewport);
        int mouseY = logicalMouseY(minecraft, slotId, viewport);
        ScreenEventRunner.run(
                renderScreen,
                "Extracting slot screen render state",
                () -> ClientHooks.extractScreen(
                        renderScreen,
                        new Stack<>(),
                        graphics,
                        mouseX,
                        mouseY,
                        partialTick
                )
        );

        GamepadInputHandler.trackFocusedCursorDuringScroll(renderScreen);
        ControlHintRenderPass.renderScreen(renderScreen, graphics);
        return renderScreen;
    }

    private static int logicalMouseX(
        Minecraft minecraft,
        int slotId,
        ViewportArea viewport
    ) {
        if (GamepadInputHandler.isControllerCursorActive(slotId)) {
            return GamepadInputHandler.resolveScreenMouseX(
                slotId,
                (int)Math.round(GamepadInputHandler.cursorX(slotId))
            );
        }
        if (!InputApi.canPhysicalMouseDriveSlot(slotId)) {
            return Integer.MIN_VALUE;
        }
        int localGuiX = GamepadInputHandler.resolveScreenMouseX(
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
        if (GamepadInputHandler.isControllerCursorActive(slotId)) {
            return GamepadInputHandler.resolveScreenMouseY(
                slotId,
                (int)Math.round(GamepadInputHandler.cursorY(slotId))
            );
        }
        if (!InputApi.canPhysicalMouseDriveSlot(slotId)) {
            return Integer.MIN_VALUE;
        }
        int localGuiY = GamepadInputHandler.resolveScreenMouseY(
            slotId,
            (int)viewport.windowMouseToLocalGuiY(minecraft.mouseHandler.ypos())
        );
        return ViewportGuiScale.logicalMouseY(viewport, localGuiY);
    }
}
