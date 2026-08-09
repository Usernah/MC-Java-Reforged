package net.alnv.javareforged.ClientRuntime.runtime;

import java.util.Queue;
import java.util.Set;
import net.alnv.javareforged.mixin.SSM.RenderSectionSSAccessor;
import net.alnv.javareforged.mixin.SSM.SectionRenderDispatcherAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public final class TerrainWorkload {
    private static final long UPLOAD_BUDGET_NANOS = 2_000_000L;
    private static final int MIN_UPLOADS_PER_FRAME = 1;
    private static final int MAX_UPLOADS_PER_FRAME = 4;
    private static final int MAX_BACKGROUND_REBUILD_STARTS_PER_FRAME = 1;
    private static final int MAX_DIRTY_CHECKS_PER_FRAME = 256;

    private TerrainWorkload() {
    }

    public static void drainUploads(SectionRenderDispatcher dispatcher) {
        Queue<Runnable> pending = ((SectionRenderDispatcherAccessor)dispatcher).splitTest$getPendingUploads();
        long deadline = System.nanoTime() + UPLOAD_BUDGET_NANOS;
        for (int completed = 0; completed < MAX_UPLOADS_PER_FRAME; completed++) {
            if (completed >= MIN_UPLOADS_PER_FRAME && System.nanoTime() >= deadline) {
                break;
            }
            Runnable upload = pending.poll();
            if (upload == null) {
                break;
            }
            upload.run();
        }
    }

    public static int asyncRebuildStartAllowance(
        SectionRenderDispatcher dispatcher,
        boolean visibleWorkPending
    ) {
        int available = Math.max(0, dispatcher.getFreeBufferCount() - dispatcher.getToBatchCount());
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
        accessor.splitTest$updateGlobalBlockEntities(Set.of());
        accessor.splitTest$setCompiled(SectionRenderDispatcher.CompiledSection.EMPTY);
        renderSection.setNotDirty();
        return true;
    }
}
