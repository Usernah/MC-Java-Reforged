package net.jr.client.runtime.terrain;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import net.jr.client.runtime.render.experimental.ExperimentalRenderController;
import net.jr.mixin.accessors.SectionRenderDispatcherAccessor;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;

/**
 * Paces asynchronous terrain compilation so worker bursts cannot consume an
 * unbounded amount of CPU time while several local cameras are moving.
 */
public final class TerrainCompileBudget {
    private static final int INITIAL_BUDGET = 4;
    private static final int MAX_BUDGET = 6;
    private static final int CORRECTION_BURST = 8;
    private static final int VANILLA_RECOMPILE_QUOTA = 2;
    private static final int RECOVERY_FRAMES = 45;
    private static final double FRAME_TIME_SMOOTHING = 0.2;
    private static final double DISPATCH_SMOOTHING = 0.2;

    private static final long DEFAULT_INITIAL_COST_NANOS = 2_000_000L;
    private static final long DEFAULT_CORRECTION_COST_NANOS = 1_000_000L;
    private static final long MINIMUM_TASK_COST_NANOS = 250_000L;
    private static final long MAXIMUM_TASK_COST_NANOS = 8_000_000L;

    private static final AtomicLong INITIAL_WORK_REMAINING = new AtomicLong();
    private static final AtomicLong CORRECTION_WORK_REMAINING = new AtomicLong();
    private static final AtomicInteger DISPATCHED = new AtomicInteger();
    private static final AtomicInteger COALESCED = new AtomicInteger();
    private static final AtomicInteger[] SLOT_DISPATCHES = createSlotCounters();
    private static final AtomicInteger NEXT_FAIR_SLOT = new AtomicInteger();
    private static final AtomicLong INITIAL_COMPLETED_NANOS = new AtomicLong();
    private static final AtomicLong INITIAL_COMPLETED_COUNT = new AtomicLong();
    private static final AtomicLong CORRECTION_COMPLETED_NANOS = new AtomicLong();
    private static final AtomicLong CORRECTION_COMPLETED_COUNT = new AtomicLong();

    private static boolean activeLastFrame;
    private static volatile boolean schedulingBatchOpen;
    private static int adaptiveBudget = INITIAL_BUDGET;
    private static int recoveryFrames;
    private static int deferredTasks;
    private static double smoothedFrameMs;
    private static double smoothedDispatched;
    private static double smoothedCoalesced;
    private static long estimatedInitialCostNanos = DEFAULT_INITIAL_COST_NANOS;
    private static long estimatedCorrectionCostNanos = DEFAULT_CORRECTION_COST_NANOS;
    private static volatile Snapshot published = Snapshot.disabled();

    private TerrainCompileBudget() {
    }

    /** Called once from the render thread before terrain scheduling begins. */
    public static void beginFrame(SectionRenderDispatcher dispatcher, int queueDepth) {
        int previousDispatched = DISPATCHED.getAndSet(0);
        int previousCoalesced = COALESCED.getAndSet(0);
        smoothedDispatched = smooth(smoothedDispatched, previousDispatched, DISPATCH_SMOOTHING);
        smoothedCoalesced = smooth(smoothedCoalesced, previousCoalesced, DISPATCH_SMOOTHING);

        if (!ExperimentalRenderController.enabled()) {
            INITIAL_WORK_REMAINING.set(0L);
            CORRECTION_WORK_REMAINING.set(0L);
            resetSlotDispatches();
            deferredTasks = 0;
            published = Snapshot.disabled();
            if (activeLastFrame && queueDepth > 0) {
                wakeWorkers(dispatcher, Math.max(1, dispatcher.getFreeBufferCount()));
            }
            activeLastFrame = false;
            return;
        }

        if (!activeLastFrame) {
            adaptiveBudget = INITIAL_BUDGET;
            recoveryFrames = 0;
            smoothedFrameMs = 0.0;
            smoothedDispatched = 0.0;
            smoothedCoalesced = 0.0;
        }
        activeLastFrame = true;

        refreshCostEstimates();
        long initialBudget = thisFrameInitialBudget();
        INITIAL_WORK_REMAINING.set(initialBudget);
        // Corrections (light, block edits and translucent re-sorts) get a small
        // independent lane. They remain responsive without being allowed to empty
        // the entire worker queue in a single frame.
        CORRECTION_WORK_REMAINING.set(
            estimatedCorrectionCostNanos * CORRECTION_BURST
        );
        resetSlotDispatches();
        deferredTasks = Math.max(0, queueDepth - adaptiveBudget);
        publish();

        // Requests from the split views are collected below before any worker is
        // awakened. Waking here lets the first viewport spend the frame before the
        // later viewports have even enqueued their nearby sections.
    }

