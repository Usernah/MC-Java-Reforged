package net.alnv.javareforged.ClientRuntime.runtime;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.world.phys.Vec3;
import net.alnv.javareforged.ClientRuntime.terrain.GlobalTerrainStore;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class TerrainSectionOwners {
    private static final ConcurrentMap<Object, TaskOwner> TASKS = new ConcurrentHashMap<>();

    private TerrainSectionOwners() {
    }

    public static void addRecentlyCompiledSection(SectionRenderDispatcher.RenderSection section) {
        TerrainCoordinator.onSectionCompiled(section);
    }

    public static Vec3 cameraPositionForSection(SectionRenderDispatcher.RenderSection section) {
        return LevelRendererFields.terrainStore().cameraPositionFor(section);
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

    public static void trackCompletion(Object task, CompletableFuture<?> future) {
        TaskOwner owner = TASKS.get(task);
        if (owner == null) {
            throw new IllegalStateException("Terrain compile task completed without registration");
        }
        future.whenComplete((result, error) -> {
            if (TASKS.remove(task, owner)) {
                owner.store().taskFinished(owner.section());
            }
        });
    }

    private record TaskOwner(GlobalTerrainStore store, SectionRenderDispatcher.RenderSection section) {
    }
}
