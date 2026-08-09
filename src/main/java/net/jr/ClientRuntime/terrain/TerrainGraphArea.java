package net.jr.ClientRuntime.terrain;

import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

/**
 * Lightweight per-player ViewArea facade over the one shared RenderSection pool.
 * It owns camera-window membership only; it never creates a second terrain engine.
 */
public final class TerrainGraphArea extends ViewArea {
    private final GlobalTerrainStore store;
    private final SlotTerrainView view;
    private final ClientLevel level;
    private final SectionOcclusionGraph graph;
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
        this.graph = graph;
        this.slotId = slotId;
    }

    @Override
    public boolean repositionCamera(SectionPos nextCameraSectionPos) {
        if (nextCameraSectionPos.equals(this.cameraSectionPos)) {
            return false;
        }
        SlotTerrainView.Update update = this.view.update(
            this.store,
            this.level,
            this.getViewDistance(),
            nextCameraSectionPos.x(),
            nextCameraSectionPos.z()
        );
        this.store.updateReferences(this.slotId, update);
        this.cameraSectionPos = nextCameraSectionPos;
        this.graph.invalidate();
        return true;
    }

    @Override
    public SectionPos getCameraSectionPos() {
        return this.cameraSectionPos;
    }

    @Override
    public @Nullable SectionRenderDispatcher.RenderSection getRenderSectionAt(BlockPos pos) {
        return this.store.sectionForGraph(this.slotId, this.view, pos);
    }

    @Override
    protected @Nullable SectionRenderDispatcher.RenderSection getRenderSection(long sectionNode) {
        return this.store.sectionForGraph(this.slotId, this.view, sectionNode);
    }

    @Override
    public void releaseAllBuffers() {
        this.store.releaseSlot(this.slotId, this.view);
        this.view.clear();
        this.cameraSectionPos = SectionPos.of(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    public boolean contains(SectionRenderDispatcher.RenderSection section) {
        return this.store.isReferencedBy(this.slotId, this.view, section);
    }

    public boolean matches(GlobalTerrainStore store, ClientLevel level, int viewDistance, int slotId) {
        return this.store == store
            && this.level == level
            && this.slotId == slotId
            && this.getViewDistance() == viewDistance;
    }
}
