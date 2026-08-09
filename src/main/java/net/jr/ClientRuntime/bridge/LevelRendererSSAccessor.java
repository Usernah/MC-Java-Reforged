package net.jr.ClientRuntime.bridge;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.state.level.LevelRenderState;

/** Bridge implemented by the 26.2 LevelRenderer mixin. */
public interface LevelRendererSSAccessor {
    SectionRenderDispatcher splitTest$getSectionRenderDispatcher();

    LevelRenderState splitTest$getLevelRenderState();

    void splitTest$setLevelRenderState(LevelRenderState state);

    SectionOcclusionGraph splitTest$getSectionOcclusionGraph();

    void splitTest$setSectionOcclusionGraph(SectionOcclusionGraph graph);

    ObjectArrayList<SectionRenderDispatcher.RenderSection> splitTest$getVisibleSections();

    void splitTest$setVisibleSections(ObjectArrayList<SectionRenderDispatcher.RenderSection> sections);

    ObjectArrayList<SectionRenderDispatcher.RenderSection> splitTest$getNearbyVisibleSections();

    void splitTest$setNearbyVisibleSections(ObjectArrayList<SectionRenderDispatcher.RenderSection> sections);

    @Nullable
    ViewArea splitTest$getViewArea();

    void splitTest$setViewArea(@Nullable ViewArea viewArea);
}
