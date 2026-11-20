package net.jr.client.runtime.render.pass;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.ReadinessPolicy;
import net.jr.client.runtime.context.ExecutionPhase;
import net.jr.client.runtime.render.world.WorldRenderBackend;
import net.jr.client.runtime.render.world.WorldRenderBackends;
import net.jr.client.runtime.render.world.WorldRenderView;
import net.jr.client.runtime.render.world.WorldRenderViews;
import net.jr.client.runtime.slot.Slots;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.runtime.viewport.ViewportRenderScope;
import net.jr.mixin.accessors.GameRendererAccessor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Vector4f;

public final class WorldRenderPass {
    private WorldRenderPass() {
    }

    public static void renderLevelForVisibleSlots(GameRenderer gameRenderer, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        clearUnusedViewports(gameRenderer);
        List<WorldRenderView> views = WorldRenderViews.collect(
            deltaTracker,
            ReadinessPolicy::gameplayReady
        );
        WorldRenderBackend backend = WorldRenderBackends.active();
        backend.beginRenderFrame(views);
        for (Slots slot : ClientRuntime.INSTANCE.viewports().drawableSlots()) {
            ViewportArea viewport = ClientRuntime.INSTANCE.viewports().viewport(slot.id());
            WorldRenderView view = viewForSlot(views, slot.id());
            if (view == null) {
                drawWaitingViewport(gameRenderer, slot, viewport);
                continue;
            }
            renderView(gameRenderer, view);
        }
    }

    private static void clearUnusedViewports(GameRenderer gameRenderer) {
        var viewports = ClientRuntime.INSTANCE.viewports();
        if (!viewports.hasWindowMetrics()) {
            return;
        }

        var table = viewports.viewportTable();
        boolean[] occupied = new boolean[table.size()];
        for (var drawable : viewports.drawableViewports()) {
            occupied[drawable.viewport().id()] = true;
        }

        RenderTarget target = gameRenderer.mainRenderTarget();
        for (ViewportArea viewport : table) {
            if (occupied[viewport.id()]) {
                continue;
            }
            RenderPass.RenderArea area = ViewportRenderScope.areaFor(
                viewport,
                target.width,
                target.height
            );
            RenderSystem.getDevice()
                .createCommandEncoder()
                .clearColorAndDepthTextures(
                    target.getColorTexture(),
                    new Vector4f(0.0F, 0.0F, 0.0F, 1.0F),
                    target.getDepthTexture(),
                    0.0D,
                    area.x(),
                    area.y(),
                    area.width(),
                    area.height()
                );
        }
    }

    private static void renderView(
        GameRenderer gameRenderer,
        WorldRenderView view
    ) {
        view.slot().runForSlot(
            view,
            ExecutionPhase.WORLD_RENDER,
            () -> gameRenderer.renderLevel(view.deltaTracker())
        );
    }

    private static WorldRenderView viewForSlot(List<WorldRenderView> views, int slotId) {
        for (WorldRenderView view : views) {
            if (view.slotId() == slotId) {
                return view;
            }
        }
        return null;
    }

    private static void drawWaitingViewport(GameRenderer gameRenderer, Slots slot, ViewportArea viewport) {
        RenderTarget target = gameRenderer.mainRenderTarget();
        RenderPass.RenderArea area = ViewportRenderScope.areaFor(viewport, target.width, target.height);
        float tint = 0.12F + slot.id() * 0.08F;
        RenderSystem.getDevice()
            .createCommandEncoder()
            .clearColorAndDepthTextures(
                target.getColorTexture(),
                new Vector4f(tint, 0.06F, 0.08F + slot.id() * 0.1F, 1.0F),
                target.getDepthTexture(),
                0.0,
                area.x(),
                area.y(),
                area.width(),
                area.height()
            );
    }
}
