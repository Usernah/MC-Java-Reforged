package net.jr.client.runtime.terrain;

public interface TerrainTaskQueueExtension {
    /** Last published queue size, including cancellations not yet consumed by a worker. */
    int javaReforged$queuedTaskCount();
}
