package net.alnv.javareforged.ClientRuntime.terrain;

import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;

public final class TerrainGraphArea extends ViewArea {
    private final GlobalTerrainStore store;
    private final SlotTerrainView view;
    private final int slotId;

    public TerrainGraphArea(
        SectionRenderDispatcher dispatcher,
        ClientLevel level,
        int viewDistance,
        LevelRenderer levelRenderer,
        GlobalTerrainStore store,
        SlotTerrainView view,
        int slotId
    ) {
        super(dispatcher, level, viewDistance, levelRenderer);
        this.store = store;
        this.view = view;
        this.slotId = slotId;
        int diameter = viewDistance * 2 + 1;
        this.sections = new SectionRenderDispatcher.RenderSection[diameter * diameter * level.getSectionsCount()];
    }

    @Override
    protected void createSections(SectionRenderDispatcher dispatcher) {
        this.sections = new SectionRenderDispatcher.RenderSection[0];
    }

    @Override
    @Nullable
    public SectionRenderDispatcher.RenderSection getRenderSectionAt(BlockPos pos) {
        return this.store.sectionForGraph(this.slotId, this.view, pos);
    }

    @Override
    public void repositionCamera(double cameraX, double cameraZ) {
    }

    @Override
    public void releaseAllBuffers() {
    }

    public boolean matches(GlobalTerrainStore store, SlotTerrainView view, ClientLevel level, int viewDistance, int slotId) {
        return this.store == store
            && this.view == view
            && this.slotId == slotId
            && this.getLevelHeightAccessor() == level
            && this.getViewDistance() == viewDistance;
    }

    public boolean belongsTo(GlobalTerrainStore store) {
        return this.store == store;
    }
}
