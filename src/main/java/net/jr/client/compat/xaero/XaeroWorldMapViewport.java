package net.jr.client.compat.xaero;

import com.mojang.blaze3d.platform.Window;
import net.jr.api.client.ui.Viewport;
import net.jr.client.runtime.context.SlotExecution;
import net.jr.client.runtime.input.GamepadInputHandler;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.runtime.viewport.ViewportGuiScale;
import net.minecraft.client.Minecraft;

public final class XaeroWorldMapViewport {
    private XaeroWorldMapViewport() {
    }

    public static int framebufferWidth(Window window) {
        ViewportArea viewport = XaeroViewport.currentOrNull();
        return viewport == null ? window.getWidth() : viewport.glWidth();
    }

    public static int framebufferHeight(Window window) {
        ViewportArea viewport = XaeroViewport.currentOrNull();
        return viewport == null ? window.getHeight() : viewport.glHeight();
    }

    public static int screenWidth(Window window) {
        ViewportArea viewport = XaeroViewport.currentOrNull();
        return viewport == null ? window.getScreenWidth() : viewport.windowWidth();
    }

    public static int screenHeight(Window window) {
        ViewportArea viewport = XaeroViewport.currentOrNull();
        return viewport == null ? window.getScreenHeight() : viewport.windowHeight();
    }

    public static int guiWidth(Window window) {
        ViewportArea viewport = XaeroViewport.currentOrNull();
        return viewport == null
                ? window.getGuiScaledWidth()
                : ViewportGuiScale.logicalWidth(viewport);
    }

    public static int guiHeight(Window window) {
        ViewportArea viewport = XaeroViewport.currentOrNull();
        return viewport == null
                ? window.getGuiScaledHeight()
                : ViewportGuiScale.logicalHeight(viewport);
    }

    public static int guiScale(Window window) {
        ViewportArea viewport = XaeroViewport.currentOrNull();
        return viewport == null ? window.getGuiScale() : viewport.guiScale();
    }

    public static Double mousePixelXOrNull(Minecraft minecraft) {


        ViewportArea viewport = XaeroViewport.currentOrNull();
        Integer slotId = SlotExecution.currentSlotIdOrNull();

        if (viewport == null || slotId == null) {
            return null;
        }

        int logicalX = GamepadInputHandler.resolveScreenMouseX(
                slotId,
                (int)Math.round(Viewport.current().windowMouseToLocalPixelX(minecraft.mouseHandler.xpos()))
        );

        if (logicalX == Integer.MIN_VALUE) {
            return (double)Integer.MIN_VALUE;
        }

        return (double) logicalX;
    }

    public static Double mousePixelYOrNull(Minecraft minecraft) {
        ViewportArea viewport = XaeroViewport.currentOrNull();
        Integer slotId = SlotExecution.currentSlotIdOrNull();

        if (viewport == null || slotId == null) {
            return null;
        }

        int logicalY = GamepadInputHandler.resolveScreenMouseY(
                slotId,
                (int)Math.round(Viewport.current().windowMouseToLocalPixelY(minecraft.mouseHandler.ypos()))
        );

        if (logicalY == Integer.MIN_VALUE) {
            return (double)Integer.MIN_VALUE;
        }

        return (double) logicalY;
    }
}
