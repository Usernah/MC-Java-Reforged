package net.jr.ClientRuntime.terrain;

import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

/** Per-slot occlusion-graph facade; all actual sections remain global. */
public final class TerrainGraphArea extends ViewArea {
    private final GlobalTerrainStore store;
    private final SlotTerrainView view;
    private final ClientLevel level;
    private final int slotId;
    private SectionPos cameraSectionPos = SectionPos.of(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);

    public static TerrainGraphArea create(
        SectionRenderDispatcher dispatcher,
        ClientLevel level,
        int viewDistance,
        SectionOcclusionGraph graph,
        GlobalTerrainStore store,
        SlotTerrainView view,
        int slotId
    ) {
        return SharedViewAreaConstruction.construct(
            () -> new TerrainGraphArea(dispatcher, level, viewDistance, graph, store, view, slotId)
        );
    }

    private TerrainGraphArea(
        SectionRenderDispatcher dispatcher,
        ClientLevel level,
        int viewDistance,
        SectionOcclusionGraph graph,
        GlobalTerrainStore store,
        SlotTerrainView view,
        int slotId
    ) {
        super(
            dispatcher,
            level.getMinY(),
            level.getMaxY(),
            level.getMinSectionY(),
            level.getMaxSectionY(),
            viewDistance,
            graph
        );
        this.store = store;
        this.view = view;
        this.level = level;
        this.slotId = slotId;
    }

    @Override
    @Nullable
    public SectionRenderDispatcher.RenderSection getRenderSectionAt(BlockPos pos) {
        return this.store.sectionForGraph(this.slotId, this.view, pos);
    }

    @Override
    @Nullable
    protected SectionRenderDispatcher.RenderSection getRenderSection(long sectionNode) {
        return this.store.sectionForGraph(this.slotId, this.view, SectionPos.of(sectionNode).origin());
    }

    @Override
    public boolean repositionCamera(SectionPos cameraSectionPos) {
        if (cameraSectionPos.equals(this.cameraSectionPos)) {
            return false;
        }
        this.cameraSectionPos = cameraSectionPos;
        return true;
    }

    @Override
    public SectionPos getCameraSectionPos() {
        return this.cameraSectionPos;
    }

    @Override
    public void releaseAllBuffers() {
        // This facade owns no RenderSection or GPU buffer.
    }

    public boolean matches(GlobalTerrainStore store, SlotTerrainView view, ClientLevel level, int viewDistance, int slotId) {
        return this.store == store
            && this.view == view
            && this.slotId == slotId
            && this.level == level
            && this.getViewDistance() == viewDistance;
    }

    public boolean belongsTo(GlobalTerrainStore store) {
        return this.store == store;
    }
}
