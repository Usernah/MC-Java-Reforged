package net.jr.client.runtime.render.pass;

import java.util.List;

import net.jr.api.client.local.Client;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.ClientInstance;
import net.jr.client.runtime.client.ReadinessPolicy;
import net.jr.client.runtime.context.InstanceExecution;
import net.jr.client.runtime.render.state.WorldEngineStateScope;
import net.jr.client.runtime.render.world.WorldRenderBackend;
import net.jr.client.runtime.render.world.WorldRenderBackends;
import net.jr.client.runtime.render.world.WorldRenderContextAccess;
import net.jr.client.runtime.render.world.WorldRenderView;
import net.jr.client.runtime.render.world.WorldRenderViews;
import net.jr.client.runtime.slot.Slots;
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
        for (Slots slot : ClientRuntime.INSTANCE.viewports().drawableSlots()) {
            if (!canExtract(slot)) {
                continue;
            }
            try (InstanceExecution.Scope ignored = InstanceExecution.enterForClient(minecraft, slot.id())) {
                Client.current().camera().update(deltaTracker);
            }
        }
    }

    public static void pickVisibleSlots(Minecraft minecraft, float partialTicks) {
        MinecraftAccessor actions = (MinecraftAccessor)(Object)minecraft;
        for (Slots slot : ClientRuntime.INSTANCE.viewports().drawableSlots()) {
            if (!canExtract(slot)) {
                continue;
            }
            try (InstanceExecution.Scope ignored = InstanceExecution.enterForClient(minecraft, slot.id())) {
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
        List<WorldRenderView> views = WorldRenderViews.collect(
            deltaTracker,
            ReadinessPolicy::worldReady
        );
        WorldRenderBackend backend = WorldRenderBackends.active();
        backend.beginExtractionFrame(views);
        for (WorldRenderView view : views) {
            Slots slot = view.slot();
            try (
                    InstanceExecution.Scope ignoredClient =
                        InstanceExecution.enterForClient(minecraft, slot.id());
                    WorldRenderContextAccess.Scope ignoredContext =
                        WorldRenderContextAccess.enter(view);
                    WorldRenderBackend.Scope ignoredBackend =
                        backend.enterExtractionView(view);
                    WorldEngineStateScope ignoredEngines =
                        WorldEngineStateScope.bind(minecraft, slot)
            ) {
                    var optionsRenderState =
                        gameRenderer.gameRenderState().optionsRenderState;

                    optionsRenderState.cameraType =
                        slot.optionsState().cameraType();

                    optionsRenderState.fov =
                        slot.optionsState().fov().get();

                    Camera camera = view.camera();

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

    private static boolean canExtract(Slots slot) {
        ClientInstance client = ClientRuntime.INSTANCE.clients().clientOrNull(slot.id());
        return client != null && ReadinessPolicy.worldReady(client);
    }
}
