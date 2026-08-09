package net.jr.ClientRuntime.runtime;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.jr.ClientRuntime.bridge.LevelRendererSSAccessor;
import net.jr.ClientRuntime.slot.PlayerSlot;
import net.jr.ClientRuntime.slot.PlayerSlots;
import net.jr.ClientRuntime.state.TerrainState;
import net.jr.ClientRuntime.terrain.GlobalTerrainStore;
import net.jr.ClientRuntime.terrain.TerrainGraphArea;
import net.jr.ClientRuntime.terrain.TerrainKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.state.level.SectionUpdateRenderState;
import net.minecraft.core.SectionPos;

/** Coordinates vanilla 26.2 terrain work without duplicating its render engine. */
public final class TerrainCoordinator {
    private TerrainCoordinator() {
    }

    public static ViewArea createViewArea(
        SectionRenderDispatcher dispatcher,
        ClientLevel level,
        int viewDistance,
        SectionOcclusionGraph graph
    ) {
        GlobalTerrainStore store = ensureStore(dispatcher);
        int slotId = activeSlotId();
        TerrainState terrain = LocalPlayers.INSTANCE.slots().slot(slotId).renderState().terrain();
        if (terrain.sectionOcclusionGraph() != graph) {
            throw new IllegalStateException("LevelRenderer terrain graph does not belong to active slot " + slotId);
        }
        return terrain.ensureViewArea(store, level, viewDistance, slotId);
    }

    public static void captureViewArea(@Nullable ViewArea viewArea) {
        TerrainState terrain = LevelRendererFields.terrain();
        if (viewArea == null) {
            terrain.setViewArea(null);
            return;
        }
        if (!(viewArea instanceof TerrainGraphArea terrainArea)) {
            throw new IllegalStateException("Split terrain received a non-shared ViewArea");
        }
        terrain.setViewArea(terrainArea);
    }

    public static void resetActiveSlotTerrain() {
        LevelRendererFields.terrain().releaseViewArea();
    }

    public static void prepareFrame() {
        GlobalTerrainStore store = LevelRendererFields.nullableTerrainStore();
        if (store != null) {
            store.beginFrame();
        }
    }

    /**
     * Runs Minecraft's own compileSections method for every extracted player.
     * The call is made only from slot 0's render pass; duplicated coordinates are
     * removed before scheduling so one shared RenderSection is never compiled twice.
     */
    public static void compileVisibleSlots(LevelRenderer levelRenderer) {
        if (!TerrainPhase.canUpdateTerrain()) {
            return;
        }
        LevelRendererSSAccessor renderer = (LevelRendererSSAccessor)levelRenderer;
        SectionRenderDispatcher dispatcher = renderer.splitTest$getSectionRenderDispatcher();
        if (dispatcher == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Map<TerrainKey, SectionUpdateRenderState> selectedUpdates = selectUniqueUpdates();
        var primaryCamera = LocalPlayers.INSTANCE.slots().slot(0).renderState().levelRenderState().cameraRenderState.pos;
        try {
            for (PlayerSlot slot : LocalPlayers.INSTANCE.slots().visibleSlots()) {
                if (!canRender(slot)) {
                    continue;
                }
                ClientLevel level = slot.renderState().level();
                var updates = slot.renderState().levelRenderState().sectionUpdateRenderStates;
                updates.removeIf(update -> selectedUpdates.get(key(level, update)) != update);
                if (updates.isEmpty() && slot.renderState().terrain().visibleSections().isEmpty()) {
                    continue;
                }

                try (
                    ClientBoundary.Scope ignoredClient = ClientBoundary.enterForSlot(minecraft, slot);
                    WorldEngineStateScope ignoredEngines = WorldEngineStateScope.bindTerrainOnly(minecraft, slot)
                ) {
                    var cameraState = slot.renderState().levelRenderState().cameraRenderState;
                    slot.renderState().terrain().setCameraPosition(cameraState.pos);
                    dispatcher.setCameraPosition(cameraState.pos);
                    renderer.splitTest$compileSections(cameraState);
                }
            }
        } finally {
            // Vanilla owns one priority queue; slot 0 remains its sole scheduling authority.
            dispatcher.setCameraPosition(primaryCamera);
        }
    }

    private static Map<TerrainKey, SectionUpdateRenderState> selectUniqueUpdates() {
        Map<TerrainKey, SectionUpdateRenderState> selected = new HashMap<>();
        for (PlayerSlot slot : LocalPlayers.INSTANCE.slots().visibleSlots()) {
            ClientLevel level = slot.renderState().level();
            if (level == null) {
                continue;
            }
            for (SectionUpdateRenderState update : slot.renderState().levelRenderState().sectionUpdateRenderStates) {
                TerrainKey key = key(level, update);
                SectionUpdateRenderState current = selected.get(key);
                if (current == null || !current.playerChanged() && update.playerChanged()) {
                    selected.put(key, update);
                }
            }
        }
        return selected;
    }

    public static void onSectionCompiled(SectionRenderDispatcher.RenderSection section) {
        for (int slotId = 0; slotId < PlayerSlots.MAX_SLOTS; slotId++) {
            TerrainState terrain = LocalPlayers.INSTANCE.slots().slot(slotId).renderState().terrain();
            if (terrain.contains(section)) {
                terrain.sectionOcclusionGraph().schedulePropagationFrom(section);
            }
        }
    }

    public static boolean isPrimaryTerrainPass() {
        Integer slotId = ActiveSlot.idOrNull();
        return slotId == null || slotId == 0;
    }

    private static GlobalTerrainStore ensureStore(SectionRenderDispatcher dispatcher) {
        GlobalTerrainStore current = LevelRendererFields.nullableTerrainStore();
        if (current != null && current.dispatcher() == dispatcher) {
            return current;
        }
        if (current != null) {
            releaseAllViews();
            current.close();
        }
        GlobalTerrainStore created = new GlobalTerrainStore(dispatcher);
        LevelRendererFields.setTerrainStore(created);
        return created;
    }

    public static void closeSharedTerrain() {
        GlobalTerrainStore current = LevelRendererFields.nullableTerrainStore();
        if (current == null) {
            return;
        }
        releaseAllViews();
        current.close();
        LevelRendererFields.setTerrainStore(null);
    }

    private static void releaseAllViews() {
        for (int slotId = 0; slotId < PlayerSlots.MAX_SLOTS; slotId++) {
            LocalPlayers.INSTANCE.slots().slot(slotId).renderState().terrain().releaseViewArea();
        }
    }

    private static TerrainKey key(ClientLevel level, SectionUpdateRenderState update) {
        long node = update.sectionNode();
        return new TerrainKey(level.dimension(), SectionPos.x(node), SectionPos.y(node), SectionPos.z(node));
    }

    private static int activeSlotId() {
        Integer active = ActiveSlot.idOrNull();
        return active != null ? active : Client.slotId();
    }

    private static boolean canRender(PlayerSlot slot) {
        return slot.drawable()
            && slot.renderState().level() != null
            && slot.gameplayState().player() != null
            && slot.gameplayState().gameMode() != null
            && !LocalPlayers.INSTANCE.sessions().isJoining(slot.id());
    }
}
