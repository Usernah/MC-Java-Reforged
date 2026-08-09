package net.jr.ClientRuntime.terrain;

import javax.annotation.Nullable;
import net.jr.ClientRuntime.runtime.LevelRendererFields;
import net.jr.ClientRuntime.runtime.TerrainCoordinator;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

/** The one global ViewArea facade backed by the shared RenderSection store. */
public final class TerrainViewArea extends ViewArea {
    @Nullable
    private GlobalTerrainStore store;

    public static TerrainViewArea create(
        SectionRenderDispatcher dispatcher,
        ClientLevel level,
        int viewDistance,
        LevelRenderer levelRenderer
    ) {
        return SharedViewAreaConstruction.construct(() -> new TerrainViewArea(dispatcher, level, viewDistance, levelRenderer));
    }

    private TerrainViewArea(
        SectionRenderDispatcher dispatcher,
        ClientLevel level,
        int viewDistance,
        LevelRenderer levelRenderer
    ) {
        super(
            dispatcher,
            level.getMinY(),
            level.getMaxY(),
            level.getMinSectionY(),
            level.getMaxSectionY(),
            viewDistance,
            levelRenderer.sectionOcclusionGraph()
        );
        this.store = new GlobalTerrainStore(dispatcher, levelRenderer);
    }

    public GlobalTerrainStore store() {
        if (this.store == null) {
            throw new IllegalStateException("Terrain store is not initialized");
        }
        return this.store;
    }

    @Override
    public boolean repositionCamera(SectionPos cameraSectionPos) {
        // SlotTerrainView owns the independent camera windows.
        return false;
    }

    @Override
    public SectionPos getCameraSectionPos() {
        var camera = LevelRendererFields.terrain().nullableCameraPosition();
        return camera == null ? SectionPos.of(0, 0, 0) : SectionPos.of(camera);
    }

    @Override
    @Nullable
    public SectionRenderDispatcher.RenderSection getRenderSectionAt(BlockPos pos) {
        return this.store().sectionAt(TerrainCoordinator.activeLevel(), pos);
    }

    @Override
    @Nullable
    protected SectionRenderDispatcher.RenderSection getRenderSection(long sectionNode) {
        return this.store().sectionAt(TerrainCoordinator.activeLevel(), SectionPos.of(sectionNode).origin());
    }

    @Override
    public void releaseAllBuffers() {
        if (this.store != null) {
            TerrainCoordinator.releaseGraphs(this.store);
            this.store.close();
            this.store = null;
        }
    }
}
