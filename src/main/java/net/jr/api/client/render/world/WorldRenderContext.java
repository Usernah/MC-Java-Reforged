package net.jr.api.client.render.world;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

/**
 * All view-dependent inputs required to extract or render one local world.
 * Implementations are valid only for the duration of the current render pass.
 */
public interface WorldRenderContext {
    int slotId();

    LocalPlayer player();

    ClientLevel level();

    Entity cameraEntity();

    Camera camera();

    RenderViewport viewport();

    DeltaTracker deltaTracker();

    /**
     * Whether this view may advance work that belongs to the shared world renderer.
     */
    boolean updatesTerrain();
}
