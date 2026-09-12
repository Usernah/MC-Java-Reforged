package net.jr.client.ui.hud;

import java.util.Locale;
import net.jr.client.runtime.client.LocalClientReadinessPolicy;
import net.jr.client.runtime.context.LocalClientScope;
import net.jr.client.runtime.render.experimental.ExperimentalRenderMetrics;
import net.jr.client.runtime.render.experimental.FrameHardwareMetrics;
import net.jr.server.chunk.LegacyChunkAdmissionLimiter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Compact replacement for Render Lab, drawn after F3 in the primary viewport. */
public final class ExperimentalRenderMetricsOverlay {
    private static FrameHardwareMetrics.Snapshot displayed;
    private static String[] lines = new String[0];

    private ExperimentalRenderMetricsOverlay() {
    }

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        var client = LocalClientScope.currentClientOrNull();
        if (client == null || client.slotId() != 0 || !LocalClientReadinessPolicy.gameplayReady(client)
            || client.screen() != null || !minecraft.debugEntries.isOverlayVisible()) {
            return;
        }
        var frame = FrameHardwareMetrics.snapshot();
        if (frame != displayed) {
            displayed = frame;
            var terrain = ExperimentalRenderMetrics.snapshot().current();
            var admissions = LegacyChunkAdmissionLimiter.snapshot();
            String gpu = frame.gpuAgeFrames() < 0 ? "GPU: esperando muestra"
                : frame.gpuAgeFrames() > 120 ? "GPU: sin resultado reciente"
                : format("GPU media %s | ult %s (-%df)", ms(frame.gpuMs()), ms(frame.lastGpuMs()), frame.gpuAgeFrames());
            lines = new String[] {
                format("JR PERF | %d/600 frames | render %s", frame.samples(), terrain.enabled() ? "ON" : "OFF"),
                format("Frame media %s | P95 %s", ms(frame.averageMs()), ms(frame.p95Ms())),
                format("Ult %s | max %s | >50ms: %d", ms(frame.lastMs()), ms(frame.maxMs()), frame.hitches()),
                format("CPU hilo principal: %s (media)", ms(frame.cpuMs())),
                format("Antes del render: %s (media)", ms(frame.beforeRenderMs())),
                gpu,
                frame.gcCount() < 0 ? "GC: esperando intervalo"
                    : format("GC/%.1fs: %d | %d ms de colector", frame.gcSeconds(), frame.gcCount(), frame.gcMs()),
                format("Heap: %d / %d MiB", frame.heapMiB(), frame.maxHeapMiB()),
                format("Cola chunks: %d | mallas: %d", admissions.pendingRequests(), terrain.queueDepth()),
                terrain.warmupFrames() > 0 ? "Terreno: calentando muestras"
                    : format("Subida mallas: %s | compila/f: %.1f", ms(terrain.averageTerrainUploadMs()), terrain.averageCompiledSections()),
                "Frame incluye esperas | GPU diferida"
            };
        }

        var font = minecraft.font;
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, font.width(line));
        }
        int lineHeight = font.lineHeight + 1;
        int panelWidth = width + 8;
        int panelHeight = lines.length * lineHeight + 8;
        float scale = Math.min(1.0f, Math.min(
            Math.max(1, graphics.guiWidth() - 8) / (float)panelWidth,
            Math.max(1, graphics.guiHeight() - 32) / (float)panelHeight
        ));
        int x = Math.max(4, graphics.guiWidth() - (int)Math.ceil(panelWidth * scale) - 4);
        graphics.nextStratum();
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(x, 4);
            graphics.pose().scale(scale, scale);
            graphics.fill(0, 0, panelWidth, panelHeight, 0xFF101010);
            for (int i = 0; i < lines.length; i++) {
                int color = i == 0 ? 0xFF80FF80 : i == lines.length - 1 ? 0xFFA0A0A0 : 0xFFE0E0E0;
                graphics.text(font, lines[i], 4, 4 + i * lineHeight, color, false);
            }
        } finally {
            graphics.pose().popMatrix();
        }
    }

    private static String ms(double value) {
        return value < 0 ? "--" : format("%.1fms", value);
    }

    private static String format(String pattern, Object... args) {
        return String.format(Locale.ROOT, pattern, args);
    }
}