    /** Starts the short gather phase which precedes one shared dispatch. */
    public static void beginSchedulingBatch() {
        if (ExperimentalRenderController.enabled()) {
            schedulingBatchOpen = true;
        }
    }

    /** Ends the gather phase and wakes workers once all local views are represented. */
    public static void finishSchedulingBatch(SectionRenderDispatcher dispatcher) {
        if (!ExperimentalRenderController.enabled()) {
            return;
        }
        schedulingBatchOpen = false;
        int availableBuilders = dispatcher.getFreeBufferCount();
        if (availableBuilders > 0) {
            wakeWorkers(dispatcher, Math.min(adaptiveBudget, availableBuilders));
        }
    }

    public static boolean shouldDeferVanillaWake() {
        return ExperimentalRenderController.enabled() && schedulingBatchOpen;
    }

    /**
     * Reserves estimated work before a task leaves the vanilla queue. This is a
     * global frame budget: all local views draw from it rather than each getting
     * an independent burst allowance.
     */
    public static boolean tryAcquire(boolean correction) {
        if (!ExperimentalRenderController.enabled()) {
            return true;
        }

        if (!correction) {
            return reserve(INITIAL_WORK_REMAINING, estimatedInitialCostNanos);
        }
        return reserve(CORRECTION_WORK_REMAINING, estimatedCorrectionCostNanos)
            || reserve(INITIAL_WORK_REMAINING, estimatedCorrectionCostNanos);
    }

    public static int recompileQuota() {
        return ExperimentalRenderController.enabled()
            ? CORRECTION_BURST
            : VANILLA_RECOMPILE_QUOTA;
    }

    private static boolean reserve(AtomicLong remaining, long cost) {
        while (true) {
            long available = remaining.get();
            if (available < cost) {
                return false;
            }
            if (remaining.compareAndSet(available, available - cost)) {
                return true;
            }
        }
    }

    /** Called after the queue has actually selected a task. */
    public static void recordDispatch(int referenceMask) {
        if (ExperimentalRenderController.enabled()) {
            DISPATCHED.incrementAndGet();
            for (int slotId = 0; slotId < SLOT_DISPATCHES.length; slotId++) {
                if ((referenceMask & 1 << slotId) != 0) {
                    SLOT_DISPATCHES[slotId].incrementAndGet();
                }
            }
        }
    }

    /**
     * Returns the next active viewport in the persistent round-robin order. The
     * cursor deliberately survives frame boundaries: with only one worker becoming
     * free per frame, resetting it would let the same viewport win forever.
     */
    public static int preferredSlot(int activeMask) {
        if (activeMask == 0) {
            return -1;
        }
        int start = Math.floorMod(NEXT_FAIR_SLOT.get(), SLOT_DISPATCHES.length);
        for (int offset = 0; offset < SLOT_DISPATCHES.length; offset++) {
            int slotId = (start + offset) % SLOT_DISPATCHES.length;
            if ((activeMask & 1 << slotId) != 0) {
                return slotId;
            }
        }
        return -1;
    }

    public static void advanceFairnessCursor(int servedSlot) {
        if (servedSlot >= 0 && servedSlot < SLOT_DISPATCHES.length) {
            NEXT_FAIR_SLOT.set((servedSlot + 1) % SLOT_DISPATCHES.length);
        }
    }

    public static int dispatchesForSlot(int slotId) {
        return slotId >= 0 && slotId < SLOT_DISPATCHES.length
            ? SLOT_DISPATCHES[slotId].get()
            : 0;
    }

    public static void recordCompletedTask(boolean correction, long durationNanos) {
        if (!ExperimentalRenderController.enabled() || durationNanos <= 0L) {
            return;
        }
        if (correction) {
            CORRECTION_COMPLETED_NANOS.addAndGet(durationNanos);
            CORRECTION_COMPLETED_COUNT.incrementAndGet();
        } else {
            INITIAL_COMPLETED_NANOS.addAndGet(durationNanos);
            INITIAL_COMPLETED_COUNT.incrementAndGet();
        }
    }

