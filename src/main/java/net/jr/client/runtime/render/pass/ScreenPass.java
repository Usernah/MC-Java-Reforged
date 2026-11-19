package net.jr.client.runtime.render.pass;

import java.util.Stack;
import net.jr.client.input.InputApi;
import net.jr.client.runtime.input.GamepadInputHandler;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.slot.Slots;
import net.jr.client.runtime.ui.ScreenEventRunner;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.ui.navigation.UiInputModeController;
import net.jr.client.ui.render.composition.ScreenFramebufferSpace;
import net.jr.api.client.systemui.SystemUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.client.ClientHooks;

public final class ScreenPass {
    private ScreenPass() {
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

        if (!SystemUI.blocksSlot(slotId)) {
            UiInputModeController.updateCurrentClientFrame(minecraft);
        }
        Slots slot = runtime.slots().slot(slotId);
        ViewportArea viewport = runtime.viewports().viewport(slotId);
        Screen screen = slot.screenState().screen();

        if (screen == null) {
            return null;
        }

        int mouseX = mouseX(minecraft, slotId, viewport, screen);
        int mouseY = mouseY(minecraft, slotId, viewport, screen);
        extractKnown(minecraft, screen, graphics, mouseX, mouseY, partialTick);
        return screen;
    }

    static void extractKnown(
        Minecraft minecraft,
        Screen screen,
        GuiGraphicsExtractor graphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        ScreenEventRunner.run(
                screen,
                "Extracting slot screen render state",
                () -> ClientHooks.extractScreen(
                        screen,
                        new Stack<>(),
                        graphics,
                        mouseX,
                        mouseY,
                        partialTick
                )
        );

        GamepadInputHandler.trackFocusedCursorDuringScroll(screen);
    }

    static int mouseX(
        Minecraft minecraft,
        int slotId,
        ViewportArea viewport,
        Screen screen
    ) {
        if (SystemUI.blocksSlot(slotId)) {
            return Integer.MIN_VALUE;
        }
        if (GamepadInputHandler.isControllerCursorActive(slotId)) {
            return GamepadInputHandler.resolveScreenMouseX(
                slotId,
                (int)Math.round(GamepadInputHandler.cursorX(slotId))
            );
        }
        if (!InputApi.canPhysicalMouseDriveSlot(slotId)) {
            return Integer.MIN_VALUE;
        }
        return GamepadInputHandler.resolveScreenMouseX(
            slotId,
            (int)Math.floor(ScreenFramebufferSpace.windowMouseX(
                screen, viewport, minecraft.mouseHandler.xpos()
            ))
        );
    }

    static int mouseY(
        Minecraft minecraft,
        int slotId,
        ViewportArea viewport,
        Screen screen
    ) {
        if (SystemUI.blocksSlot(slotId)) {
            return Integer.MIN_VALUE;
        }
        if (GamepadInputHandler.isControllerCursorActive(slotId)) {
            return GamepadInputHandler.resolveScreenMouseY(
                slotId,
                (int)Math.round(GamepadInputHandler.cursorY(slotId))
            );
        }
        if (!InputApi.canPhysicalMouseDriveSlot(slotId)) {
            return Integer.MIN_VALUE;
        }
        return GamepadInputHandler.resolveScreenMouseY(
            slotId,
            (int)Math.floor(ScreenFramebufferSpace.windowMouseY(
                screen, viewport, minecraft.mouseHandler.ypos()
            ))
        );
    }
}
