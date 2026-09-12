package net.jr.client.runtime.render.experimental;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuQueryPool;
import com.mojang.blaze3d.systems.RenderSystem;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;

/** Render-thread diagnostics. GPU results are polled, never waited for. */
public final class FrameHardwareMetrics {
    private static final int WINDOW = 600;
    private static final int GPU_SLOTS = 4;
    private static final ThreadMXBean THREADS = ManagementFactory.getThreadMXBean();
    private static final List<GarbageCollectorMXBean> COLLECTORS = ManagementFactory.getGarbageCollectorMXBeans();
    private static final double[] FRAMES = new double[WINDOW];
    private static final double[] CPU = new double[WINDOW];
    private static final double[] BEFORE_RENDER = new double[WINDOW];
    private static final double[] SORTED = new double[WINDOW];
    private static final double[] GPU = new double[WINDOW];
    private static final boolean[] GPU_PENDING = new boolean[GPU_SLOTS];
    private static final long[] GPU_GENERATION = new long[GPU_SLOTS];
    private static final long[] GPU_FRAME = new long[GPU_SLOTS];
    private static Object world;
    private static boolean active;
    private static boolean cpuChecked;
    private static boolean cpuAvailable;
    private static long generation;
    private static long frameNumber;
    private static long frameStart;
    private static long cpuStart;
    private static long renderStart;
    private static long lastPublish;
    private static int cursor;
    private static int count;
    private static int gpuCursor;
    private static int gpuCount;
    private static long lastGpuFrame;
    private static double lastGpuMs;
    private static GpuQueryPool queryPool;
    private static CommandEncoder queryEncoder;
    private static int recordingGpu = -1;
    private static long gcStart;
    private static long gcCountBaseline;
    private static long gcTimeBaseline;
    private static long gcCount = -1;
    private static long gcTime = -1;
    private static double gcInterval;
    private static Snapshot snapshot = Snapshot.EMPTY;

    private FrameHardwareMetrics() {
    }

    public static void beginFrame(Minecraft minecraft) {
        boolean enabled = minecraft.level != null && minecraft.debugEntries.isOverlayVisible();
        Object currentWorld = enabled ? minecraft.level : null;
        if (active != enabled || world != currentWorld) {
            active = enabled;
            world = currentWorld;
            reset();
        }
        if (!active) {
            return;
        }
        if (!cpuChecked) {
            cpuChecked = true;
            try {
                if (THREADS.isCurrentThreadCpuTimeSupported()) {
                    THREADS.setThreadCpuTimeEnabled(true);
                    cpuAvailable = THREADS.isThreadCpuTimeEnabled();
                }
            } catch (UnsupportedOperationException | SecurityException ignored) {
                cpuAvailable = false;
            }
        }
        frameNumber++;
        renderStart = 0;
        frameStart = System.nanoTime();
        cpuStart = cpuAvailable ? THREADS.getCurrentThreadCpuTime() : -1;
    }

    public static void beforeRender() {
        if (active) {
            renderStart = System.nanoTime();
        }
    }

    public static void endFrame() {
        if (!active) {
            return;
        }
        long now = System.nanoTime();
        long cpuEnd = cpuAvailable ? THREADS.getCurrentThreadCpuTime() : -1;
        // Whole client iteration: ticks, packets, sound, rendering, present and FPS limiter.
        // Thread CPU time separately excludes sleeping/waiting; it is not total process CPU.
        FRAMES[cursor] = (now - frameStart) / 1_000_000.0;
        CPU[cursor] = cpuStart >= 0 && cpuEnd >= cpuStart ? (cpuEnd - cpuStart) / 1_000_000.0 : -1;
        BEFORE_RENDER[cursor] = ((renderStart == 0 ? now : renderStart) - frameStart) / 1_000_000.0;
        cursor = (cursor + 1) % WINDOW;
        count = Math.min(count + 1, WINDOW);
        if (now - lastPublish >= 250_000_000L) {
            publish(now);
            lastPublish = now;
        }
    }

