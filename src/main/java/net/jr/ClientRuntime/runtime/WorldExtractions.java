package net.jr.ClientRuntime.runtime;

import net.jr.ClientRuntime.slot.PlayerSlot;
import net.jr.mixin.SSM.GameRendererSSAccessor;
import net.jr.mixin.SSM.LightmapRenderStateExtractorSSAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.extract.LevelExtractor;

/** Extracts one immutable 26.2 render-state tree per player through one shared extractor engine. */
public final class WorldExtractions {
    private WorldExtractions() {
    }

    public static void updateCameras(DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        for (PlayerSlot slot : LocalPlayers.INSTANCE.slots().visibleSlots()) {
            if (!canExtract(slot)) {
                continue;
            }
            try (ClientBoundary.Scope ignored = ClientBoundary.enterForSlot(minecraft, slot)) {
                Client.camera().update(deltaTracker);
            }
        }
    }

    public static void extractVisibleSlots(
        GameRenderer gameRenderer,
        LevelExtractor levelExtractor,
        DeltaTracker deltaTracker,
        float worldPartialTicks
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        GameRendererSSAccessor renderer = (GameRendererSSAccessor)(Object)gameRenderer;
        LightmapRenderStateExtractor lightmapExtractor = renderer.splitTest$getLightmapExtractor();

        for (PlayerSlot slot : LocalPlayers.INSTANCE.slots().visibleSlots()) {
            if (!canExtract(slot)) {
                continue;
            }

            try (
                ClientBoundary.Scope ignoredClient = ClientBoundary.enterForSlot(minecraft, slot);
                WorldEngineStateScope ignoredEngines = WorldEngineStateScope.bind(minecraft, slot)
            ) {
                Camera camera = Client.camera();
                ((LightmapRenderStateExtractorSSAccessor)(Object)lightmapExtractor).splitTest$setNeedsUpdate(true);
                lightmapExtractor.extract(slot.renderState().lightmapRenderState(), 1.0F);
                float cameraPartialTicks = camera.getCameraEntityPartialTicks(deltaTracker);
                renderer.splitTest$extractCamera(deltaTracker, worldPartialTicks, cameraPartialTicks);
                levelExtractor.extract(deltaTracker, camera, worldPartialTicks);
            }
        }
    }

    private static boolean canExtract(PlayerSlot slot) {
        return slot.renderState().level() != null
            && slot.gameplayState().player() != null
            && !slot.gameplayState().player().isRemoved()
            && !LocalPlayers.INSTANCE.sessions().isJoining(slot.id());
    }
}
