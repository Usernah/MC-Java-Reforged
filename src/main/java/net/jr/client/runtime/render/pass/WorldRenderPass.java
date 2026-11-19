package net.jr.client.runtime.render.pass;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.ReadinessPolicy;
import net.jr.client.runtime.context.InstanceExecution;
import net.jr.client.runtime.render.world.WorldRenderBackend;
import net.jr.client.runtime.render.world.WorldRenderBackends;
import net.jr.client.runtime.render.world.WorldRenderContextAccess;
import net.jr.client.runtime.render.world.WorldRenderView;
import net.jr.client.runtime.render.world.WorldRenderViews;
import net.jr.client.runtime.render.target.ViewportRenderTargets;
import net.jr.client.runtime.render.state.WorldEngineStateScope;
import net.jr.client.runtime.slot.Slots;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.runtime.viewport.ViewportRenderScope;
import net.jr.mixin.accessors.GameRendererAccessor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;
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
            renderView(minecraft, gameRenderer, backend, view);
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
        Minecraft minecraft,
        GameRenderer gameRenderer,
        WorldRenderBackend backend,
        WorldRenderView view
    ) {
        Slots slot = view.slot();
        ViewportArea viewport = view.viewport();
        DeltaTracker deltaTracker = view.deltaTracker();
        try (
                InstanceExecution.Scope ignoredClient = InstanceExecution.enterForClient(minecraft, slot.id());
                WorldRenderContextAccess.Scope ignoredContext = WorldRenderContextAccess.enter(view);
                ViewportRenderScope.Scope ignoredViewport = ViewportRenderScope.enter(viewport);
                WorldEngineStateScope ignoredEngines = WorldEngineStateScope.bind(minecraft, slot);
                WorldRenderBackend.Scope ignoredBackend = backend.enterRenderView(view)
        ) {
            GameRendererAccessor renderer = (GameRendererAccessor)(Object)gameRenderer;
            var gameState = gameRenderer.gameRenderState();
            var optionsState = gameState.optionsRenderState;

            gameState.guiRenderState.isHudHidden = minecraft.gui.hud.isHidden();

            optionsState.cameraType =
                    slot.optionsState().cameraType();

            optionsState.fov =
                    slot.optionsState().fov().get();

            renderer.javaReforged$getGlobalSettingsUniform().update(
                viewport.width(),
                viewport.height(),
                optionsState.glintStrength,
                slot.renderState().level().getGameTime(),
                deltaTracker,
                optionsState.menuBackgroundBlurriness,
                slot.renderState().levelRenderState().cameraRenderState.pos,
                optionsState.textureFiltering == TextureFilteringMethod.RGSS
            );
            renderer.javaReforged$getLightmap().render(slot.renderState().lightmapRenderState());
            RenderTarget globalTarget = gameRenderer.mainRenderTarget();
            try (ViewportRenderTargets.Scope ignoredTargets = ViewportRenderTargets.enter(slot.id(), viewport, globalTarget)) {
                minecraft.level = view.level();
                minecraft.player = view.player();
                gameRenderer.renderLevel(deltaTracker);
                minecraft.levelRenderer.doEntityOutline();
                Identifier postEffect = gameRenderer.currentPostEffect();
                if (postEffect != null && renderer.javaReforged$isPostEffectActive()) {
                    PostChain chain = minecraft.getShaderManager().getPostChain(postEffect, LevelTargetBundle.MAIN_TARGETS);
                    if (chain != null) {
                        chain.process(gameRenderer.mainRenderTarget(), renderer.javaReforged$getResourcePool());
                    }
                }
                ignoredTargets.present();
            }
        }
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
