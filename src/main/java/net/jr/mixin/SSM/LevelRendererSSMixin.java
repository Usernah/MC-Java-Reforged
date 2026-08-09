package net.jr.mixin.SSM;

import net.jr.ClientRuntime.bridge.LevelRendererSSAccessor;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import javax.annotation.Nullable;
import net.jr.ClientRuntime.runtime.ActiveSlot;
import net.jr.ClientRuntime.runtime.Client;
import net.jr.ClientRuntime.runtime.LevelRendererFields;
import net.jr.ClientRuntime.runtime.TerrainCoordinator;
import net.jr.ClientRuntime.runtime.TerrainPhase;
import net.jr.ClientRuntime.terrain.TerrainViewArea;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererSSMixin implements LevelRendererSSAccessor {
    @Shadow @Final @Mutable
    LevelRenderState levelRenderState;
    @Shadow @Final @Mutable
    private SectionOcclusionGraph sectionOcclusionGraph;
    @Shadow @Final @Mutable
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections;
    @Shadow @Final @Mutable
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> nearbyVisibleSections;
    @Shadow @Nullable
    private ViewArea viewArea;
    @Shadow @Nullable
    private SectionRenderDispatcher sectionRenderDispatcher;

    @Override
    public SectionRenderDispatcher splitTest$getSectionRenderDispatcher() {
        return this.sectionRenderDispatcher;
    }

    @Override
    public LevelRenderState splitTest$getLevelRenderState() {
        return this.levelRenderState;
    }

    @Override
    public void splitTest$setLevelRenderState(LevelRenderState state) {
        this.levelRenderState = state;
    }

    @Override
    public SectionOcclusionGraph splitTest$getSectionOcclusionGraph() {
        return this.sectionOcclusionGraph;
    }

    @Override
    public void splitTest$setSectionOcclusionGraph(SectionOcclusionGraph graph) {
        this.sectionOcclusionGraph = graph;
    }

    @Override
    public ObjectArrayList<SectionRenderDispatcher.RenderSection> splitTest$getVisibleSections() {
        return this.visibleSections;
    }

    @Override
    public void splitTest$setVisibleSections(ObjectArrayList<SectionRenderDispatcher.RenderSection> sections) {
        this.visibleSections = sections;
    }

    @Override
    public ObjectArrayList<SectionRenderDispatcher.RenderSection> splitTest$getNearbyVisibleSections() {
        return this.nearbyVisibleSections;
    }

    @Override
    public void splitTest$setNearbyVisibleSections(ObjectArrayList<SectionRenderDispatcher.RenderSection> sections) {
        this.nearbyVisibleSections = sections;
    }

    @Override
    public ViewArea splitTest$getViewArea() {
        return this.viewArea;
    }

    @Override
    public void splitTest$setViewArea(@Nullable ViewArea viewArea) {
        this.viewArea = viewArea;
    }

    @Inject(method = "invalidateCompiledGeometry", at = @At("HEAD"), cancellable = true)
    private void splitTest$keepSingleTerrainEngine(CallbackInfo ci) {
        if (!LevelRendererFields.hasTerrainStore()) {
            return;
        }
        int requestedDistance = net.minecraft.client.Minecraft.getInstance().options.getEffectiveRenderDistance();
        boolean primaryReconfiguration = ActiveSlot.idOrNull() != null
            && ActiveSlot.idOrNull() == 0
            && LevelRendererFields.viewArea().getViewDistance() != requestedDistance;
        if (!primaryReconfiguration) {
            this.viewArea = LevelRendererFields.nullableViewArea();
            ci.cancel();
        }
    }

    @WrapOperation(
        method = "invalidateCompiledGeometry",
        at = @At(value = "NEW", target = "(Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;IIIIILnet/minecraft/client/renderer/SectionOcclusionGraph;)Lnet/minecraft/client/renderer/ViewArea;")
    )
    private ViewArea splitTest$createSharedViewArea(
        SectionRenderDispatcher dispatcher,
        int minY,
        int maxY,
        int minSectionY,
        int maxSectionY,
        int viewDistance,
        SectionOcclusionGraph graph,
        Operation<ViewArea> original
    ) {
        return TerrainViewArea.create(dispatcher, TerrainCoordinator.activeLevel(), viewDistance, (LevelRenderer)(Object)this);
    }

    @Inject(method = "invalidateCompiledGeometry", at = @At("RETURN"))
    private void splitTest$captureSharedViewArea(CallbackInfo ci) {
        LevelRendererFields.setViewArea(this.viewArea);
    }

    @Inject(method = "resetLevelRenderData", at = @At("HEAD"), cancellable = true)
    private void splitTest$onlyPrimaryResetsTerrain(CallbackInfo ci) {
        Integer slotId = ActiveSlot.idOrNull();
        if (slotId != null && slotId != 0) {
            this.viewArea = LevelRendererFields.nullableViewArea();
            ci.cancel();
        }
    }

    @Inject(method = "resetLevelRenderData", at = @At("RETURN"))
    private void splitTest$forgetReleasedTerrain(CallbackInfo ci) {
        if (this.viewArea == null) {
            LevelRendererFields.setViewArea(null);
        }
    }

    @Inject(method = "compileSections", at = @At("HEAD"), cancellable = true)
    private void splitTest$compileGlobalTerrain(CameraRenderState camera, CallbackInfo ci) {
        TerrainCoordinator.compileSections(Client.camera());
        ci.cancel();
    }

    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;uploadTerrainBuffersToGpu()V")
    )
    private void splitTest$uploadTerrainOnce(SectionRenderDispatcher dispatcher) {
        if (TerrainPhase.canUpdateTerrain()) {
            dispatcher.uploadTerrainBuffersToGpu();
        }
    }
}