    public static void beginGpu() {
        if (!active || recordingGpu >= 0) {
            return;
        }
        // Same timestamp API as vanilla TimerQuery; spans all viewports and the GUI,
        // excluding present. Timeline gaps can be included, so this is not GPU utilization.
        if (queryPool == null) {
            queryPool = RenderSystem.getDevice().createTimestampQueryPool(GPU_SLOTS * 2);
        }
        for (int i = 0; i < GPU_SLOTS; i++) {
            if (!GPU_PENDING[i]) {
                continue;
            }
            var start = queryPool.getValue(i * 2);
            var end = queryPool.getValue(i * 2 + 1);
            if (start.isEmpty() || end.isEmpty()) {
                continue;
            }
            GPU_PENDING[i] = false;
            long elapsed = end.getAsLong() - start.getAsLong();
            if (GPU_GENERATION[i] == generation && elapsed >= 0) {
                double ms = elapsed * (double)RenderSystem.getDevice().getDeviceInfo().timestampPeriod() / 1_000_000.0;
                GPU[gpuCursor] = ms;
                gpuCursor = (gpuCursor + 1) % WINDOW;
                gpuCount = Math.min(gpuCount + 1, WINDOW);
                if (GPU_FRAME[i] >= lastGpuFrame) {
                    lastGpuMs = ms;
                    lastGpuFrame = GPU_FRAME[i];
                }
            }
        }
        for (int i = 0; i < GPU_SLOTS; i++) {
            if (!GPU_PENDING[i]) {
                recordingGpu = i;
                GPU_GENERATION[i] = generation;
                GPU_FRAME[i] = frameNumber;
                queryEncoder = RenderSystem.getDevice().createCommandEncoder();
                queryEncoder.writeTimestamp(queryPool, i * 2);
                return;
            }
        }
        // A slow GPU must not make instrumentation block or grow an unbounded queue.
    }

    public static void endGpu() {
        if (recordingGpu >= 0) {
            queryEncoder.writeTimestamp(queryPool, recordingGpu * 2 + 1);
            GPU_PENDING[recordingGpu] = true;
            recordingGpu = -1;
            queryEncoder = null;
        }
    }

    private static void reset() {
        generation++;
        cursor = count = gpuCursor = gpuCount = 0;
        frameNumber = lastGpuFrame = lastPublish = 0;
        snapshot = Snapshot.EMPTY;
        gcCount = gcTime = -1;
        gcInterval = 0;
        gcStart = 0;
        // Pending queries keep their generation until they can safely be reused.
    }

    private static void sampleGc(long now) {
        long collections = 0;
        long time = 0;
        boolean supported = false;
        for (var collector : COLLECTORS) {
            long n = collector.getCollectionCount();
            long ms = collector.getCollectionTime();
            if (n >= 0 && ms >= 0) {
                supported = true;
                collections += n;
                time += ms;
            }
        }
        if (gcStart != 0 && now - gcStart < 1_000_000_000L) {
            return;
        }
        if (supported && gcStart != 0) {
            gcCount = Math.max(0, collections - gcCountBaseline);
            gcTime = Math.max(0, time - gcTimeBaseline);
            gcInterval = (now - gcStart) / 1_000_000_000.0;
        }
        gcCountBaseline = collections;
        gcTimeBaseline = time;
        gcStart = now;
    }

    private static void publish(long now) {
        sampleGc(now);
        double total = 0;
        double cpu = 0;
        double before = 0;
        int cpuSamples = 0;
        int hitches = 0;
        for (int i = 0; i < count; i++) {
            total += FRAMES[i];
            before += BEFORE_RENDER[i];
            if (CPU[i] >= 0) {
                cpu += CPU[i];
                cpuSamples++;
            }
            if (FRAMES[i] > 50) {
                hitches++;
            }
            SORTED[i] = FRAMES[i];
        }
        Arrays.sort(SORTED, 0, count);
        double gpuTotal = 0;
        for (int i = 0; i < gpuCount; i++) {
            gpuTotal += GPU[i];
        }
        Runtime runtime = Runtime.getRuntime();
        snapshot = new Snapshot(count, total / count, FRAMES[(cursor + WINDOW - 1) % WINDOW],
            SORTED[(int)Math.ceil(count * 0.95) - 1], SORTED[count - 1], hitches,
            cpuSamples == 0 ? -1 : cpu / cpuSamples, before / count,
            gpuCount == 0 ? -1 : gpuTotal / gpuCount, gpuCount == 0 ? -1 : lastGpuMs,
            gpuCount == 0 ? -1 : frameNumber - lastGpuFrame, gcCount, gcTime, gcInterval,
            (runtime.totalMemory() - runtime.freeMemory()) / 1_048_576L, runtime.maxMemory() / 1_048_576L);
    }

    public static Snapshot snapshot() {
        return snapshot;
    }

    public static void close() {
        active = false;
        world = null;
        reset();
        if (queryPool != null) {
            queryPool.close();
            queryPool = null;
        }
        queryEncoder = null;
        recordingGpu = -1;
        Arrays.fill(GPU_PENDING, false);
    }

    /** GC milliseconds are collector accounting, not an exact stop-the-world pause measurement. */
    public record Snapshot(int samples, double averageMs, double lastMs, double p95Ms, double maxMs,
                           int hitches, double cpuMs, double beforeRenderMs, double gpuMs,
                           double lastGpuMs, long gpuAgeFrames, long gcCount, long gcMs, double gcSeconds,
                           long heapMiB, long maxHeapMiB) {
        private static final Snapshot EMPTY = new Snapshot(0, 0, 0, 0, 0, 0, -1, 0, -1, -1, -1, -1, -1, 0, 0, 0);
    }
}
