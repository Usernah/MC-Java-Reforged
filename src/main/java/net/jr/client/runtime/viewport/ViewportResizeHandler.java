package net.jr.client.runtime.viewport;

import com.mojang.blaze3d.platform.Window;
import java.util.Objects;
import net.jr.api.client.split.SplitOrientation;
import net.jr.client.runtime.ui.LocalScreenManager;
import net.minecraft.client.Minecraft;

public final class ViewportResizeHandler {
    private final ViewportManager viewports;

    public ViewportResizeHandler(ViewportManager viewports) {
        this.viewports = Objects.requireNonNull(viewports, "viewports");
    }

    public void refreshWindow(Minecraft minecraft) {
        Objects.requireNonNull(minecraft, "minecraft");
        Window window = minecraft.getWindow();
        if (window == null) {
            return;
        }

        int windowWidth = window.getScreenWidth();
        int windowHeight = window.getScreenHeight();
        int framebufferWidth = window.getWidth();
        int framebufferHeight = window.getHeight();
        int guiWidth = window.getGuiScaledWidth();
        int guiHeight = window.getGuiScaledHeight();
        double guiScale = window.getGuiScale();

        if (!valid(windowWidth)
            || !valid(windowHeight)
            || !valid(framebufferWidth)
            || !valid(framebufferHeight)
            || !valid(guiWidth)
            || !valid(guiHeight)
            || guiScale <= 0.0D
            || Double.isNaN(guiScale)
            || Double.isInfinite(guiScale)
        ) {
            return;
        }

        WindowMetrics metrics = new WindowMetrics(
            windowWidth,
            windowHeight,
            framebufferWidth,
            framebufferHeight,
            guiWidth,
            guiHeight,
            (int)Math.max(1.0D, Math.round(guiScale)),
            guiScale
        );

        if (this.viewports.hasWindowMetrics() && metrics.equals(this.viewports.windowMetrics())) {
            return;
        }

        this.viewports.rebuild(metrics);
        LocalScreenManager.resizeAll(minecraft);
    }

    public void refreshViewportOptions(Minecraft minecraft) {
        Objects.requireNonNull(minecraft, "minecraft");
        if (!this.viewports.hasWindowMetrics()) {
            this.refreshWindow(minecraft);
            return;
        }
        this.viewports.rebuild(this.viewports.windowMetrics());
        LocalScreenManager.resizeAll(minecraft);
    }

    public void setTwoPlayerOrientation(Minecraft minecraft, SplitOrientation orientation) {
        Objects.requireNonNull(minecraft, "minecraft");
        Objects.requireNonNull(orientation, "orientation");
        this.viewports.setTwoPlayerOrientation(orientation);
        if (this.viewports.hasWindowMetrics()) {
            this.applyLayoutTransition(minecraft);
        }
    }

    public void applyLayoutTransition(Minecraft minecraft) {
        Objects.requireNonNull(minecraft, "minecraft");
        minecraft.resizeGui();
        this.refreshWindow(minecraft);
        if (this.viewports.hasWindowMetrics()) {
            this.viewports.rebuild(this.viewports.windowMetrics());
        }
        LocalScreenManager.resizeAll(minecraft, true);
    }

    private static boolean valid(int value) {
        return value > 0;
    }
}
