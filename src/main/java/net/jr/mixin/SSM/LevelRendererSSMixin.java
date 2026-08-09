package net.jr.mixin.SSM;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import javax.annotation.Nullable;
import net.jr.ClientRuntime.bridge.LevelRendererSSAccessor;
import net.jr.ClientRuntime.runtime.ActiveSlot;
import net.jr.ClientRuntime.runtime.Client;
import net.jr.ClientRuntime.runtime.TerrainCoordinator;
import net.jr.ClientRuntime.runtime.TerrainPhase;
import net.jr.ClientRuntime.runtime.SlotRenderTargets;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.multiplayer.ClientLevel;
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
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Makes vanilla's single LevelRenderer consume the active player's state tree. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererSSMixin implements LevelRendererSSAccessor {
    @Shadow @Final @Mutable
    private LevelRenderState levelRenderState;
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
    public @Nullable SectionRenderDispatcher splitTest$getSectionRenderDispatcher() {
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
    public @Nullable ViewArea splitTest$getViewArea() {
        return this.viewArea;
    }

    @Override
    public void splitTest$setViewArea(@Nullable ViewArea viewArea) {
        this.viewArea = viewArea;
    }

    @Override
    @Invoker("compileSections")
    public abstract void splitTest$compileSections(CameraRenderState cameraState);

    @ModifyExpressionValue(
        method = {"render", "doEntityOutline"},
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;entityOutlineTarget:Lcom/mojang/blaze3d/pipeline/RenderTarget;")
    )
    private RenderTarget splitTest$resolveActiveOutlineTarget(RenderTarget original) {
        return SlotRenderTargets.activeOutlineOr(original);
    }

    @WrapOperation(
        method = "invalidateCompiledGeometry",
        at = @At(value = "NEW", target = "(Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;IIIIILnet/minecraft/client/renderer/SectionOcclusionGraph;)Lnet/minecraft/client/renderer/ViewArea;")
    )
    private ViewArea splitTest$createLogicalSharedViewArea(
        SectionRenderDispatcher dispatcher,
        int minY,
        int maxY,
        int minSectionY,
        int maxSectionY,
        int viewDistance,
        SectionOcclusionGraph graph,
        Operation<ViewArea> original
    ) {
        ClientLevel level = Client.level();
        if (level == null) {
            throw new IllegalStateException("Cannot create terrain ViewArea without an active ClientLevel");
        }
        return TerrainCoordinator.createViewArea(dispatcher, level, viewDistance, graph);
    }

    @Redirect(
        method = "invalidateCompiledGeometry",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;clearCompileQueue()V")
    )
    private void splitTest$clearSharedCompileQueueOnce(SectionRenderDispatcher dispatcher) {
        if (TerrainCoordinator.isPrimaryTerrainPass()) {
            dispatcher.clearCompileQueue();
        }
    }

    @Inject(method = "invalidateCompiledGeometry", at = @At("RETURN"))
    private void splitTest$captureLogicalViewArea(CallbackInfo ci) {
        TerrainCoordinator.captureViewArea(this.viewArea);
    }

    @Inject(method = "resetLevelRenderData", at = @At("HEAD"), cancellable = true)
    private void splitTest$releaseOnlyActivePlayerTerrain(CallbackInfo ci) {
        if (ActiveSlot.idOrNull() == null) {
            return;
        }
        TerrainCoordinator.resetActiveSlotTerrain();
        this.viewArea = null;
        ci.cancel();
    }

    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;compileSections(Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V")
    )
    private void splitTest$compileAllPlayerRequestsFromPrimary(LevelRenderer renderer, CameraRenderState cameraState) {
        if (TerrainPhase.canUpdateTerrain()) {
            TerrainCoordinator.compileVisibleSlots(renderer);
        }
    }

    @Redirect(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;uploadTerrainBuffersToGpu()V")
    )
    private void splitTest$uploadSharedTerrainOnce(SectionRenderDispatcher dispatcher) {
        if (TerrainPhase.canUpdateTerrain()) {
            dispatcher.uploadTerrainBuffersToGpu();
        }
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void splitTest$closeSharedTerrainBeforeDispatcher(CallbackInfo ci) {
        TerrainCoordinator.closeSharedTerrain();
    }
}
