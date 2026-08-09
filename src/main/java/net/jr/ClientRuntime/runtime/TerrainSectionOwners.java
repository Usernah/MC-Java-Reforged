package net.jr.ClientRuntime.runtime;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import net.jr.ClientRuntime.terrain.GlobalTerrainStore;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.world.phys.Vec3;

/** Captures only the camera context required by Vanilla's asynchronous terrain task. */
public final class TerrainSectionOwners {
    private static final ConcurrentMap<Object, TaskContext> TASKS = new ConcurrentHashMap<>();

    private TerrainSectionOwners() {
    }

    public static AtomicReference<Vec3> cameraReferenceForTask(Object task) {
        TaskContext context = TASKS.get(task);
        if (context == null) {
            throw new IllegalStateException("Terrain task has no captured player camera");
        }
        return context.cameraPosition();
    }

    public static void taskCreated(SectionRenderDispatcher.RenderSection section, Object task) {
        Vec3 cameraPosition = LevelRendererFields.terrain().nullableCameraPosition();
        if (cameraPosition == null) {
            throw new IllegalStateException("Cannot create terrain task without an active player camera");
        }
        GlobalTerrainStore store = LevelRendererFields.nullableTerrainStore();
        if (store != null) {
            store.taskStarted(section);
        }
        TaskContext previous = TASKS.putIfAbsent(
            task,
            new TaskContext(section, store, new AtomicReference<>(cameraPosition), new AtomicBoolean())
        );
        if (previous != null) {
            if (store != null) {
                store.taskFinished(section);
            }
            throw new IllegalStateException("Terrain task was registered twice");
        }
    }

    public static void taskFinished(Object task) {
        TaskContext context = TASKS.remove(task);
        release(context);
    }

    public static void taskExecuting(Object task) {
        TaskContext context = TASKS.get(task);
        if (context != null) {
            context.executing().set(true);
        }
    }

    public static void taskCancelled(Object task) {
        TaskContext context = TASKS.get(task);
        if (context != null && !context.executing().get() && TASKS.remove(task, context)) {
            release(context);
        }
    }

    private static void release(@Nullable TaskContext context) {
        if (context != null && context.store() != null) {
            context.store().taskFinished(context.section());
        }
    }

    private record TaskContext(
        SectionRenderDispatcher.RenderSection section,
        @Nullable GlobalTerrainStore store,
        AtomicReference<Vec3> cameraPosition,
        AtomicBoolean executing
    ) {
    }
}
