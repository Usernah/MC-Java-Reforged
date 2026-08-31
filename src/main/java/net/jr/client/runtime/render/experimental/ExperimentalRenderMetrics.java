package net.jr.client.runtime.render.experimental;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.LongAdder;
import net.jr.Java_reforged;
import net.jr.client.runtime.terrain.TerrainCompileBudget;

public final class ExperimentalRenderMetrics {
    private static final int WARMUP_FRAMES = 120;
    private static final int SAMPLE_WINDOW = 600;
    private static final int SNAPSHOT_INTERVAL = 15;

    private static final LongAdder COMPILED_SECTIONS = new LongAdder();
    private static final LongAdder STAGING_RETRIES = new LongAdder();
    private static final LongAdder[] SCHEDULER_SELECTIONS = createAdders(4);
    private static final LongAdder SCHEDULER_FALLBACKS = new LongAdder();
    private static final ThreadLocal<int[]> STAGING_RETRY_SAMPLER = ThreadLocal.withInitial(() -> new int[1]);

    private static final FrameAccumulator FRAME = new FrameAccumulator();
    private static RunStats currentRun;
    private static RunSnapshot lastOffRun = RunSnapshot.empty(false);
    private static RunSnapshot lastOnRun = RunSnapshot.empty(true);
    private static volatile MetricsSnapshot published = MetricsSnapshot.empty();
    private static int warmupFrames;
    private static int framesUntilSnapshot;
    private static boolean frameOpen;

    private ExperimentalRenderMetrics() {
    }

    public static void beginRun(boolean enabled, long generation) {
        resetSchedulerCounters();
        currentRun = new RunStats(enabled, generation);
        warmupFrames = WARMUP_FRAMES;
        framesUntilSnapshot = 0;
        publish();
    }

    public static void changeRun(boolean previousEnabled, boolean nextEnabled, long generation) {
        if (currentRun != null) {
            RunSnapshot completed = currentRun.snapshot(0);
            if (previousEnabled) {
                lastOnRun = completed;
            } else {
                lastOffRun = completed;
            }
            logCompletedRun(completed);
        }
        currentRun = new RunStats(nextEnabled, generation);
        warmupFrames = WARMUP_FRAMES;
        framesUntilSnapshot = 0;
        FRAME.reset();
        COMPILED_SECTIONS.reset();
        STAGING_RETRIES.reset();
        resetSchedulerCounters();
        publish();
    }

    public static void beginFrame() {
        if (currentRun == null) {
            beginRun(ExperimentalRenderController.enabled(), ExperimentalRenderController.generation());
        }
        FRAME.reset();
        FRAME.frameStarted = System.nanoTime();
        frameOpen = true;
    }

    public static void endFrame() {
        if (!frameOpen) {
            return;
        }
        frameOpen = false;
        FRAME.frameNanos = System.nanoTime() - FRAME.frameStarted;
        FRAME.compiledSections = COMPILED_SECTIONS.sumThenReset();
        FRAME.stagingRetries = STAGING_RETRIES.sumThenReset();
        TerrainCompileBudget.endFrame(
            FRAME.frameNanos,
            FRAME.stagingRetries,
            FRAME.renderedSlots > 0
        );
        if (FRAME.renderedSlots <= 0) {
            return;
        }
        if (warmupFrames > 0) {
            warmupFrames--;
        } else {
            currentRun.add(FRAME.sample());
        }
        if (--framesUntilSnapshot <= 0) {
            framesUntilSnapshot = SNAPSHOT_INTERVAL;
            publish();
        }
    }

    public static long startTimer() {
        return System.nanoTime();
    }

    public static void recordExtraction(int slotId, long startedAt) {
        if (!frameOpen) {
            return;
        }
        long elapsed = System.nanoTime() - startedAt;
        FRAME.extractionNanos += elapsed;
        if (slotId >= 0 && slotId < FRAME.slotExtractionNanos.length) {
            FRAME.slotExtractionNanos[slotId] += elapsed;
        }
    }

    public static void recordSlotRender(int slotId, long startedAt, int visibleSections) {
        if (!frameOpen) {
            return;
        }
        long elapsed = System.nanoTime() - startedAt;
        FRAME.renderedSlotsNanos += elapsed;
        FRAME.renderedSlots++;
        FRAME.visibleSections += visibleSections;
        if (slotId >= 0 && slotId < FRAME.slotRenderBreakdownNanos.length) {
            FRAME.slotRenderBreakdownNanos[slotId] += elapsed;
        }
    }

    public static void recordTerrainScheduling(long startedAt, int queueDepth) {
        if (!frameOpen) {
            return;
        }
        FRAME.terrainSchedulingNanos += System.nanoTime() - startedAt;
        FRAME.queueDepth = Math.max(FRAME.queueDepth, queueDepth);
    }

    public static void recordTerrainUpload(long startedAt, int queueDepth) {
        if (!frameOpen) {
            return;
        }
        FRAME.terrainUploadNanos += System.nanoTime() - startedAt;
        FRAME.queueDepth = Math.max(FRAME.queueDepth, queueDepth);
    }

