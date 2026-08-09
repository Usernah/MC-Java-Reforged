package net.alnv.javareforged.ClientRuntime.runtime;

import net.alnv.javareforged.mixin.SSM.LevelRendererSSAccessor;
import net.alnv.javareforged.mixin.SSM.ParticleEngineSSAccessor;
import net.alnv.javareforged.ClientRuntime.slot.PlayerSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;

/**
 * Scoped binding for the vanilla world/render engines that still store a single ClientLevel.
 *
 * <p>This is not a slot-0/vanilla fallback. The caller must already be inside the slot/owner
 * whose work is being executed. If the owner level is null, null is installed too, so bugs are
 * visible instead of silently repaired by the primary slot.</p>
 */
public final class WorldEngineStateScope implements AutoCloseable {
    private final Minecraft minecraft;
    private final ClientLevel previousLevel;
    private final ClientLevel previousParticleLevel;
    private final boolean bindEntityDispatchers;
    private boolean closed;

    private WorldEngineStateScope(Minecraft minecraft, boolean bindEntityDispatchers) {
        this.minecraft = minecraft;
        this.bindEntityDispatchers = bindEntityDispatchers;
        this.previousLevel = ((LevelRendererSSAccessor)minecraft.levelRenderer).splitTest$getLevel();
        this.previousParticleLevel = ((ParticleEngineSSAccessor)minecraft.particleEngine).splitTest$getLevel();
    }

    public static WorldEngineStateScope bind(Minecraft minecraft, PlayerSlot slot) {
        return bind(minecraft, slot.renderState().level());
    }

    public static WorldEngineStateScope bind(Minecraft minecraft, ClientLevel level) {
        WorldEngineStateScope scope = new WorldEngineStateScope(minecraft, true);
        scope.install(level);
        return scope;
    }

    public static WorldEngineStateScope bindTerrainOnly(Minecraft minecraft, PlayerSlot slot) {
        WorldEngineStateScope scope = new WorldEngineStateScope(minecraft, false);
        scope.install(slot.renderState().level());
        return scope;
    }

    private void install(ClientLevel level) {
        LevelRendererSSAccessor levelRenderer = (LevelRendererSSAccessor)this.minecraft.levelRenderer;
        levelRenderer.splitTest$setLevel(level);
        SectionRenderDispatcher dispatcher = levelRenderer.splitTest$getSectionRenderDispatcher();
        if (dispatcher != null) {
            dispatcher.setLevel(level);
        }
        if (this.bindEntityDispatchers) {
            this.minecraft.getEntityRenderDispatcher().setLevel(level);
            this.minecraft.getBlockEntityRenderDispatcher().setLevel(level);
        }
        ((ParticleEngineSSAccessor)this.minecraft.particleEngine).splitTest$setLevel(level);
        TerrainDebug.recordRawStateCheck("world.engines.scope", TerrainDebug.slotForLevel(level), this.minecraft);
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        this.install(this.previousLevel);
        ((ParticleEngineSSAccessor)this.minecraft.particleEngine).splitTest$setLevel(this.previousParticleLevel);
    }
}
