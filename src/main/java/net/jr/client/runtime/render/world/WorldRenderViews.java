package net.jr.client.runtime.render.world;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.ClientInstance;
import net.jr.client.runtime.slot.Slots;
import net.jr.client.runtime.viewport.ViewportArea;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.entity.Entity;

public final class WorldRenderViews {
    private WorldRenderViews() {
    }

    public static List<WorldRenderView> collect(
        DeltaTracker deltaTracker,
        Predicate<ClientInstance> readiness
    ) {
        ClientRuntime runtime = ClientRuntime.INSTANCE;
        int terrainDriverSlotId = selectTerrainDriver(runtime, readiness);
        List<WorldRenderView> views = new ArrayList<>();
        for (Slots slot : runtime.viewports().drawableSlots()) {
            ClientInstance client = runtime.clients().clientOrNull(slot.id());
            if (client == null || !readiness.test(client)) {
                continue;
            }

            ViewportArea viewport = runtime.viewports().viewport(slot.id());
            slot.renderState().setAspectRatio(viewport.aspectRatio());
            Entity cameraEntity = client.cameraEntity();
            if (cameraEntity == null) {
                cameraEntity = client.player();
            }
            views.add(new WorldRenderView(
                slot,
                client,
                client.player(),
                client.level(),
                cameraEntity,
                client.camera(),
                viewport,
                deltaTracker,
                slot.id() == terrainDriverSlotId
            ));
        }
        return List.copyOf(views);
    }

    private static int selectTerrainDriver(
        ClientRuntime runtime,
        Predicate<ClientInstance> readiness
    ) {
        int readyFallbackSlotId = -1;
        for (Slots slot : runtime.viewports().drawableSlots()) {
            ClientInstance client = runtime.clients().clientOrNull(slot.id());
            if (client == null || !readiness.test(client)) {
                continue;
            }
            if (readyFallbackSlotId < 0) {
                readyFallbackSlotId = slot.id();
            }
            if (client.player().isAlive()) {
                return slot.id();
            }
        }
        return readyFallbackSlotId;
    }
}
