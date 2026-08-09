package net.jr.ClientRuntime.input;

import javax.annotation.Nullable;
import net.jr.ClientRuntime.runtime.Client;
import net.jr.ClientRuntime.runtime.ScreenScale;
import net.jr.ClientRuntime.viewport.ViewportArea;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;

/** Screen input after a high-level keyboard, mouse or gamepad boundary selected the Client. */
public final class ScreenInput {
    private ScreenInput() {
    }

    @Nullable
    public static Screen screen() {
        return Client.screen();
    }

    public static double localWindowX(MouseHandler mouseHandler) {
        return mouseHandler.xpos() - viewport().windowX();
    }

    public static double localWindowY(MouseHandler mouseHandler) {
        return mouseHandler.ypos() - viewport().windowY();
    }

    public static double localGuiX(MouseHandler mouseHandler) {
        ViewportArea viewport = viewport();
        return (mouseHandler.xpos() - viewport.windowX()) * guiWidth() / viewport.windowWidth();
    }

    public static double localGuiY(MouseHandler mouseHandler) {
        ViewportArea viewport = viewport();
        return (mouseHandler.ypos() - viewport.windowY()) * guiHeight() / viewport.windowHeight();
    }

    public static int guiWidth() {
        return ScreenScale.logicalWidth(viewport());
    }

    public static int guiHeight() {
        return ScreenScale.logicalHeight(viewport());
    }

    public static int windowWidth() {
        return viewport().windowWidth();
    }

    public static int windowHeight() {
        return viewport().windowHeight();
    }

    public static void runEvent(Runnable event, String errorTitle, String screenName) {
        try {
            event.run();
        } catch (Throwable throwable) {
            CrashReport report = CrashReport.forThrowable(throwable, errorTitle);
            CrashReportCategory category = report.addCategory("Screen details");
            category.setDetail("Screen name", screenName);
            throw new ReportedException(report);
        }
    }

    private static ViewportArea viewport() {
        return Client.viewport();
    }
}