    public static void recordDrawPreparation(int slotId, long elapsed, int draws) {
        if (!frameOpen) {
            return;
        }
        FRAME.drawPreparationNanos += elapsed;
        FRAME.draws += draws;
        if (slotId >= 0 && slotId < FRAME.slotDrawPreparationNanos.length) {
            FRAME.slotDrawPreparationNanos[slotId] += elapsed;
        }
    }

    public static void recordSectionCompiled() {
        COMPILED_SECTIONS.increment();
    }

    public static void recordStagingRetry() {
        int[] sampler = STAGING_RETRY_SAMPLER.get();
        if ((sampler[0]++ & 255) == 0) {
            STAGING_RETRIES.increment();
        }
    }

    public static void recordSchedulerSelection(int slotId) {
        if (slotId >= 0 && slotId < SCHEDULER_SELECTIONS.length) {
            SCHEDULER_SELECTIONS[slotId].increment();
        }
    }

    public static void recordSchedulerFallback() {
        SCHEDULER_FALLBACKS.increment();
    }

    public static MetricsSnapshot snapshot() {
        return published;
    }

    private static void publish() {
        RunSnapshot current = currentRun == null
            ? RunSnapshot.empty(ExperimentalRenderController.enabled())
            : currentRun.snapshot(warmupFrames);
        published = new MetricsSnapshot(current, lastOffRun, lastOnRun, schedulerSnapshot());
    }

    private static LongAdder[] createAdders(int count) {
        LongAdder[] adders = new LongAdder[count];
        Arrays.setAll(adders, ignored -> new LongAdder());
        return adders;
    }

    private static void resetSchedulerCounters() {
        for (LongAdder selection : SCHEDULER_SELECTIONS) {
            selection.reset();
        }
        SCHEDULER_FALLBACKS.reset();
    }

    private static SchedulerSnapshot schedulerSnapshot() {
        return new SchedulerSnapshot(
            SCHEDULER_SELECTIONS[0].sum(),
            SCHEDULER_SELECTIONS[1].sum(),
            SCHEDULER_SELECTIONS[2].sum(),
            SCHEDULER_SELECTIONS[3].sum(),
            SCHEDULER_FALLBACKS.sum()
        );
    }

