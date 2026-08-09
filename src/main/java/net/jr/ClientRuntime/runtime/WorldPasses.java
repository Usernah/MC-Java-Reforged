package net.jr.ClientRuntime.runtime;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.jr.ClientRuntime.slot.PlayerSlot;
import net.jr.ClientRuntime.viewport.ViewportArea;
import net.jr.mixin.SSM.GameRendererSSAccessor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Vector4f;

/** Renders every visible local player through one shared GameRenderer/LevelRenderer pair. */
public final class WorldPasses {
    private WorldPasses() {
    }

    public static void renderLevelForVisibleSlots(GameRenderer gameRenderer, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayers players = LocalPlayers.INSTANCE;
        TerrainCoordinator.prepareFrame();
        for (PlayerSlot slot : players.slots().visibleSlots()) {
            ViewportArea viewport = slot.viewport();
            slot.renderState().setAspectRatio(viewport.aspectRatio());
            if (!canRenderLevel(slot)) {
                drawWaitingViewport(gameRenderer, slot, viewport);
                continue;
            }
            // Only slot 0 may build, compile or upload the one global terrain engine.
            renderSlot(minecraft, gameRenderer, deltaTracker, slot, viewport, slot.id() == 0);
        }
    }

    private static void renderSlot(
        Minecraft minecraft,
        GameRenderer gameRenderer,
        DeltaTracker deltaTracker,
        PlayerSlot slot,
        ViewportArea viewport,
        boolean updateTerrain
    ) {
        try (
            ClientBoundary.Scope ignoredClient = ClientBoundary.enterForSlot(minecraft, slot);
            TerrainPhase.Scope ignoredTerrain = updateTerrain ? TerrainPhase.update() : TerrainPhase.drawOnly();
            ViewportPass.Scope ignoredViewport = ViewportPass.enter(viewport);
            WorldEngineStateScope ignoredEngines = WorldEngineStateScope.bind(minecraft, slot)
        ) {
            GameRendererSSAccessor renderer = (GameRendererSSAccessor)(Object)gameRenderer;
            var gameState = gameRenderer.gameRenderState();
            var optionsState = gameState.optionsRenderState;
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
            try (SlotRenderTargets.Scope ignoredTargets = SlotRenderTargets.enter(slot.id(), viewport, globalTarget)) {
                gameRenderer.renderLevel(deltaTracker);
                ignoredTargets.present();
            }
        }
    }

    private static boolean canRenderLevel(PlayerSlot slot) {
        return slot.renderState().level() != null
            && slot.gameplayState().player() != null
            && !slot.gameplayState().player().isRemoved()
            && slot.gameplayState().gameMode() != null
            && !LocalPlayers.INSTANCE.sessions().isJoining(slot.id());
    }

    private static void drawWaitingViewport(GameRenderer gameRenderer, PlayerSlot slot, ViewportArea viewport) {
        RenderTarget target = gameRenderer.mainRenderTarget();
        RenderPass.RenderArea area = ViewportPass.areaFor(viewport, target.width, target.height);
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
