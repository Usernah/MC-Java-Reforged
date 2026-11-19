package net.jr.client.runtime.ui;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.gui.screens.Screen;

public final class ScreenEventRunner {
    private ScreenEventRunner() {
    }

    public static void run(
            Screen screen,
            String errorTitle,
            Runnable event
    ) {
        try {
            event.run();
        } catch (Throwable throwable) {
            CrashReport report =
                    CrashReport.forThrowable(throwable, errorTitle);

            CrashReportCategory category =
                    report.addCategory("Screen details");

            category.setDetail(
                    "Screen name",
                    screen.getClass().getCanonicalName()
            );

            throw new ReportedException(report);
        }
    }
}
