package net.jr.ClientRuntime.terrain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;

public final class SlotTerrainView {
    /*
     * This is only the logical terrain window for one player slot:
     * which section coordinates the slot wants to see, ordered from the camera
     * center outward. RenderSection instances and buffers still live in the
     * global store.
     */
    private static final Map<Integer, List<ColumnOffset>> COLUMN_OFFSETS = new ConcurrentHashMap<>();

    private final Set<TerrainKey> activeKeys = new LinkedHashSet<>();

    @Nullable
    private volatile GlobalTerrainStore store;
    @Nullable
    private volatile ClientLevel level;
    private volatile int viewDistance = -1;
    private volatile int centerSectionX = Integer.MIN_VALUE;
    private volatile int centerSectionZ = Integer.MIN_VALUE;

    public boolean needsUpdate(GlobalTerrainStore store, ClientLevel level, int viewDistance, double cameraX, double cameraZ) {
        return this.store != store
            || this.level != level
            || this.viewDistance != viewDistance
            || this.centerSectionX != SectionPos.blockToSectionCoord(cameraX)
            || this.centerSectionZ != SectionPos.blockToSectionCoord(cameraZ);
    }

    public Update update(GlobalTerrainStore store, ClientLevel level, int viewDistance, double cameraX, double cameraY, double cameraZ) {
        int nextCenterX = SectionPos.blockToSectionCoord(cameraX);
        int nextCenterY = SectionPos.blockToSectionCoord(cameraY);
        int nextCenterZ = SectionPos.blockToSectionCoord(cameraZ);
        boolean reset = this.store != store || this.level != level || this.viewDistance != viewDistance;
        int previousCenterX = this.centerSectionX;
        int previousCenterZ = this.centerSectionZ;

        Set<TerrainKey> removed = new LinkedHashSet<>();
        Set<TerrainKey> added = new LinkedHashSet<>();
        if (reset || this.activeKeys.isEmpty() || disjointMove(viewDistance, previousCenterX, previousCenterZ, nextCenterX, nextCenterZ)) {
            Set<TerrainKey> nextTarget = new LinkedHashSet<>();
            this.addFullArea(level, viewDistance, nextCenterX, nextCenterY, nextCenterZ, nextTarget);
            if (this.store == store) {
                removed.addAll(this.activeKeys);
            }
            added.addAll(nextTarget);
            if (!reset) {
                added.removeAll(this.activeKeys);
            }
            this.activeKeys.clear();
            this.activeKeys.addAll(nextTarget);
        } else {
            this.collectRemovedOutside(viewDistance, nextCenterX, nextCenterZ, removed);
            this.collectAddedOutsidePrevious(
                level,
                viewDistance,
                nextCenterX,
                nextCenterY,
                nextCenterZ,
                previousCenterX,
                previousCenterZ,
                added
            );
            this.activeKeys.removeAll(removed);
            this.activeKeys.addAll(added);
        }

        this.store = store;
        this.level = level;
        this.viewDistance = viewDistance;
        this.centerSectionX = nextCenterX;
        this.centerSectionZ = nextCenterZ;
        return new Update(added, removed, reset);
    }

    private static boolean disjointMove(int viewDistance, int previousCenterX, int previousCenterZ, int nextCenterX, int nextCenterZ) {
        int diameterWithoutCenter = viewDistance * 2;
        return Math.abs(nextCenterX - previousCenterX) > diameterWithoutCenter
            || Math.abs(nextCenterZ - previousCenterZ) > diameterWithoutCenter;
    }

    private void collectRemovedOutside(int viewDistance, int nextCenterX, int nextCenterZ, Set<TerrainKey> removed) {
        int minX = nextCenterX - viewDistance;
        int maxX = nextCenterX + viewDistance;
        int minZ = nextCenterZ - viewDistance;
        int maxZ = nextCenterZ + viewDistance;
        for (TerrainKey key : this.activeKeys) {
            if (key.sectionX() < minX || key.sectionX() > maxX || key.sectionZ() < minZ || key.sectionZ() > maxZ) {
                removed.add(key);
            }
        }
    }

