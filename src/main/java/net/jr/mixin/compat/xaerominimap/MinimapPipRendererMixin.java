package net.jr.mixin.compat.xaerominimap;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.jr.client.compat.xaero.XaeroMinimapPipState;
import net.jr.client.compat.xaero.XaeroMinimapPipRenderers;
import net.jr.client.compat.xaero.XaeroMinimapRuntimeState;
import net.jr.client.runtime.context.LocalClientExecution;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import xaero.hud.minimap.render.MinimapPipRenderState;
import xaero.hud.minimap.render.MinimapPipRenderer;

@Mixin(value = MinimapPipRenderer.class, remap = false)
abstract class MinimapPipRendererMixin {
    @ModifyArgs(
        method = "renderToTexture(Lxaero/hud/minimap/render/MinimapPipRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V",
        at = @At(
            value = "INVOKE",
            target = "Lxaero/common/minimap/MinimapProcessor;onRender(IIIIDFIIFLxaero/lib/client/graphics/XaeroBufferProvider;II)V"
        )
    )
    private void javaReforged$keepLocalRenderSize(Args args) {
        MinimapPipRenderState state = XaeroMinimapPipRenderers.renderingStateOrNull();
        if (state != null) {
            XaeroMinimapPipState viewportState = (XaeroMinimapPipState)state;
            args.set(10, viewportState.javaReforged$renderWidth());
            args.set(11, viewportState.javaReforged$renderHeight());
        }
    }

    @WrapMethod(
        method = "prepare(Lxaero/hud/minimap/render/MinimapPipRenderState;Lnet/minecraft/client/renderer/state/gui/GuiRenderState;Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;I)V"
    )
    private void javaReforged$prepareWithViewportScale(
        MinimapPipRenderState state,
        GuiRenderState guiRenderState,
        FeatureRenderDispatcher featureRenderDispatcher,
        int guiScale,
        Operation<Void> original
    ) {
        if (XaeroMinimapPipRenderers.isRouting()) {
            original.call(state, guiRenderState, featureRenderDispatcher, guiScale);
            return;
        }
        XaeroMinimapPipRenderers.prepare(state, guiRenderState, featureRenderDispatcher, guiScale);
    }

    @WrapMethod(
        method = "renderToTexture(Lxaero/hud/minimap/render/MinimapPipRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V"
    )
    private void javaReforged$renderOwningClient(
        MinimapPipRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        Operation<Void> original
    ) {
        int slotId = ((XaeroMinimapPipState)state).javaReforged$slotId();
        if (slotId < 0) {
            XaeroMinimapPipRenderers.render(
                state,
                () -> original.call(state, poseStack, submitNodeCollector)
            );
            return;
        }
        LocalClientExecution.runForClient(
            slotId,
            () -> XaeroMinimapRuntimeState.run(
                () -> XaeroMinimapPipRenderers.render(
                    state,
                    () -> original.call(state, poseStack, submitNodeCollector)
                )
            )
        );
    }

}