    private static void logCompletedRun(RunSnapshot run) {
        if (run.samples() == 0) {
            return;
        }
        SchedulerSnapshot scheduler = schedulerSnapshot();
        Java_reforged.LOGGER.info(
            "Experimental render {} run: {} samples, frame avg={} ms, p95={} ms, p99={} ms, draws/frame={}, sections/frame={}, priority=[{},{},{},{}], fallback={}",
            run.enabled() ? "ON" : "OFF",
            run.samples(),
            format(run.averageFrameMs()),
            format(run.p95FrameMs()),
            format(run.p99FrameMs()),
            format(run.averageDraws()),
            format(run.averageVisibleSections()),
            scheduler.slot0(),
            scheduler.slot1(),
            scheduler.slot2(),
            scheduler.slot3(),
            scheduler.fallbacks()
        );
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static final class FrameAccumulator {
        private final long[] slotExtractionNanos = new long[4];
        private final long[] slotRenderBreakdownNanos = new long[4];
        private final long[] slotDrawPreparationNanos = new long[4];
        private long frameStarted;
        private long frameNanos;
        private long extractionNanos;
        private long renderedSlotsNanos;
        private long terrainSchedulingNanos;
        private long terrainUploadNanos;
        private long drawPreparationNanos;
        private long compiledSections;
        private long stagingRetries;
        private int renderedSlots;
        private int visibleSections;
        private int draws;
        private int queueDepth;

        private void reset() {
            Arrays.fill(this.slotExtractionNanos, 0L);
            Arrays.fill(this.slotRenderBreakdownNanos, 0L);
            Arrays.fill(this.slotDrawPreparationNanos, 0L);
            this.frameStarted = 0L;
            this.frameNanos = 0L;
            this.extractionNanos = 0L;
            this.renderedSlotsNanos = 0L;
            this.terrainSchedulingNanos = 0L;
            this.terrainUploadNanos = 0L;
            this.drawPreparationNanos = 0L;
            this.compiledSections = 0L;
            this.stagingRetries = 0L;
            this.renderedSlots = 0;
            this.visibleSections = 0;
            this.draws = 0;
            this.queueDepth = 0;
        }

        private FrameSample sample() {
            return new FrameSample(
                this.frameNanos,
                this.extractionNanos,
                this.renderedSlotsNanos,
                this.terrainSchedulingNanos,
                this.terrainUploadNanos,
                this.drawPreparationNanos,
                this.compiledSections,
                this.stagingRetries,
                this.renderedSlots,
                this.visibleSections,
                this.draws,
                this.queueDepth
            );
        }
    }

    private static final class RunStats {
        private final boolean enabled;
        private final long generation;
        private final FrameSample[] samples = new FrameSample[SAMPLE_WINDOW];
        private int nextIndex;
        private int size;

        private RunStats(boolean enabled, long generation) {
            this.enabled = enabled;
            this.generation = generation;
        }

        private void add(FrameSample sample) {
            this.samples[this.nextIndex] = sample;
            this.nextIndex = (this.nextIndex + 1) % this.samples.length;
            this.size = Math.min(this.size + 1, this.samples.length);
        }

        private RunSnapshot snapshot(int warmup) {
            if (this.size == 0) {
                return new RunSnapshot(
                    this.enabled,
                    this.generation,
                    warmup,
                    0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0
                );
            }
            long[] frameTimes = new long[this.size];
            double frameTotal = 0.0;
            double extractionTotal = 0.0;
            double renderTotal = 0.0;
            double schedulingTotal = 0.0;
            double uploadTotal = 0.0;
            double drawPreparationTotal = 0.0;
            double compiledTotal = 0.0;
            double stagingRetryTotal = 0.0;
            double visibleSectionTotal = 0.0;
            double drawTotal = 0.0;
            double renderedSlotTotal = 0.0;
            for (int index = 0; index < this.size; index++) {
                FrameSample sample = this.samples[index];
                frameTimes[index] = sample.frameNanos();
                frameTotal += sample.frameNanos();
                extractionTotal += sample.extractionNanos();
                renderTotal += sample.slotRenderNanos();
                schedulingTotal += sample.terrainSchedulingNanos();
                uploadTotal += sample.terrainUploadNanos();
                drawPreparationTotal += sample.drawPreparationNanos();
                compiledTotal += sample.compiledSections();
                stagingRetryTotal += sample.stagingRetries();
                visibleSectionTotal += sample.visibleSections();
                drawTotal += sample.draws();
                renderedSlotTotal += sample.renderedSlots();
            }
            int latestIndex = (this.nextIndex - 1 + this.samples.length) % this.samples.length;
            int queueDepth = this.samples[latestIndex].queueDepth();
            Arrays.sort(frameTimes);
            return new RunSnapshot(
                this.enabled,
                this.generation,
                warmup,
                this.size,
                nanosToMillis(frameTotal / this.size),
                nanosToMillis(percentile(frameTimes, 0.95)),
                nanosToMillis(percentile(frameTimes, 0.99)),
                nanosToMillis(extractionTotal / this.size),
                nanosToMillis(renderTotal / this.size),
                nanosToMillis(schedulingTotal / this.size),
                nanosToMillis(uploadTotal / this.size),
                nanosToMillis(drawPreparationTotal / this.size),
                compiledTotal / this.size,
                stagingRetryTotal / this.size,
                visibleSectionTotal / this.size,
                drawTotal / this.size,
                renderedSlotTotal / this.size,
                queueDepth
            );
        }
    }

    private static long percentile(long[] sorted, double percentile) {
        int index = Math.max(0, Math.min(sorted.length - 1, (int)Math.ceil(sorted.length * percentile) - 1));
        return sorted[index];
    }

    private static double nanosToMillis(double nanos) {
        return nanos / 1_000_000.0;
    }

    private record FrameSample(
        long frameNanos,
        long extractionNanos,
        long slotRenderNanos,
        long terrainSchedulingNanos,
        long terrainUploadNanos,
        long drawPreparationNanos,
        long compiledSections,
        long stagingRetries,
        int renderedSlots,
        int visibleSections,
        int draws,
        int queueDepth
    ) {
    }

    public record RunSnapshot(
        boolean enabled,
        long generation,
        int warmupFrames,
        int samples,
        double averageFrameMs,
        double p95FrameMs,
        double p99FrameMs,
        double averageExtractionMs,
        double averageSlotRenderMs,
        double averageTerrainSchedulingMs,
        double averageTerrainUploadMs,
        double averageDrawPreparationMs,
        double averageCompiledSections,
        double averageStagingRetries,
        double averageVisibleSections,
        double averageDraws,
        double averageRenderedSlots,
        int queueDepth
    ) {
        private static RunSnapshot empty(boolean enabled) {
            return new RunSnapshot(enabled, 0L, WARMUP_FRAMES, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0);
        }
    }

    public record SchedulerSnapshot(long slot0, long slot1, long slot2, long slot3, long fallbacks) {
        private static SchedulerSnapshot empty() {
            return new SchedulerSnapshot(0L, 0L, 0L, 0L, 0L);
        }
    }

    public record MetricsSnapshot(
        RunSnapshot current,
        RunSnapshot lastOff,
        RunSnapshot lastOn,
        SchedulerSnapshot scheduler
    ) {
        private static MetricsSnapshot empty() {
            return new MetricsSnapshot(
                RunSnapshot.empty(false),
                RunSnapshot.empty(false),
                RunSnapshot.empty(true),
                SchedulerSnapshot.empty()
            );
        }

        public RunSnapshot comparison() {
            return this.current.enabled() ? this.lastOff : this.lastOn;
        }
    }
}
