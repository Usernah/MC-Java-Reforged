package net.jr.client.runtime.viewport;

import javax.annotation.Nullable;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.ui.render.composition.GuiCompositionScope;
import net.jr.client.ui.render.composition.ScreenFramebufferSpace;
import net.minecraft.client.gui.screens.Screen;

public final class ViewportGuiDimensions {
    private ViewportGuiDimensions() {
    }

    @Nullable
    public static Integer activeWidthOrNull() {
        GuiCompositionScope.Extraction composition = GuiCompositionScope.activeExtractionOrNull();
        if (composition != null) {
            return composition.width();
        }
        Integer screenWidth = ViewportGuiScale.activeGuiWidthOrNull();
        if (screenWidth != null) {
            return screenWidth;
        }
        ViewportArea viewport = ViewportRenderScope.activeViewportOrNull();
        return viewport != null ? viewport.guiWidth() : null;
    }

    @Nullable
    public static Integer activeHeightOrNull() {
        GuiCompositionScope.Extraction composition = GuiCompositionScope.activeExtractionOrNull();
        if (composition != null) {
            return composition.height();
        }
        Integer screenHeight = ViewportGuiScale.activeGuiHeightOrNull();
        if (screenHeight != null) {
            return screenHeight;
        }
        ViewportArea viewport = ViewportRenderScope.activeViewportOrNull();
        return viewport != null ? viewport.guiHeight() : null;
    }


    public static int width(ViewportArea viewport) {
        Integer activeWidth = activeWidthOrNull();
        if (activeWidth != null) {
            return activeWidth;
        }

        Screen screen = ClientRuntime.INSTANCE.slots()
            .slot(viewport.id())
            .screenState()
            .screen();
        return screen == null
            ? viewport.logicalGuiWidth()
            : ScreenFramebufferSpace.width(
                screen,
                viewport,
                ViewportGuiScale.screenLogicalWidth(viewport)
            );
    }


    public static int height(ViewportArea viewport) {
        Integer activeHeight = activeHeightOrNull();
        if (activeHeight != null) {
            return activeHeight;
        }

        Screen screen = ClientRuntime.INSTANCE.slots()
            .slot(viewport.id())
            .screenState()
            .screen();
        return screen == null
            ? viewport.logicalGuiHeight()
            : ScreenFramebufferSpace.height(
                screen,
                viewport,
                ViewportGuiScale.screenLogicalHeight(viewport)
            );
    }
}
