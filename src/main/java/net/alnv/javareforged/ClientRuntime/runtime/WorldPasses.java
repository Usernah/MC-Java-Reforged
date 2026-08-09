package net.alnv.javareforged.ClientRuntime.runtime;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import net.alnv.javareforged.mixin.SSM.LevelRendererSSAccessor;
import net.alnv.javareforged.mixin.SSM.LightTextureSSAccessor;
import net.alnv.javareforged.ClientRuntime.slot.PlayerSlot;
import net.alnv.javareforged.ClientRuntime.viewport.ViewportArea;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import org.lwjgl.opengl.GL11;

public final class WorldPasses {
    private WorldPasses() {
    }

    public static void renderLevelForVisibleSlots(GameRenderer gameRenderer, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayers players = LocalPlayers.INSTANCE;
        TerrainDebug.beginFrame();
        try {
            TerrainCoordinator.prepareFrame();
            int terrainUpdateSlotId = selectTerrainUpdateSlot(players);
            for (PlayerSlot slot : players.slots().visibleSlots()) {
                ViewportArea viewport = slot.viewport();
                slot.renderState().setAspectRatio(viewport.aspectRatio());
                if (!canRenderLevel(slot)) {
                    TerrainDebug.recordInactiveSlot(slot);
                    drawWaitingViewport(slot, viewport);
                    continue;
                }
                renderSlot(minecraft, gameRenderer, deltaTracker, slot, viewport, slot.id() == terrainUpdateSlotId);
            }
            restoreAfterWorldLoop(minecraft, players);
        } finally {
            TerrainDebug.endFrame();
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
            TerrainPhase.Scope phase = updateTerrain ? TerrainPhase.update() : TerrainPhase.drawOnly()
        ) {
            BufferUploader.invalidate();
            ViewportPass.run(viewport, () -> {
                try (WorldEngineStateScope ignoredEngines = WorldEngineStateScope.bind(minecraft, slot)) {
                    TerrainDebug.recordRawStateCheck("world.render", slot.id(), minecraft);
                    renderViewportWorld(minecraft, gameRenderer, deltaTracker, viewport);
                }
            });
        }
    }


    private static void bindWorldEnginesToLevel(Minecraft minecraft, ClientLevel level) {
        ((LevelRendererSSAccessor)minecraft.levelRenderer).splitTest$setLevel(level);
        if (((LevelRendererSSAccessor)minecraft.levelRenderer).splitTest$getSectionRenderDispatcher() != null) {
            ((LevelRendererSSAccessor)minecraft.levelRenderer).splitTest$getSectionRenderDispatcher().setLevel(level);
        }
        TerrainDebug.recordRawStateCheck("world.engines.bind", TerrainDebug.slotForLevel(level), minecraft);
        minecraft.getEntityRenderDispatcher().setLevel(level);
        minecraft.getBlockEntityRenderDispatcher().setLevel(level);
    }

    private static void renderViewportWorld(Minecraft minecraft, GameRenderer gameRenderer, DeltaTracker deltaTracker, ViewportArea viewport) {
        TransparencyPass.prepare(minecraft.levelRenderer);
        minecraft.getMainRenderTarget().bindWrite(true);
        RenderSystem.viewport(viewport.glX(), viewport.glY(), viewport.glWidth(), viewport.glHeight());
        RenderSystem.enableScissor(viewport.glX(), viewport.glY(), viewport.glWidth(), viewport.glHeight());
        RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 1.0F);
        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        markLightTextureDirty(gameRenderer);
        gameRenderer.renderLevel(deltaTracker);
        RenderSystem.disableScissor();
    }

    private static void markLightTextureDirty(GameRenderer gameRenderer) {
        ((LightTextureSSAccessor)(Object)gameRenderer.lightTexture()).splitTest$setUpdateLightTexture(true);
    }

    private static void restoreAfterWorldLoop(Minecraft minecraft, LocalPlayers players) {
        ClientLevel level = primaryTerrainLevel(players);
        if (level == null) {
            throw new IllegalStateException("Primary player slot has no ClientLevel after world render loop");
        }
        bindWorldEnginesToLevel(minecraft, level);
        TransparencyPass.restorePrimary(minecraft.levelRenderer);
        minecraft.getMainRenderTarget().bindWrite(true);
        RenderSystem.viewport(0, 0, minecraft.getMainRenderTarget().viewWidth, minecraft.getMainRenderTarget().viewHeight);
        RenderSystem.disableScissor();
    }

    private static ClientLevel primaryTerrainLevel(LocalPlayers players) {
        return players.primarySlot().renderState().level();
    }

    private static int selectTerrainUpdateSlot(LocalPlayers players) {
        int fallbackSlotId = -1;
        for (PlayerSlot slot : players.slots().visibleSlots()) {
            if (!canRenderLevel(slot)) {
                continue;
            }
            if (fallbackSlotId < 0) {
                fallbackSlotId = slot.id();
            }
            if (canDriveTerrainUpdate(slot)) {
                return slot.id();
            }
        }
        return fallbackSlotId;
    }

    private static boolean canDriveTerrainUpdate(PlayerSlot slot) {
        return slot.gameplayState().player() != null
            && !slot.gameplayState().player().isRemoved()
            && !slot.gameplayState().player().isDeadOrDying();
    }

    private static boolean canRenderLevel(PlayerSlot slot) {
        return slot.renderState().level() != null
            && slot.gameplayState().player() != null
            && !slot.gameplayState().player().isRemoved()
            && slot.gameplayState().gameMode() != null
            && !LocalPlayers.INSTANCE.sessions().isJoining(slot.id());
    }

    private static void drawWaitingViewport(PlayerSlot slot, ViewportArea viewport) {
        float tint = 0.12F + (slot.id() * 0.08F);
        RenderSystem.viewport(viewport.glX(), viewport.glY(), viewport.glWidth(), viewport.glHeight());
        RenderSystem.enableScissor(viewport.glX(), viewport.glY(), viewport.glWidth(), viewport.glHeight());
        RenderSystem.clearColor(tint, 0.06F, 0.08F + (slot.id() * 0.1F), 1.0F);
        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.disableScissor();
    }

}
