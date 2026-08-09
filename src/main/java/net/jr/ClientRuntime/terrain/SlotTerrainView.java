package net.jr.ClientRuntime.terrain;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * A player's logical terrain window.
 *
 * <p>The window stores only its bounds. It deliberately does not retain one
 * TerrainKey (or RenderSection) per visible coordinate; those objects belong to
 * {@link GlobalTerrainStore}. Moving the camera enumerates only columns that
 * leave or enter the square window.</p>
 */
public final class SlotTerrainView {
    @Nullable
    private GlobalTerrainStore store;
    @Nullable
    private ClientLevel level;
    private int viewDistance = -1;
    private int centerSectionX = Integer.MIN_VALUE;
    private int centerSectionZ = Integer.MIN_VALUE;

    public Update update(
        GlobalTerrainStore store,
        ClientLevel level,
        int viewDistance,
        int nextCenterX,
        int nextCenterZ
    ) {
        boolean reset = this.store != store || this.level != level || this.viewDistance != viewDistance;
        ArrayList<TerrainKey> removed = new ArrayList<>();
        ArrayList<TerrainKey> added = new ArrayList<>();

        if (reset) {
            this.collectCurrent(removed);
            collectArea(level, viewDistance, nextCenterX, nextCenterZ, added);
        } else if (this.centerSectionX != nextCenterX || this.centerSectionZ != nextCenterZ) {
            int previousMinX = this.centerSectionX - viewDistance;
            int previousMaxX = this.centerSectionX + viewDistance;
            int previousMinZ = this.centerSectionZ - viewDistance;
            int previousMaxZ = this.centerSectionZ + viewDistance;
            int nextMinX = nextCenterX - viewDistance;
            int nextMaxX = nextCenterX + viewDistance;
            int nextMinZ = nextCenterZ - viewDistance;
            int nextMaxZ = nextCenterZ + viewDistance;

            collectDifference(
                level,
                previousMinX,
                previousMaxX,
                previousMinZ,
                previousMaxZ,
                nextMinX,
                nextMaxX,
                nextMinZ,
                nextMaxZ,
                removed
            );
            collectDifference(
                level,
                nextMinX,
                nextMaxX,
                nextMinZ,
                nextMaxZ,
                previousMinX,
                previousMaxX,
                previousMinZ,
                previousMaxZ,
                added
            );
        }

        this.store = store;
        this.level = level;
        this.viewDistance = viewDistance;
        this.centerSectionX = nextCenterX;
        this.centerSectionZ = nextCenterZ;
        return new Update(added, removed);
    }

    private void collectCurrent(List<TerrainKey> target) {
        ClientLevel currentLevel = this.level;
        if (currentLevel != null && this.viewDistance >= 0) {
            collectArea(currentLevel, this.viewDistance, this.centerSectionX, this.centerSectionZ, target);
        }
    }

    private static void collectArea(
        ClientLevel level,
        int viewDistance,
        int centerX,
        int centerZ,
        List<TerrainKey> target
    ) {
        for (int sectionX = centerX - viewDistance; sectionX <= centerX + viewDistance; sectionX++) {
            for (int sectionZ = centerZ - viewDistance; sectionZ <= centerZ + viewDistance; sectionZ++) {
                collectColumn(level, sectionX, sectionZ, target);
            }
        }
    }

    /** Adds columns in the first rectangle that are not inside the second rectangle. */
    private static void collectDifference(
        ClientLevel level,
        int sourceMinX,
        int sourceMaxX,
        int sourceMinZ,
        int sourceMaxZ,
        int otherMinX,
        int otherMaxX,
        int otherMinZ,
        int otherMaxZ,
        List<TerrainKey> target
    ) {
        for (int sectionX = sourceMinX; sectionX <= sourceMaxX; sectionX++) {
            for (int sectionZ = sourceMinZ; sectionZ <= sourceMaxZ; sectionZ++) {
                if (
                    sectionX < otherMinX
                        || sectionX > otherMaxX
                        || sectionZ < otherMinZ
                        || sectionZ > otherMaxZ
                ) {
                    collectColumn(level, sectionX, sectionZ, target);
                }
            }
        }
    }

    private static void collectColumn(ClientLevel level, int sectionX, int sectionZ, List<TerrainKey> target) {
        for (int sectionY = level.getMinSectionY(); sectionY <= level.getMaxSectionY(); sectionY++) {
            target.add(new TerrainKey(level.dimension(), sectionX, sectionY, sectionZ));
        }
    }

    public void forEachKey(Consumer<TerrainKey> consumer) {
        ClientLevel currentLevel = this.level;
        if (currentLevel == null || this.viewDistance < 0) {
            return;
        }
        for (int sectionX = this.centerSectionX - this.viewDistance; sectionX <= this.centerSectionX + this.viewDistance; sectionX++) {
            for (int sectionZ = this.centerSectionZ - this.viewDistance; sectionZ <= this.centerSectionZ + this.viewDistance; sectionZ++) {
                for (int sectionY = currentLevel.getMinSectionY(); sectionY <= currentLevel.getMaxSectionY(); sectionY++) {
                    consumer.accept(new TerrainKey(currentLevel.dimension(), sectionX, sectionY, sectionZ));
                }
            }
        }
    }

    public boolean contains(TerrainKey key) {
        ClientLevel currentLevel = this.level;
        int distance = this.viewDistance;
        return currentLevel != null
            && distance >= 0
            && key.dimension().equals(currentLevel.dimension())
            && key.sectionY() >= currentLevel.getMinSectionY()
            && key.sectionY() <= currentLevel.getMaxSectionY()
            && Math.abs(key.sectionX() - this.centerSectionX) <= distance
            && Math.abs(key.sectionZ() - this.centerSectionZ) <= distance;
    }

    public @Nullable ClientLevel level() {
        return this.level;
    }

    public void clear() {
        this.store = null;
        this.level = null;
        this.viewDistance = -1;
        this.centerSectionX = Integer.MIN_VALUE;
        this.centerSectionZ = Integer.MIN_VALUE;
    }

    public record Update(List<TerrainKey> added, List<TerrainKey> removed) {
    }
}
