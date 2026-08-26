package net.jr.mixin.accessors;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {
    @Accessor("sectionRenderDispatcher")
    @Nullable SectionRenderDispatcher javaReforged$getSectionRenderDispatcher();

    @Accessor("levelRenderState")
    LevelRenderState javaReforged$getLevelRenderState();

    @Mutable
    @Accessor("levelRenderState")
    void javaReforged$setLevelRenderState(LevelRenderState state);

    @Accessor("sectionOcclusionGraph")
    SectionOcclusionGraph javaReforged$getSectionOcclusionGraph();

    @Mutable
    @Accessor("sectionOcclusionGraph")
    void javaReforged$setSectionOcclusionGraph(SectionOcclusionGraph graph);

    @Accessor("visibleSections")
    ObjectArrayList<SectionRenderDispatcher.RenderSection> javaReforged$getVisibleSections();

    @Mutable
    @Accessor("visibleSections")
    void javaReforged$setVisibleSections(ObjectArrayList<SectionRenderDispatcher.RenderSection> sections);

    @Accessor("nearbyVisibleSections")
    ObjectArrayList<SectionRenderDispatcher.RenderSection> javaReforged$getNearbyVisibleSections();

    @Mutable
    @Accessor("nearbyVisibleSections")
    void javaReforged$setNearbyVisibleSections(ObjectArrayList<SectionRenderDispatcher.RenderSection> sections);

    @Accessor("viewArea")
    @Nullable ViewArea javaReforged$getViewArea();

    @Accessor("viewArea")
    void javaReforged$setViewArea(@Nullable ViewArea viewArea);

    @Invoker("repositionCamera")
    void javaReforged$repositionCamera(CameraRenderState cameraState);

    @Invoker("compileSections")
    void javaReforged$compileSections(CameraRenderState cameraState);
}
