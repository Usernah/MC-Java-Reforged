package net.jr.client.compat.xaero;

import com.mojang.blaze3d.platform.Window;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.input.LocalGamepadInputHandler;
import net.jr.client.runtime.input.LocalScreenInput;
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
        Integer slotId = SlotScope.idOrNull();

        if (viewport == null || slotId == null) {
            return null;
        }

        int logicalX = LocalGamepadInputHandler.resolveScreenMouseX(
                slotId,
                (int)Math.round(LocalScreenInput.localGuiX(minecraft.mouseHandler))
        );

        if (logicalX == Integer.MIN_VALUE) {
            return (double)Integer.MIN_VALUE;
        }

        return logicalX
                * viewport.glWidth()
                / (double)LocalScreenInput.guiWidth();
    }

    public static Double mousePixelYOrNull(Minecraft minecraft) {
        ViewportArea viewport = XaeroViewport.currentOrNull();
        Integer slotId = SlotScope.idOrNull();

        if (viewport == null || slotId == null) {
            return null;
        }

        int logicalY = LocalGamepadInputHandler.resolveScreenMouseY(
                slotId,
                (int)Math.round(LocalScreenInput.localGuiY(minecraft.mouseHandler))
        );

        if (logicalY == Integer.MIN_VALUE) {
            return (double)Integer.MIN_VALUE;
        }

        return logicalY
                * viewport.glHeight()
                / (double)LocalScreenInput.guiHeight();
    }
}