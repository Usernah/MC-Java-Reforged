package net.jr.client.runtime.terrain;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.render.experimental.ExperimentalRenderController;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;

/**
 * Bounds admission of full visibility-graph rebuilds while allocation pressure
 * is real. Work that has already entered a world pipeline is never paused here:
 * lighting and terrain publication must preserve their dependency ordering.
 */
public final class ClientWorkBackpressure {
    private static final long MIB = 1024L * 1024L;
    private static final double GROWTH_SMOOTHING = 0.25D;
    private static final double MODERATE_OCCUPANCY = 0.50D;
    private static final double SEVERE_OCCUPANCY = 0.72D;
    private static final double MODERATE_GROWTH_MIB_PER_SECOND = 192.0D;
    private static final double SEVERE_GROWTH_MIB_PER_SECOND = 512.0D;
    private static final int PRESSURE_HOLD_TICKS = 60;
    private static final int NORMAL_GRAPH_REBUILDS = 2;
    private static final int PRESSURED_GRAPH_REBUILDS = 1;

    private static final Object GRAPH_LOCK = new Object();
    private static long previousSampleNanos;
    private static long previousUsedBytes;
    private static double smoothedGrowthMiBPerSecond;
    private static Pressure pressure = Pressure.NORMAL;
    private static int pressureHoldTicks;
    private static int runningGraphSlotsMask;
    private static int waitingGraphSlotsMask;
    private static int graphCursor;
    private static volatile Snapshot published = Snapshot.disabled();

    private ClientWorkBackpressure() {
    }

    public static void beginClientTick() {
        if (!ExperimentalRenderController.enabled()) {
            resetDisabled();
            return;
        }

        Runtime runtime = Runtime.getRuntime();
        long now = System.nanoTime();
        long usedBytes = runtime.totalMemory() - runtime.freeMemory();
        long maxBytes = runtime.maxMemory();
        double occupancy = maxBytes <= 0L ? 0.0D : (double)usedBytes / (double)maxBytes;
        double instantaneousGrowth = 0.0D;
        if (previousSampleNanos != 0L && now > previousSampleNanos && usedBytes > previousUsedBytes) {
            instantaneousGrowth = (usedBytes - previousUsedBytes)
                * 1_000_000_000.0D / (now - previousSampleNanos) / MIB;
        }
        previousSampleNanos = now;
        previousUsedBytes = usedBytes;
        smoothedGrowthMiBPerSecond = smooth(
            smoothedGrowthMiBPerSecond,
            instantaneousGrowth,
            GROWTH_SMOOTHING
        );

        Pressure observed = classify(occupancy, smoothedGrowthMiBPerSecond);
        if (observed.ordinal() > pressure.ordinal()) {
            pressure = observed;
            pressureHoldTicks = PRESSURE_HOLD_TICKS;
        } else if (observed == pressure) {
            if (observed != Pressure.NORMAL) {
                pressureHoldTicks = PRESSURE_HOLD_TICKS;
            }
        } else if (pressureHoldTicks > 0) {
            pressureHoldTicks--;
        } else {
            pressure = Pressure.values()[pressure.ordinal() - 1];
            pressureHoldTicks = pressure == Pressure.NORMAL ? 0 : PRESSURE_HOLD_TICKS;
        }

        synchronized (GRAPH_LOCK) {
            waitingGraphSlotsMask &= activeGraphSlotsMask();
        }
        publish(occupancy);
    }

    /** Fair, non-blocking admission for asynchronous full occlusion rebuilds. */
    public static boolean tryAcquireGraphRebuild(int slotId) {
        if (!ExperimentalRenderController.enabled()) {
            return true;
        }
        int bit = 1 << slotId;
        synchronized (GRAPH_LOCK) {
            waitingGraphSlotsMask |= bit;
            int limit = pressure == Pressure.NORMAL ? NORMAL_GRAPH_REBUILDS : PRESSURED_GRAPH_REBUILDS;
            if (Integer.bitCount(runningGraphSlotsMask) >= limit) {
                publishCurrentHeap();
                return false;
            }
            int selected = firstSelectedSlot(waitingGraphSlotsMask, graphCursor);
            if (selected != slotId) {
                return false;
            }
            waitingGraphSlotsMask &= ~bit;
            runningGraphSlotsMask |= bit;
            graphCursor = (slotId + 1) % LocalClientSlotRegistry.MAX_SLOTS;
            publishCurrentHeap();
            return true;
        }
    }

    public static void releaseGraphRebuild(int slotId) {
        synchronized (GRAPH_LOCK) {
            runningGraphSlotsMask &= ~(1 << slotId);
            publishCurrentHeap();
        }
    }

    public static Snapshot snapshot() {
        return published;
    }

    private static Pressure classify(double occupancy, double growthMiBPerSecond) {
        if (occupancy >= SEVERE_OCCUPANCY || growthMiBPerSecond >= SEVERE_GROWTH_MIB_PER_SECOND) {
            return Pressure.SEVERE;
        }
        if (
            occupancy >= MODERATE_OCCUPANCY
                || occupancy >= 0.25D && growthMiBPerSecond >= MODERATE_GROWTH_MIB_PER_SECOND
        ) {
            return Pressure.MODERATE;
        }
        return Pressure.NORMAL;
    }

    private static int activeGraphSlotsMask() {
        int mask = 0;
        for (int slotId = 0; slotId < LocalClientSlotRegistry.MAX_SLOTS; slotId++) {
            var client = ClientRuntime.INSTANCE.clients().clientOrNull(slotId);
            if (client != null && client.level() != null) {
                mask |= 1 << slotId;
            }
        }
        return mask;
    }

    private static int firstSelectedSlot(int mask, int cursor) {
        for (int offset = 0; offset < LocalClientSlotRegistry.MAX_SLOTS; offset++) {
            int slotId = (cursor + offset) % LocalClientSlotRegistry.MAX_SLOTS;
            if ((mask & 1 << slotId) != 0) {
                return slotId;
            }
        }
        return -1;
    }

    private static double smooth(double previous, double sample, double weight) {
        return previous == 0.0D ? sample : previous + (sample - previous) * weight;
    }

    private static void resetDisabled() {
        previousSampleNanos = 0L;
        previousUsedBytes = 0L;
        smoothedGrowthMiBPerSecond = 0.0D;
        pressure = Pressure.NORMAL;
        pressureHoldTicks = 0;
        synchronized (GRAPH_LOCK) {
            waitingGraphSlotsMask = 0;
        }
        published = Snapshot.disabled();
    }

    private static void publishCurrentHeap() {
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        long used = runtime.totalMemory() - runtime.freeMemory();
        publish(max <= 0L ? 0.0D : (double)used / (double)max);
    }

    private static void publish(double occupancy) {
        int runningGraphs;
        int waitingGraphs;
        synchronized (GRAPH_LOCK) {
            runningGraphs = Integer.bitCount(runningGraphSlotsMask);
            waitingGraphs = Integer.bitCount(waitingGraphSlotsMask);
        }
        published = new Snapshot(
            pressure.name(),
            occupancy * 100.0D,
            smoothedGrowthMiBPerSecond,
            runningGraphs,
            waitingGraphs
        );
    }

    private enum Pressure {
        NORMAL,
        MODERATE,
        SEVERE
    }

    public record Snapshot(
        String pressure,
        double heapPercent,
        double growthMiBPerSecond,
        int runningGraphRebuilds,
        int waitingGraphRebuilds
    ) {
        private static Snapshot disabled() {
            return new Snapshot("OFF", 0.0D, 0.0D, 0, 0);
        }
    }
}
