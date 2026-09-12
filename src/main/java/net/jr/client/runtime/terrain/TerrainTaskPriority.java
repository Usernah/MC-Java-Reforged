package net.jr.client.runtime.terrain;

import net.jr.client.runtime.render.experimental.ExperimentalRenderController;
import net.jr.client.runtime.render.experimental.ExperimentalRenderMetrics;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class TerrainTaskPriority {
    // Tasks outside the persistent round-robin turn remain selectable as a
    // fallback, but can never beat a task requested by the preferred viewport.
    private static final double FAIRNESS_DEFERRED_SCORE = Double.MAX_VALUE / 4.0;
    private static final Vec3[] NO_CAMERAS = new Vec3[LocalClientSlotRegistry.MAX_SLOTS];

    private static volatile Vec3[] cameras = NO_CAMERAS;

    private TerrainTaskPriority() {
    }

    public static void publishCameras(Vec3[] nextCameras) {
        if (nextCameras.length != LocalClientSlotRegistry.MAX_SLOTS) {
            throw new IllegalArgumentException("Unexpected local camera count " + nextCameras.length);
        }
        cameras = nextCameras;
    }

    public static void clearCameras() {
        cameras = NO_CAMERAS;
    }

    public static double distanceSquared(Object task, BlockPos origin, double vanillaDistance) {
        if (!ExperimentalRenderController.enabled()) {
            return vanillaDistance;
        }

        TerrainTaskOwnership.TaskContext context = TerrainTaskOwnership.context(task);
        if (context == null || context.referenceMask() == null) {
            return vanillaDistance;
        }

        int referenceMask = context.referenceMask().get();
        Vec3[] cameraSnapshot = cameras;
        double bestDistance = Double.MAX_VALUE;
        boolean found = false;
        for (int slotId = 0; slotId < cameraSnapshot.length; slotId++) {
            if ((referenceMask & 1 << slotId) == 0) {
                continue;
            }
            Vec3 camera = cameraSnapshot[slotId];
            if (camera == null) {
                continue;
            }
            bestDistance = Math.min(bestDistance, origin.distToCenterSqr(camera));
            found = true;
        }
        if (!found) {
            return vanillaDistance;
        }

        int activeMask = activeCameraMask(cameraSnapshot);
        int preferredSlot = TerrainCompileBudget.preferredSlot(activeMask);
        return preferredSlot >= 0 && (referenceMask & 1 << preferredSlot) == 0
            ? FAIRNESS_DEFERRED_SCORE + bestDistance
            : bestDistance;
    }

    public static void recordSelection(Object task, BlockPos origin) {
        if (!ExperimentalRenderController.enabled()) {
            return;
        }

        TerrainTaskOwnership.TaskContext context = TerrainTaskOwnership.context(task);
        if (context == null || context.referenceMask() == null) {
            ExperimentalRenderMetrics.recordSchedulerFallback();
            return;
        }

        int referenceMask = context.referenceMask().get();
        Vec3[] cameraSnapshot = cameras;
        int activeMask = activeCameraMask(cameraSnapshot);
        int preferredSlot = TerrainCompileBudget.preferredSlot(activeMask);
        int bestSlotId = selectServedSlot(referenceMask, activeMask, preferredSlot);

        if (bestSlotId >= 0) {
            TerrainCompileBudget.recordDispatch(referenceMask);
            TerrainCompileBudget.advanceFairnessCursor(bestSlotId);
            ExperimentalRenderMetrics.recordSchedulerSelection(bestSlotId);
        } else {
            ExperimentalRenderMetrics.recordSchedulerFallback();
        }
    }

    private static int activeCameraMask(Vec3[] cameraSnapshot) {
        int mask = 0;
        for (int slotId = 0; slotId < cameraSnapshot.length; slotId++) {
            if (cameraSnapshot[slotId] != null) {
                mask |= 1 << slotId;
            }
        }
        return mask;
    }

    private static int selectServedSlot(int referenceMask, int activeMask, int preferredSlot) {
        int eligibleMask = referenceMask & activeMask;
        if (eligibleMask == 0) {
            return -1;
        }
        int start = preferredSlot >= 0 ? preferredSlot : 0;
        for (int offset = 0; offset < LocalClientSlotRegistry.MAX_SLOTS; offset++) {
            int slotId = (start + offset) % LocalClientSlotRegistry.MAX_SLOTS;
            if ((eligibleMask & 1 << slotId) != 0) {
                return slotId;
            }
        }
        return -1;
    }
}
