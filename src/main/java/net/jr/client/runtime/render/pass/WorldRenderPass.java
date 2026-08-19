package net.jr.client.runtime.render.pass;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.client.LocalClientReadinessPolicy;
import net.jr.client.runtime.context.LocalClientExecution;
import net.jr.client.runtime.render.target.ViewportRenderTargets;
import net.jr.client.runtime.terrain.TerrainCoordinator;
import net.jr.client.runtime.terrain.TerrainWorkPhase;
import net.jr.client.runtime.render.state.WorldEngineStateScope;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.runtime.viewport.ViewportRenderScope;
import net.jr.mixin.runtime.GameRendererSSAccessor;
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
        ClientRuntime players = ClientRuntime.INSTANCE;
        TerrainCoordinator.prepareFrame();
        int terrainDriverSlotId = selectTerrainDriver(players);
        for (LocalClientSlot slot : players.viewports().drawableSlots()) {
            ViewportArea viewport = players.viewports().viewport(slot.id());
            slot.renderState().setAspectRatio(viewport.aspectRatio());
            if (!canRenderLevel(slot)) {
                drawWaitingViewport(gameRenderer, slot, viewport);
                continue;
            }
            renderSlot(minecraft, gameRenderer, deltaTracker, slot, viewport, slot.id() == terrainDriverSlotId);
        }
    }

    private static void renderSlot(
        Minecraft minecraft,
        GameRenderer gameRenderer,
        DeltaTracker deltaTracker,
        LocalClientSlot slot,
        ViewportArea viewport,
        boolean updateTerrain
    ) {
        try (
            LocalClientExecution.Scope ignoredClient = LocalClientExecution.enterForClient(minecraft, slot.id());
            TerrainWorkPhase.Scope ignoredTerrain = updateTerrain ? TerrainWorkPhase.update() : TerrainWorkPhase.drawOnly();
            ViewportRenderScope.Scope ignoredViewport = ViewportRenderScope.enter(viewport);
            WorldEngineStateScope ignoredEngines = WorldEngineStateScope.bind(minecraft, slot)
        ) {
            GameRendererSSAccessor renderer = (GameRendererSSAccessor)(Object)gameRenderer;
            var gameState = gameRenderer.gameRenderState();
            var optionsState = gameState.optionsRenderState;

            optionsState.cameraType =
                    slot.optionsState().cameraType();

            optionsState.fov =
                    slot.optionsState().fov().get();

            renderer.splitTest$getGlobalSettingsUniform().update(
                    viewport.width(),
                    viewport.height(),
                    optionsState.glintStrength,
                    slot.renderState().level().getGameTime(),
                    deltaTracker,
                    optionsState.menuBackgroundBlurriness,
                    slot.renderState().levelRenderState().cameraRenderState.pos,
                    optionsState.textureFiltering == TextureFilteringMethod.RGSS
            );
            renderer.splitTest$getGlobalSettingsUniform().update(
                viewport.width(),
                viewport.height(),
                optionsState.glintStrength,
                slot.renderState().level().getGameTime(),
                deltaTracker,
                optionsState.menuBackgroundBlurriness,
                slot.renderState().levelRenderState().cameraRenderState.pos,
                optionsState.textureFiltering == TextureFilteringMethod.RGSS
            );
            renderer.splitTest$getLightmap().render(slot.renderState().lightmapRenderState());
            RenderTarget globalTarget = gameRenderer.mainRenderTarget();
            try (ViewportRenderTargets.Scope ignoredTargets = ViewportRenderTargets.enter(slot.id(), viewport, globalTarget)) {
                gameRenderer.renderLevel(deltaTracker);
                minecraft.levelRenderer.doEntityOutline();
                Identifier postEffect = gameRenderer.currentPostEffect();
                if (postEffect != null && renderer.splitTest$isPostEffectActive()) {
                    PostChain chain = minecraft.getShaderManager().getPostChain(postEffect, LevelTargetBundle.MAIN_TARGETS);
                    if (chain != null) {
                        chain.process(gameRenderer.mainRenderTarget(), renderer.splitTest$getResourcePool());
                    }
                }
                ignoredTargets.present();
            }
        }
    }

    private static boolean canRenderLevel(LocalClientSlot slot) {
        LocalClient client = ClientRuntime.INSTANCE.clients().clientOrNull(slot.id());
        return client != null && LocalClientReadinessPolicy.gameplayReady(client);
    }

    private static int selectTerrainDriver(ClientRuntime players) {
        for (LocalClientSlot slot : players.viewports().drawableSlots()) {
            if (canRenderLevel(slot) && slot.gameplayState().player().isAlive()) {
                return slot.id();
            }
        }
        return -1;
    }

    private static void drawWaitingViewport(GameRenderer gameRenderer, LocalClientSlot slot, ViewportArea viewport) {
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
