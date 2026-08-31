package net.jr.client.runtime.terrain;

import javax.annotation.Nullable;
import net.jr.client.runtime.render.experimental.ExperimentalRenderController;
import net.jr.client.runtime.render.experimental.ExperimentalRenderMetrics;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class TerrainTaskPriority {
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
        return found ? bestDistance : vanillaDistance;
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
        int bestSlotId = -1;
        double bestDistance = Double.MAX_VALUE;
        for (int slotId = 0; slotId < cameraSnapshot.length; slotId++) {
            if ((referenceMask & 1 << slotId) == 0) {
                continue;
            }
            @Nullable Vec3 camera = cameraSnapshot[slotId];
            if (camera == null) {
                continue;
            }
            double distance = origin.distToCenterSqr(camera);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestSlotId = slotId;
            }
        }

        if (bestSlotId >= 0) {
            ExperimentalRenderMetrics.recordSchedulerSelection(bestSlotId);
        } else {
            ExperimentalRenderMetrics.recordSchedulerFallback();
        }
    }
}
