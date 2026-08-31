package net.jr.client.ui.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.jr.client.runtime.context.LocalClientScope;
import net.jr.client.runtime.render.experimental.ExperimentalRenderMetrics;
import net.jr.client.runtime.render.experimental.TerrainMultiDraw;
import net.jr.client.runtime.terrain.TerrainCompileBudget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;

public final class ExperimentalRenderMetricsOverlay {
    private static final int PADDING = 3;
    private static final int BACKGROUND = 0xC0101010;
    private static final int TEXT = 0xFFE0E0E0;
    private static final int ACTIVE_TEXT = 0xFF80FF80;
    private static final int BASELINE_TEXT = 0xFFFFD080;

    private ExperimentalRenderMetricsOverlay() {
    }

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        var client = LocalClientScope.currentClientOrNull();
        if (client == null || client.slotId() != 0 || !minecraft.debugEntries.isOverlayVisible()) {
            return;
        }

        ExperimentalRenderMetrics.MetricsSnapshot metrics = ExperimentalRenderMetrics.snapshot();
        ExperimentalRenderMetrics.RunSnapshot current = metrics.current();
        List<Line> lines = new ArrayList<>();
        lines.add(new Line("JR Render Lab  " + (current.enabled() ? "ON" : "OFF") + "  gen " + current.generation(), current.enabled() ? ACTIVE_TEXT : BASELINE_TEXT));
        if (current.warmupFrames() > 0) {
            lines.add(new Line("Warmup: " + current.warmupFrames() + " frames", TEXT));
        } else {
            lines.add(new Line("Samples: " + current.samples() + "/600", TEXT));
        }
        lines.add(new Line(String.format(Locale.ROOT, "Frame %.2f ms | P95 %.2f | P99 %.2f", current.averageFrameMs(), current.p95FrameMs(), current.p99FrameMs()), TEXT));
        lines.add(new Line(String.format(Locale.ROOT, "Extract %.2f | Slots %.2f ms", current.averageExtractionMs(), current.averageSlotRenderMs()), TEXT));
        lines.add(new Line(String.format(Locale.ROOT, "Terrain %.2f | Draw prep %.2f | Upload %.2f ms", current.averageTerrainSchedulingMs(), current.averageDrawPreparationMs(), current.averageTerrainUploadMs()), TEXT));
        lines.add(new Line(String.format(Locale.ROOT, "Sections %.0f | Draws %.0f | Queue %d", current.averageVisibleSections(), current.averageDraws(), current.queueDepth()), TEXT));
        lines.add(new Line(String.format(Locale.ROOT, "Compiled/f %.2f | Staging pressure/f %.2f", current.averageCompiledSections(), current.averageStagingRetries()), TEXT));
        if (current.enabled()) {
            var budget = TerrainCompileBudget.snapshot();
            lines.add(new Line(String.format(
                Locale.ROOT,
                "Budget %d | Dispatch %.2f/f | Deferred %d",
                budget.budget(),
                budget.dispatchedPerFrame(),
                budget.deferredTasks()
            ), TEXT));
            lines.add(new Line(String.format(
                Locale.ROOT,
                "Coalesced %.2f duplicate requests/f",
                budget.coalescedPerFrame()
            ), TEXT));
            var multiDraw = TerrainMultiDraw.snapshot();
            lines.add(new Line(String.format(
                Locale.ROOT,
                "MDI %d draws -> %d submits | B %d | F %d | C %d/%d (%s)",
                multiDraw.logicalDraws(),
                multiDraw.submits(),
                multiDraw.batches(),
                multiDraw.fallbacks(),
                multiDraw.cacheHits(),
                multiDraw.cacheRebuilds(),
                multiDraw.status()
            ), TEXT));
            var scheduler = metrics.scheduler();
            lines.add(new Line(String.format(
                Locale.ROOT,
                "Priority P1 %d | P2 %d | P3 %d | P4 %d | F %d",
                scheduler.slot0(),
                scheduler.slot1(),
                scheduler.slot2(),
                scheduler.slot3(),
                scheduler.fallbacks()
            ), TEXT));
        }

        ExperimentalRenderMetrics.RunSnapshot comparison = metrics.comparison();
        if (comparison.samples() > 0) {
            lines.add(new Line(String.format(Locale.ROOT, "Last %s: %.2f ms | P95 %.2f | P99 %.2f", comparison.enabled() ? "ON" : "OFF", comparison.averageFrameMs(), comparison.p95FrameMs(), comparison.p99FrameMs()), 0xFFB0B0B0));
        }

        draw(graphics, minecraft.font, lines);
    }

    private static void draw(GuiGraphicsExtractor graphics, Font font, List<Line> lines) {
        int width = 0;
        for (Line line : lines) {
            width = Math.max(width, font.width(line.text()));
        }
        int lineHeight = font.lineHeight + 1;
        int height = lines.size() * lineHeight;
        int x = Math.max(PADDING, graphics.guiWidth() - width - PADDING * 2);
        int y = Math.max(PADDING, graphics.guiHeight() - height - PADDING * 2);
        graphics.fill(x - PADDING, y - PADDING, x + width + PADDING, y + height + PADDING, BACKGROUND);
        for (int index = 0; index < lines.size(); index++) {
            Line line = lines.get(index);
            graphics.text(font, line.text(), x, y + index * lineHeight, line.color(), false);
        }
    }

    private record Line(String text, int color) {
    }
}
