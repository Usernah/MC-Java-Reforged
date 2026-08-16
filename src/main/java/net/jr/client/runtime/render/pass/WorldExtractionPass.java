package net.jr.client.runtime.render.pass;

import java.util.List;

import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.LocalClientAcces;
import net.jr.client.runtime.context.LocalClientExecution;
import net.jr.client.runtime.terrain.TerrainCoordinator;
import net.jr.client.runtime.render.state.WorldEngineStateScope;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.jr.mixin.runtime.GameRendererSSAccessor;
import net.jr.mixin.runtime.LightmapRenderStateExtractorSSAccessor;
import net.jr.mixin.runtime.MinecraftActionSSAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.extract.LevelExtractor;

/** Extracts one immutable 26.2 render-state tree per player through one shared extractor engine. */
public final class WorldExtractionPass {
    private WorldExtractionPass() {
    }

    public static void updateCameras(DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        for (LocalClientSlot slot : ClientRuntime.INSTANCE.slots().visibleSlots()) {
            if (!canExtract(slot)) {
                continue;
            }
            try (LocalClientExecution.Scope ignored = LocalClientExecution.enterForSlot(minecraft, slot)) {
                LocalClientAcces.camera().update(deltaTracker);
            }
        }
    }

    /** Resolves the block/entity under each local player's own camera. */
    public static void pickVisibleSlots(Minecraft minecraft, float partialTicks) {
        MinecraftActionSSAccessor actions = (MinecraftActionSSAccessor)(Object)minecraft;
        for (LocalClientSlot slot : ClientRuntime.INSTANCE.slots().visibleSlots()) {
            if (!canExtract(slot)) {
                continue;
            }
            try (LocalClientExecution.Scope ignored = LocalClientExecution.enterForSlot(minecraft, slot)) {
                actions.splitTest$pick(partialTicks);
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
        List<LocalClientSlot> extractionSlots = ClientRuntime.INSTANCE.slots().visibleSlots().stream()
            .filter(WorldExtractionPass::canExtract)
            .toList();
        TerrainCoordinator.beginExtractionFrame(extractionSlots);

        for (LocalClientSlot slot : extractionSlots) {
            try (
                    LocalClientExecution.Scope ignoredClient = LocalClientExecution.enterForSlot(minecraft, slot);
                    WorldEngineStateScope ignoredEngines = WorldEngineStateScope.bind(minecraft, slot)
            ) {
                Camera camera = LocalClientAcces.camera();
                ((LightmapRenderStateExtractorSSAccessor)(Object)lightmapExtractor).splitTest$setNeedsUpdate(true);
                lightmapExtractor.extract(slot.renderState().lightmapRenderState(), 1.0F);
                float cameraPartialTicks = camera.getCameraEntityPartialTicks(deltaTracker);
                renderer.splitTest$extractCamera(deltaTracker, worldPartialTicks, cameraPartialTicks);
                levelExtractor.extract(deltaTracker, camera, worldPartialTicks);
            }
        }
    }

    private static boolean canExtract(LocalClientSlot slot) {
        return slot.renderState().level() != null
            && slot.gameplayState().player() != null
            && !slot.gameplayState().player().isRemoved()
            && !ClientRuntime.INSTANCE.sessions().isJoining(slot.id());
    }
}
