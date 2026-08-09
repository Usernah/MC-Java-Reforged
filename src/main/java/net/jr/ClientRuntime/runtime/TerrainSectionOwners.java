package net.jr.ClientRuntime.runtime;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.world.phys.Vec3;
import net.jr.ClientRuntime.terrain.GlobalTerrainStore;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public final class TerrainSectionOwners {
    private static final ConcurrentMap<Object, TaskOwner> TASKS = new ConcurrentHashMap<>();
    private static final ThreadLocal<AtomicReference<Vec3>> CAMERA_REFERENCE = ThreadLocal.withInitial(AtomicReference::new);

    private TerrainSectionOwners() {
    }

    public static void addRecentlyCompiledSection(SectionRenderDispatcher.RenderSection section) {
        TerrainCoordinator.onSectionCompiled(section);
    }

    public static Vec3 cameraPositionForSection(SectionRenderDispatcher.RenderSection section) {
        return LevelRendererFields.terrainStore().cameraPositionFor(section);
    }

    public static AtomicReference<Vec3> cameraReferenceForSection(SectionRenderDispatcher.RenderSection section) {
        AtomicReference<Vec3> reference = CAMERA_REFERENCE.get();
        reference.set(cameraPositionForSection(section));
        return reference;
    }

    public static AtomicReference<Vec3> cameraReferenceForTask(Object task) {
        TaskOwner owner = TASKS.get(task);
        if (owner == null) {
            throw new IllegalStateException("Terrain task has no registered owner");
        }
        return cameraReferenceForSection(owner.section());
    }

    public static double compilationPriority(SectionRenderDispatcher.RenderSection section) {
        return LevelRendererFields.terrainStore().compilationPriority(section);
    }

    public static ClientLevel levelForSection(SectionRenderDispatcher.RenderSection section) {
        return LevelRendererFields.terrainStore().ownerFor(section);
    }

    public static boolean deferUntilOwner(SectionRenderDispatcher.RenderSection section, String reason) {
        return LevelRendererFields.terrainStore().deferUntilOwner(section, reason);
    }

    public static void taskCreated(SectionRenderDispatcher.RenderSection section, Object task) {
        GlobalTerrainStore store = LevelRendererFields.terrainStore();
        store.taskStarted(section);
        TaskOwner previous = TASKS.putIfAbsent(task, new TaskOwner(store, section));
        if (previous != null) {
            throw new IllegalStateException("Terrain compile task was registered twice");
        }
    }

    public static void taskFinished(Object task) {
        TaskOwner owner = TASKS.remove(task);
        if (owner != null) {
            owner.store().taskFinished(owner.section());
        }
    }

    private record TaskOwner(GlobalTerrainStore store, SectionRenderDispatcher.RenderSection section) {
    }
}
