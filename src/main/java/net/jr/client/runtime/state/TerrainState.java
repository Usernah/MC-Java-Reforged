package net.jr.client.runtime.state;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import javax.annotation.Nullable;
import net.jr.client.runtime.terrain.SharedTerrainStore;
import net.jr.client.runtime.terrain.LocalTerrainView;
import net.jr.client.runtime.terrain.TerrainGraphArea;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.world.phys.Vec3;

/** Per-player state consumed by Minecraft's one shared LevelRenderer. */
public final class TerrainState {
    private final SectionOcclusionGraph sectionOcclusionGraph = new SectionOcclusionGraph();
    private final ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections = new ObjectArrayList<>(10000);
    private final ObjectArrayList<SectionRenderDispatcher.RenderSection> nearbyVisibleSections = new ObjectArrayList<>(256);
    private final LocalTerrainView view = new LocalTerrainView();
    @Nullable
    private TerrainGraphArea viewArea;
    @Nullable
    private Vec3 cameraPosition;

    public SectionOcclusionGraph sectionOcclusionGraph() {
        return this.sectionOcclusionGraph;
    }

    public ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections() {
        return this.visibleSections;
    }

    public ObjectArrayList<SectionRenderDispatcher.RenderSection> nearbyVisibleSections() {
        return this.nearbyVisibleSections;
    }

    public LocalTerrainView view() {
        return this.view;
    }

    public @Nullable TerrainGraphArea viewArea() {
        return this.viewArea;
    }

    public void setViewArea(@Nullable TerrainGraphArea viewArea) {
        this.viewArea = viewArea;
    }

    public TerrainGraphArea ensureViewArea(
        SharedTerrainStore store,
        ClientLevel level,
        int viewDistance,
        int slotId
    ) {
        if (this.viewArea == null || !this.viewArea.matches(store, level, viewDistance, slotId)) {
            this.releaseViewArea();
            this.viewArea = TerrainGraphArea.create(
                store.dispatcher(),
                level,
                viewDistance,
                this.sectionOcclusionGraph,
                store,
                this.view,
                slotId
            );
            this.sectionOcclusionGraph.waitAndReset(this.viewArea);
        }
        return this.viewArea;
    }

    public boolean contains(SectionRenderDispatcher.RenderSection section) {
        return this.viewArea != null && this.viewArea.contains(section);
    }

    public @Nullable Vec3 nullableCameraPosition() {
        return this.cameraPosition;
    }

    public void setCameraPosition(Vec3 cameraPosition) {
        this.cameraPosition = cameraPosition;
    }

    public void releaseViewArea() {
        if (this.viewArea != null) {
            this.sectionOcclusionGraph.waitAndReset(null);
            this.viewArea.releaseAllBuffers();
            this.viewArea = null;
        }
        this.visibleSections.clear();
        this.nearbyVisibleSections.clear();
        this.view.clear();
    }

    public void clear() {
        this.releaseViewArea();
        this.cameraPosition = null;
    }
}
