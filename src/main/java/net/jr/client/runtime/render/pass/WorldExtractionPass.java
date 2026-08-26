package net.jr.client.runtime.render.pass;

import java.util.List;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.client.LocalClientReadinessPolicy;
import net.jr.client.runtime.context.LocalClientExecution;
import net.jr.client.runtime.terrain.TerrainCoordinator;
import net.jr.client.runtime.render.state.WorldEngineStateScope;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.jr.mixin.accessors.GameRendererAccessor;
import net.jr.mixin.accessors.LightmapRenderStateExtractorAccessor;
import net.jr.mixin.accessors.MinecraftAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.extract.LevelExtractor;

public final class WorldExtractionPass {
    private WorldExtractionPass() {
    }

    public static void updateCameras(DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        for (LocalClientSlot slot : ClientRuntime.INSTANCE.viewports().drawableSlots()) {
            if (!canExtract(slot)) {
                continue;
            }
            try (LocalClientExecution.Scope ignored = LocalClientExecution.enterForClient(minecraft, slot.id())) {
                ClientRuntime.INSTANCE.clients().client(slot.id()).camera().update(deltaTracker);
            }
        }
    }

    public static void pickVisibleSlots(Minecraft minecraft, float partialTicks) {
        MinecraftAccessor actions = (MinecraftAccessor)(Object)minecraft;
        for (LocalClientSlot slot : ClientRuntime.INSTANCE.viewports().drawableSlots()) {
            if (!canExtract(slot)) {
                continue;
            }
            try (LocalClientExecution.Scope ignored = LocalClientExecution.enterForClient(minecraft, slot.id())) {
                actions.javaReforged$pick(partialTicks);
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
        GameRendererAccessor renderer = (GameRendererAccessor)(Object)gameRenderer;
        LightmapRenderStateExtractor lightmapExtractor = renderer.javaReforged$getLightmapExtractor();
        List<LocalClientSlot> extractionSlots = ClientRuntime.INSTANCE.viewports().drawableSlots().stream()
            .filter(WorldExtractionPass::canExtract)
            .toList();
        TerrainCoordinator.beginExtractionFrame(extractionSlots);

        for (LocalClientSlot slot : extractionSlots) {
            try (
                    LocalClientExecution.Scope ignoredClient =
                            LocalClientExecution.enterForClient(
                                    minecraft,
                                    slot.id()
                            );
                    WorldEngineStateScope ignoredEngines =
                            WorldEngineStateScope.bind(minecraft, slot)
            ) {
                var optionsRenderState =
                        gameRenderer.gameRenderState().optionsRenderState;

                optionsRenderState.cameraType =
                        slot.optionsState().cameraType();

                optionsRenderState.fov =
                        slot.optionsState().fov().get();

                Camera camera =
                        ClientRuntime.INSTANCE
                                .clients()
                                .client(slot.id())
                                .camera();

                ((LightmapRenderStateExtractorAccessor)(Object)lightmapExtractor)
                        .javaReforged$setNeedsUpdate(true);

                lightmapExtractor.extract(
                        slot.renderState().lightmapRenderState(),
                        1.0F
                );

                float cameraPartialTicks =
                        camera.getCameraEntityPartialTicks(deltaTracker);

                renderer.javaReforged$extractCamera(
                        deltaTracker,
                        worldPartialTicks,
                        cameraPartialTicks
                );

                levelExtractor.extract(
                        deltaTracker,
                        camera,
                        worldPartialTicks
                );
            }
        }
    }

    private static boolean canExtract(LocalClientSlot slot) {
        LocalClient client = ClientRuntime.INSTANCE.clients().clientOrNull(slot.id());
        return client != null && LocalClientReadinessPolicy.worldReady(client);
    }
}
