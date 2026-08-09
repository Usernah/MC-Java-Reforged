package net.alnv.javareforged.ClientRuntime.runtime;

import com.mojang.blaze3d.systems.RenderSystem;
import net.alnv.javareforged.ClientRuntime.viewport.ViewportArea;
import net.minecraft.client.Minecraft;

public final class ScreenScissors {
    private ScreenScissors() {
    }

    public static void enable(int x, int y, int width, int height) {
        ViewportArea viewport = ViewportPass.activeViewportOrNull();
        if (viewport == null) {
            RenderSystem.enableScissor(x, y, width, height);
            return;
        }

        ScreenScale.Context screenScale = ScreenScale.activeOrNull();
        if (screenScale != null) {
            enableScaledScreenScissor(viewport, x, y, width, height);
            return;
        }

        int windowHeight = Minecraft.getInstance().getWindow().getHeight();
        int localBottomPixels = windowHeight - y;
        int viewportX = viewport.glX() + x;
        int viewportY = viewport.glY() + viewport.glHeight() - localBottomPixels;

        RenderSystem.enableScissor(
                viewportX,
                viewportY,
                Math.max(0, width),
                Math.max(0, height)
        );
    }

    private static void enableScaledScreenScissor(
            ViewportArea viewport,
            int x,
            int y,
            int width,
            int height
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        double sourceGuiScale = minecraft.getWindow().getGuiScale();
        double targetGuiScale = ScreenScale.effectiveGuiScale(viewport);
        int windowHeight = minecraft.getWindow().getHeight();

        int viewportX = viewport.glX() + scaledFloor(x, sourceGuiScale, targetGuiScale);
        int localBottomPixels = scaledCeil(windowHeight - y, sourceGuiScale, targetGuiScale);
        int viewportY = viewport.glY() + viewport.glHeight() - localBottomPixels;
        int viewportWidth = scaledCeil(width, sourceGuiScale, targetGuiScale);
        int viewportHeight = scaledCeil(height, sourceGuiScale, targetGuiScale);

        RenderSystem.enableScissor(
                viewportX,
                viewportY,
                Math.max(0, viewportWidth),
                Math.max(0, viewportHeight)
        );
    }

    private static int scaledFloor(int value, double sourceGuiScale, double targetGuiScale) {
        return (int)Math.floor(value / sourceGuiScale * targetGuiScale);
    }

    private static int scaledCeil(int value, double sourceGuiScale, double targetGuiScale) {
        return (int)Math.ceil(value / sourceGuiScale * targetGuiScale);
    }

    public static void disableOrRestoreViewport() {
        ViewportArea viewport = ViewportPass.activeViewportOrNull();
        if (viewport == null) {
            RenderSystem.disableScissor();
            return;
        }

        RenderSystem.enableScissor(
                viewport.glX(),
                viewport.glY(),
                viewport.glWidth(),
                viewport.glHeight()
        );
    }
}
