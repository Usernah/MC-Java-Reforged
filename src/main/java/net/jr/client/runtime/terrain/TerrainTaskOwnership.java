package net.jr.client.runtime.terrain;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.world.phys.Vec3;

public final class TerrainTaskOwnership {
    private static final ConcurrentMap<Object, TaskContext> TASKS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<SectionRenderDispatcher.RenderSection, Object> PENDING_COMPILES =
        new ConcurrentHashMap<>();

    private TerrainTaskOwnership() {
    }

    public static AtomicReference<Vec3> cameraReferenceForTask(Object task) {
        TaskContext context = TASKS.get(task);
        if (context == null) {
            throw new IllegalStateException("Terrain task has no captured player camera");
        }
        return context.cameraPosition();
    }

    public static void taskCreated(
        SectionRenderDispatcher.RenderSection section,
        Object task,
        boolean compileTask
    ) {
        Vec3 cameraPosition = TerrainStateStore.terrain().nullableCameraPosition();
        if (cameraPosition == null) {
            throw new IllegalStateException("Cannot create terrain task without an active player camera");
        }
        SharedTerrainStore store = TerrainStateStore.nullableTerrainStore();
        AtomicInteger referenceMask = null;
        long revision = -1L;
        if (store != null) {
            store.taskStarted(section);
            referenceMask = store.referenceMaskForTask(section);
            revision = store.revision(section);
        }
        TaskContext previous = TASKS.putIfAbsent(
            task,
            new TaskContext(
                section,
                store,
                referenceMask,
                revision,
                new AtomicReference<>(cameraPosition),
                new AtomicBoolean(),
                compileTask
            )
        );
        if (previous != null) {
            if (store != null) {
                store.taskFinished(section);
            }
            throw new IllegalStateException("Terrain task was registered twice");
        }
        if (compileTask) {
            PENDING_COMPILES.put(section, task);
        }
    }

    public static void taskFinished(Object task) {
        TaskContext context = TASKS.remove(task);
        release(task, context);
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
            release(task, context);
        }
    }

    public static boolean hasPendingCompile(SectionRenderDispatcher.RenderSection section) {
        return PENDING_COMPILES.containsKey(section);
    }

    public static boolean pendingCompileCoversCurrentRevision(
        SectionRenderDispatcher.RenderSection section
    ) {
        Object task = PENDING_COMPILES.get(section);
        TaskContext context = task == null ? null : TASKS.get(task);
        if (context == null) {
            return false;
        }
        return context.store() == null
            || context.revision() == context.store().revision(section);
    }

    public static boolean isCurrent(Object task) {
        TaskContext context = TASKS.get(task);
        return context == null
            || context.store() == null
            || context.revision() == context.store().revision(context.section());
    }

    public static boolean isTracked(Object task) {
        return TASKS.containsKey(task);
    }

    private static void release(Object task, @Nullable TaskContext context) {
        if (context == null) {
            return;
        }
        if (context.compileTask()) {
            PENDING_COMPILES.remove(context.section(), task);
        }
        if (context.store() != null) {
            context.store().taskFinished(context.section());
        }
    }

    static @Nullable TaskContext context(Object task) {
        return TASKS.get(task);
    }

    record TaskContext(
        SectionRenderDispatcher.RenderSection section,
        @Nullable SharedTerrainStore store,
        @Nullable AtomicInteger referenceMask,
        long revision,
        AtomicReference<Vec3> cameraPosition,
        AtomicBoolean executing,
        boolean compileTask
    ) {
    }
}
