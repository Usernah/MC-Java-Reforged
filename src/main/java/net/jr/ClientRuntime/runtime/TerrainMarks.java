package net.jr.ClientRuntime.runtime;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.jr.ClientRuntime.slot.PlayerSlot;
import net.jr.ClientRuntime.slot.PlayerSlots;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;

public final class TerrainMarks {
    private TerrainMarks() {
    }

    public static void setBlocksDirty(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        for (int z = minZ - 1; z <= maxZ + 1; z++) {
            for (int x = minX - 1; x <= maxX + 1; x++) {
                for (int y = minY - 1; y <= maxY + 1; y++) {
                    setSectionDirty(
                        SectionPos.blockToSectionCoord(x),
                        SectionPos.blockToSectionCoord(y),
                        SectionPos.blockToSectionCoord(z),
                        true
                    );
                }
            }
        }
    }

    public static void setSectionDirtyWithNeighbors(int sectionX, int sectionY, int sectionZ) {
        for (int z = sectionZ - 1; z <= sectionZ + 1; z++) {
            for (int x = sectionX - 1; x <= sectionX + 1; x++) {
                for (int y = sectionY - 1; y <= sectionY + 1; y++) {
                    setSectionDirty(x, y, z, false);
                }
            }
        }
    }

    public static void setSectionDirty(int sectionX, int sectionY, int sectionZ, boolean reRenderOnMainThread) {
        if (!LevelRendererFields.hasTerrainStore()) {
            return;
        }

        Set<ClientLevel> levels = Collections.newSetFromMap(new IdentityHashMap<>());
        Integer activeSlotId = ActiveSlot.idOrNull();
        if (activeSlotId != null) {
            markSlotLevel(activeSlotId, levels, sectionX, sectionY, sectionZ, reRenderOnMainThread);
            return;
        }

        for (int slotId = 0; slotId < PlayerSlots.MAX_SLOTS; slotId++) {
            markSlotLevel(slotId, levels, sectionX, sectionY, sectionZ, reRenderOnMainThread);
        }
    }

    private static void markSlotLevel(
        int slotId,
        Set<ClientLevel> levels,
        int sectionX,
        int sectionY,
        int sectionZ,
        boolean reRenderOnMainThread
    ) {
        PlayerSlot slot = LocalPlayers.INSTANCE.slots().slot(slotId);
        ClientLevel level = slot.renderState().level();
        if (level != null && levels.add(level)) {
            LevelRendererFields.terrainStore().setDirty(level, sectionX, sectionY, sectionZ, reRenderOnMainThread);
        }
    }
}
