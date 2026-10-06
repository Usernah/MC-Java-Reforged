package net.jr.client.runtime.render.world;

import java.util.Objects;
import net.jr.api.client.render.world.WorldRenderContext;
import net.jr.client.runtime.client.ClientInstance;
import net.jr.client.runtime.slot.Slots;
import net.jr.client.runtime.state.ClientRenderState;
import net.jr.client.runtime.viewport.ViewportArea;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

public record WorldRenderView(
    Slots slot,
    ClientInstance client,
    LocalPlayer player,
    ClientLevel level,
    Entity cameraEntity,
    Camera camera,
    ViewportArea viewport,
    DeltaTracker deltaTracker,
    boolean updatesTerrain
) implements WorldRenderContext {
    public WorldRenderView {
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(cameraEntity, "cameraEntity");
        Objects.requireNonNull(camera, "camera");
        Objects.requireNonNull(viewport, "viewport");
        Objects.requireNonNull(deltaTracker, "deltaTracker");
        if (slot.id() != client.slotId()) {
            throw new IllegalArgumentException("World view slot and local client do not match");
        }
        if (player.level() != level) {
            throw new IllegalArgumentException("World view player and level do not match");
        }
    }

    @Override
    public int slotId() {
        return this.slot.id();
    }

    public ClientRenderState renderState() {
        return this.slot.renderState();
    }
}
