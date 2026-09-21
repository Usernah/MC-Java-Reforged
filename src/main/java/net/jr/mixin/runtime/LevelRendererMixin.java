package net.jr.mixin.runtime;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.util.function.Consumer;
import net.minecraft.TracingExecutor;
import javax.annotation.Nullable;
import net.jr.client.runtime.terrain.TerrainCoordinator;
import net.jr.client.runtime.render.experimental.TerrainMultiDraw;
import net.jr.client.runtime.render.target.ViewportRenderTargets;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.joml.Matrix4fc;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow @Nullable
    private ViewArea viewArea;

    @ModifyExpressionValue(method = {"render", "doEntityOutline"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;entityOutlineTarget:Lcom/mojang/blaze3d/pipeline/RenderTarget;"))
    private RenderTarget javaReforged$resolveActiveOutlineTarget(RenderTarget original) {
        return ViewportRenderTargets.activeOutlineOr(original);
    }

    @Inject(method = "prepareChunkRenders", at = @At("HEAD"), cancellable = true)
    private void javaReforged$beginDrawPreparation(
        Matrix4fc modelViewMatrix,
        CallbackInfoReturnable<ChunkSectionsToRender> cir
    ) {
        ChunkSectionsToRender prepared = TerrainMultiDraw.prepare((LevelRenderer)(Object)this, modelViewMatrix);
        if (prepared != null) {
            cir.setReturnValue(prepared);
        }
    }

    @WrapOperation(
        method = "invalidateCompiledGeometry",
        at = @At(value = "NEW", target = "(Lnet/minecraft/TracingExecutor;Lnet/minecraft/client/renderer/RenderBuffers;Lnet/minecraft/client/renderer/chunk/SectionCompiler;Ljava/util/function/Consumer;)Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;")
    )
    private SectionRenderDispatcher javaReforged$createSharedSectionDispatcher(TracingExecutor executor, RenderBuffers renderBuffers, SectionCompiler compiler, Consumer<SectionRenderDispatcher.RenderSection> originalMeshUpdate, Operation<SectionRenderDispatcher> original) {
        return original.call(executor, renderBuffers, compiler, (Consumer<SectionRenderDispatcher.RenderSection>)TerrainCoordinator::onSectionCompiled);
    }

    @WrapOperation(
        method = "invalidateCompiledGeometry",
        at = @At(value = "NEW", target = "(Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;IIIIILnet/minecraft/client/renderer/SectionOcclusionGraph;)Lnet/minecraft/client/renderer/ViewArea;")
    )
    private ViewArea javaReforged$createLogicalSharedViewArea(SectionRenderDispatcher dispatcher, int minY, int maxY, int minSectionY, int maxSectionY, int viewDistance, SectionOcclusionGraph graph, Operation<ViewArea> original) {
        return TerrainCoordinator.createActiveViewArea(dispatcher, viewDistance, graph);
    }

    @Redirect(method = "invalidateCompiledGeometry", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;clearCompileQueue()V"))
    private void javaReforged$clearSharedCompileQueueOnce(SectionRenderDispatcher dispatcher) {
        TerrainCoordinator.clearSharedCompileQueueOnce(dispatcher);
    }

    @Inject(method = "invalidateCompiledGeometry", at = @At("RETURN"))
    private void javaReforged$captureLogicalViewArea(CallbackInfo ci) {
        TerrainCoordinator.captureViewArea(this.viewArea);
    }

    @Inject(
        method = "invalidateCompiledGeometry",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;clearCompileQueue()V", shift = At.Shift.BEFORE)
    )
    private void javaReforged$invalidateSharedCompiledGeometry(CallbackInfo ci) {
        TerrainCoordinator.invalidateSharedCompiledGeometry();
    }

    @Inject(method = "resetLevelRenderData", at = @At("HEAD"), cancellable = true)
    private void javaReforged$releaseOnlyActivePlayerTerrain(CallbackInfo ci) {
        if (TerrainCoordinator.resetActiveSlotTerrainIfScoped()) {
            this.viewArea = null;
            ci.cancel();
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;compileSections(Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"))
    private void javaReforged$compileAllPlayerRequestsFromPrimary(LevelRenderer renderer, CameraRenderState cameraState) {
        TerrainCoordinator.compileVisibleSlots(renderer);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;uploadTerrainBuffersToGpu()V"))
    private void javaReforged$uploadSharedTerrainOnce(SectionRenderDispatcher dispatcher) {
        TerrainCoordinator.uploadSharedTerrain(dispatcher);
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void javaReforged$closeSharedTerrainBeforeDispatcher(CallbackInfo ci) {
        TerrainCoordinator.closeSharedTerrain();
    }
}
