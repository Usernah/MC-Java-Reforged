package net.jr.ClientRuntime.runtime;

import net.jr.mixin.SSM.RenderSectionSSAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public final class TerrainWorkload {
    private static final int MAX_BACKGROUND_REBUILD_STARTS_PER_FRAME = 1;
    private static final int MAX_DIRTY_CHECKS_PER_FRAME = 256;

    private TerrainWorkload() {
    }

    public static int asyncRebuildStartAllowance(
        SectionRenderDispatcher dispatcher,
        boolean visibleWorkPending
    ) {
        int available = Math.max(0, dispatcher.getFreeBufferCount() - dispatcher.getCompileQueueSize());
        return visibleWorkPending ? available : Math.min(available, MAX_BACKGROUND_REBUILD_STARTS_PER_FRAME);
    }

    public static int dirtyCheckAllowance() {
        return MAX_DIRTY_CHECKS_PER_FRAME;
    }

    public static int materializationAllowance(SectionRenderDispatcher dispatcher) {
        return Math.max(32, dispatcher.getFreeBufferCount() * 16);
    }

    public static boolean completeIfEmpty(
        ClientLevel level,
        SectionPos sectionPos,
        SectionRenderDispatcher.RenderSection renderSection
    ) {
        LevelChunk chunk = level.getChunkSource().getChunk(sectionPos.x(), sectionPos.z(), false);
        if (chunk == null) {
            return false;
        }
        int sectionIndex = level.getSectionIndexFromSectionY(sectionPos.y());
        LevelChunkSection[] sections = chunk.getSections();
        if (sectionIndex < 0 || sectionIndex >= sections.length || !sections[sectionIndex].hasOnlyAir()) {
            return false;
        }

        RenderSectionSSAccessor accessor = (RenderSectionSSAccessor)(Object)renderSection;
        accessor.splitTest$setSectionMesh(CompiledSectionMesh.EMPTY);
        return true;
    }
}
