package net.alnv.javareforged.ClientRuntime.terrain;

import javax.annotation.Nullable;
import net.alnv.javareforged.ClientRuntime.runtime.TerrainCoordinator;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;

public final class TerrainViewArea extends ViewArea {
    @Nullable
    private GlobalTerrainStore store;

    public TerrainViewArea(SectionRenderDispatcher dispatcher, ClientLevel level, int viewDistance, LevelRenderer levelRenderer) {
        super(dispatcher, level, viewDistance, levelRenderer);
        this.store = new GlobalTerrainStore(dispatcher, levelRenderer);
    }

    public GlobalTerrainStore store() {
        if (this.store == null) {
            throw new IllegalStateException("Terrain store is not initialized");
        }
        return this.store;
    }

    @Override
    protected void createSections(SectionRenderDispatcher dispatcher) {
        this.sections = new SectionRenderDispatcher.RenderSection[0];
    }

    @Override
    public void repositionCamera(double cameraX, double cameraZ) {
    }

    @Override
    public void setDirty(int sectionX, int sectionY, int sectionZ, boolean playerChanged) {
        this.store().setDirty(TerrainCoordinator.activeLevel(), sectionX, sectionY, sectionZ, playerChanged);
    }

    @Override
    @Nullable
    protected SectionRenderDispatcher.RenderSection getRenderSectionAt(BlockPos pos) {
        return this.store().sectionAt(TerrainCoordinator.activeLevel(), pos);
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
