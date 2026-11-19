package net.jr.api.client.render.world;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;


public interface WorldRenderContext {
    int slotId();

    LocalPlayer player();

    ClientLevel level();

    Entity cameraEntity();

    Camera camera();

    RenderViewport viewport();

    DeltaTracker deltaTracker();


    boolean updatesTerrain();
}
