package net.jr.mixin.compat.xaerominimap;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.jr.client.compat.xaero.XaeroMinimapPipState;
import net.jr.client.compat.xaero.XaeroMinimapPipRenderers;
import net.jr.client.compat.xaero.XaeroMinimapRuntimeState;
import net.jr.client.runtime.context.InstanceExecution;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import xaero.hud.minimap.render.MinimapPipRenderState;
import xaero.hud.minimap.render.MinimapPipRenderer;

@Mixin(value = MinimapPipRenderer.class, remap = false)
abstract class MinimapPipRendererMixin {
    @Unique
    private static Method javaReforged$getBufferProvider;
    @Unique
    private static Method javaReforged$endBufferBatch;
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
                () -> javaReforged$renderAndFlush(
                    state,
                    () -> original.call(state, poseStack, submitNodeCollector)
                )
            );
            return;
        }
        InstanceExecution.runForClient(
            slotId,
            () -> XaeroMinimapRuntimeState.run(
                () -> XaeroMinimapPipRenderers.render(
                    state,
                    () -> javaReforged$renderAndFlush(
                        state,
                        () -> original.call(state, poseStack, submitNodeCollector)
                    )
                )
            )
        );
    }

    private static void javaReforged$renderAndFlush(
        MinimapPipRenderState state,
        Runnable render
    ) {
        try {
            render.run();
        } finally {
            // Xaero's waypoint world renderer uses the same global provider. Leaving even
            // one minimap batch queued makes it draw later with the world projection.
            javaReforged$flush(state);
        }
    }

    private static void javaReforged$flush(MinimapPipRenderState state) {
        try {
            Method getter = javaReforged$getBufferProvider;
            if (getter == null) {
                getter = state.getClass().getMethod("getCvc");
                javaReforged$getBufferProvider = getter;
            }
            Object provider = getter.invoke(state);
            if (provider == null) {
                return;
            }
            Method flush = javaReforged$endBufferBatch;
            if (flush == null || flush.getDeclaringClass() != provider.getClass()) {
                flush = provider.getClass().getMethod("endBatch");
                javaReforged$endBufferBatch = flush;
            }
            flush.invoke(provider);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("No se pudo vaciar el buffer del minimapa", cause);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("No se pudo acceder al buffer del minimapa", exception);
        }
    }

}