    public static void recordCoalescedRequest() {
        if (ExperimentalRenderController.enabled()) {
            COALESCED.incrementAndGet();
        }
    }

    /** Refreshes the visible backlog after all local cameras scheduled work. */
    public static void observeQueueDepth(int queueDepth) {
        if (!ExperimentalRenderController.enabled()) {
            return;
        }
        deferredTasks = Math.max(0, queueDepth - adaptiveBudget);
        publish();
    }

    /** Supplies frame feedback after rendering. Budget cuts are immediate; recovery is deliberately slow. */
    public static void endFrame(long frameNanos, long stagingRetries, boolean renderedTerrain) {
        if (!ExperimentalRenderController.enabled() || !renderedTerrain) {
            return;
        }

        double frameMs = frameNanos / 1_000_000.0;
        smoothedFrameMs = smooth(smoothedFrameMs, frameMs, FRAME_TIME_SMOOTHING);
        int desired = desiredBudget(smoothedFrameMs, stagingRetries);
        if (desired < adaptiveBudget) {
            adaptiveBudget = desired;
            recoveryFrames = 0;
        } else if (desired > adaptiveBudget) {
            if (++recoveryFrames >= RECOVERY_FRAMES) {
                adaptiveBudget++;
                recoveryFrames = 0;
            }
        } else {
            recoveryFrames = 0;
        }
        publish();
    }

    public static Snapshot snapshot() {
        return published;
    }

    private static int desiredBudget(double frameMs, long stagingRetries) {
        if (frameMs >= 90.0) {
            return 1;
        }
        if (stagingRetries > 0 || frameMs >= 50.0) {
            return 2;
        }
        if (frameMs >= 38.0) {
            return 3;
        }
        if (frameMs >= 28.0) {
            return 4;
        }
        return MAX_BUDGET;
    }

    private static double smooth(double previous, double sample, double weight) {
        return previous == 0.0 ? sample : previous + (sample - previous) * weight;
    }

    private static long thisFrameInitialBudget() {
        return Math.max(estimatedInitialCostNanos, estimatedInitialCostNanos * adaptiveBudget);
    }

    private static void refreshCostEstimates() {
        estimatedInitialCostNanos = smoothEstimate(
            estimatedInitialCostNanos,
            INITIAL_COMPLETED_NANOS.getAndSet(0L),
            INITIAL_COMPLETED_COUNT.getAndSet(0L)
        );
        estimatedCorrectionCostNanos = smoothEstimate(
            estimatedCorrectionCostNanos,
            CORRECTION_COMPLETED_NANOS.getAndSet(0L),
            CORRECTION_COMPLETED_COUNT.getAndSet(0L)
        );
    }

    private static long smoothEstimate(long previous, long totalNanos, long count) {
        if (count <= 0L) {
            return previous;
        }
        long sample = Math.max(MINIMUM_TASK_COST_NANOS, Math.min(MAXIMUM_TASK_COST_NANOS, totalNanos / count));
        return Math.max(
            MINIMUM_TASK_COST_NANOS,
            Math.min(MAXIMUM_TASK_COST_NANOS, Math.round(previous + (sample - previous) * 0.20))
        );
    }

    private static AtomicInteger[] createSlotCounters() {
        AtomicInteger[] counters = new AtomicInteger[4];
        for (int slotId = 0; slotId < counters.length; slotId++) {
            counters[slotId] = new AtomicInteger();
        }
        return counters;
    }

    private static void resetSlotDispatches() {
        for (AtomicInteger counter : SLOT_DISPATCHES) {
            counter.set(0);
        }
    }

    private static void wakeWorkers(SectionRenderDispatcher dispatcher, int count) {
        SectionRenderDispatcherAccessor accessor = (SectionRenderDispatcherAccessor)(Object)dispatcher;
        for (int index = 0; index < count; index++) {
            accessor.javaReforged$getExecutor().execute(accessor::javaReforged$runTask);
        }
    }

    private static void publish() {
        published = new Snapshot(
            adaptiveBudget,
            smoothedDispatched,
            smoothedCoalesced,
            deferredTasks,
            smoothedFrameMs
        );
    }

    public record Snapshot(
        int budget,
        double dispatchedPerFrame,
        double coalescedPerFrame,
        int deferredTasks,
        double smoothedFrameMs
    ) {
        private static Snapshot disabled() {
            return new Snapshot(0, 0.0, 0.0, 0, 0.0);
        }
    }
}