    private void collectAddedOutsidePrevious(
        ClientLevel level,
        int viewDistance,
        int nextCenterX,
        int nextCenterY,
        int nextCenterZ,
        int previousCenterX,
        int previousCenterZ,
        Set<TerrainKey> added
    ) {
        int previousMinX = previousCenterX - viewDistance;
        int previousMaxX = previousCenterX + viewDistance;
        int previousMinZ = previousCenterZ - viewDistance;
        int previousMaxZ = previousCenterZ + viewDistance;
        for (ColumnOffset offset : columnOffsets(viewDistance)) {
            int sectionX = nextCenterX + offset.x();
            int sectionZ = nextCenterZ + offset.z();
            if (sectionX < previousMinX || sectionX > previousMaxX || sectionZ < previousMinZ || sectionZ > previousMaxZ) {
                this.addColumn(level, sectionX, nextCenterY, sectionZ, added);
            }
        }
    }

    private void addFullArea(ClientLevel level, int viewDistance, int centerX, int centerY, int centerZ, Set<TerrainKey> target) {
        for (ColumnOffset offset : columnOffsets(viewDistance)) {
            this.addColumn(level, centerX + offset.x(), centerY, centerZ + offset.z(), target);
        }
    }

    private static List<ColumnOffset> columnOffsets(int viewDistance) {
        return COLUMN_OFFSETS.computeIfAbsent(viewDistance, SlotTerrainView::buildColumnOffsets);
    }

    private static List<ColumnOffset> buildColumnOffsets(int viewDistance) {
        ArrayList<ColumnOffset> offsets = new ArrayList<>();

        for (int dx = -viewDistance; dx <= viewDistance; dx++) {
            for (int dz = -viewDistance; dz <= viewDistance; dz++) {
                offsets.add(new ColumnOffset(dx, dz));
            }
        }

        offsets.sort(
            Comparator
                .comparingInt((ColumnOffset offset) -> offset.x() * offset.x() + offset.z() * offset.z())
                .thenComparingInt(offset -> Math.abs(offset.x()))
                .thenComparingInt(offset -> Math.abs(offset.z()))
        );
        return List.copyOf(offsets);
    }

    private void addColumn(ClientLevel level, int sectionX, int centerY, int sectionZ, Set<TerrainKey> target) {
        int minY = level.getMinSectionY();
        int maxY = level.getMaxSectionY();
        int clampedCenterY = Math.max(minY, Math.min(centerY, maxY));
        this.addSection(level, sectionX, clampedCenterY, sectionZ, target);
        for (int distance = 1; clampedCenterY - distance >= minY || clampedCenterY + distance <= maxY; distance++) {
            if (clampedCenterY - distance >= minY) {
                this.addSection(level, sectionX, clampedCenterY - distance, sectionZ, target);
            }
            if (clampedCenterY + distance <= maxY) {
                this.addSection(level, sectionX, clampedCenterY + distance, sectionZ, target);
            }
        }
    }

    private void addSection(ClientLevel level, int sectionX, int sectionY, int sectionZ, Set<TerrainKey> target) {
        target.add(new TerrainKey(level.dimension(), sectionX, sectionY, sectionZ));
    }

    public Set<TerrainKey> sections() {
        return Collections.unmodifiableSet(this.activeKeys);
    }

    public boolean contains(TerrainKey key) {
        ClientLevel currentLevel = this.level;
        int distance = this.viewDistance;
        if (currentLevel == null || distance < 0 || !key.dimension().equals(currentLevel.dimension())) {
            return false;
        }
        if (key.sectionY() < currentLevel.getMinSectionY() || key.sectionY() > currentLevel.getMaxSectionY()) {
            return false;
        }
        return Math.abs(key.sectionX() - this.centerSectionX) <= distance
            && Math.abs(key.sectionZ() - this.centerSectionZ) <= distance;
    }

    @Nullable
    public ClientLevel level() {
        return this.level;
    }

    public boolean belongsTo(GlobalTerrainStore store) {
        return this.store == store;
    }

    public void clear() {
        this.activeKeys.clear();
        this.store = null;
        this.level = null;
        this.viewDistance = -1;
        this.centerSectionX = Integer.MIN_VALUE;
        this.centerSectionZ = Integer.MIN_VALUE;
    }

    public record Update(Set<TerrainKey> added, Set<TerrainKey> removed, boolean reset) {
    }

    private record ColumnOffset(int x, int z) {
    }
}
