package net.jr.client.runtime.terrain;

import java.util.concurrent.atomic.AtomicInteger;
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
    private static final int RECOVERY_FRAMES = 45;
    private static final double FRAME_TIME_SMOOTHING = 0.2;
    private static final double DISPATCH_SMOOTHING = 0.2;

    private static final AtomicInteger PERMITS = new AtomicInteger();
    private static final AtomicInteger DISPATCHED = new AtomicInteger();
    private static final AtomicInteger COALESCED = new AtomicInteger();

    private static boolean activeLastFrame;
    private static int adaptiveBudget = INITIAL_BUDGET;
    private static int recoveryFrames;
    private static int deferredTasks;
    private static double smoothedFrameMs;
    private static double smoothedDispatched;
    private static double smoothedCoalesced;
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
            PERMITS.set(0);
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

        PERMITS.set(adaptiveBudget);
        deferredTasks = Math.max(0, queueDepth - adaptiveBudget);
        publish();

        if (queueDepth > 0) {
            int availableBuilders = dispatcher.getFreeBufferCount();
            if (availableBuilders > 0) {
                wakeWorkers(dispatcher, Math.min(adaptiveBudget, availableBuilders));
            }
        }
    }

    /**
     * Claims permission to remove one task from the vanilla dynamic queue.
     * Returning true while disabled preserves vanilla queue semantics.
     */
    public static boolean tryAcquirePermit() {
        if (!ExperimentalRenderController.enabled()) {
            return true;
        }

        while (true) {
            int available = PERMITS.get();
            if (available <= 0) {
                return false;
            }
            if (PERMITS.compareAndSet(available, available - 1)) {
                return true;
            }
        }
    }

    /** Called after the queue has actually selected a task. */
    public static void recordDispatch() {
        if (ExperimentalRenderController.enabled()) {
            DISPATCHED.incrementAndGet();
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
        deferredTasks = Math.max(0, queueDepth - Math.max(0, PERMITS.get()));
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
