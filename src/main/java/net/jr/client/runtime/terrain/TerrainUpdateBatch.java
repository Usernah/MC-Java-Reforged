package net.jr.client.runtime.terrain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.LocalClient;
import net.jr.client.runtime.client.LocalClientReadinessPolicy;
import net.jr.client.runtime.context.LocalClientExecution;
import net.jr.client.runtime.context.LocalClientScope;
import net.jr.client.runtime.render.experimental.ExperimentalRenderController;
import net.jr.client.runtime.render.experimental.ExperimentalRenderMetrics;
import net.jr.client.runtime.render.state.WorldEngineStateScope;
import net.jr.client.runtime.slot.LocalClientSlot;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.SectionUpdateTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.state.level.SectionUpdateRenderState;
import net.minecraft.core.SectionPos;
import net.neoforged.neoforge.client.ClientHooks;

public final class TerrainUpdateBatch implements AutoCloseable {
    private static final ThreadLocal<TerrainUpdateBatch> ACTIVE = new ThreadLocal<>();

    private final TerrainUpdateBatch previous;
    private final List<List<Request>> requests = new ArrayList<>(LocalClientSlotRegistry.MAX_SLOTS);
    private final Map<TerrainKey, Request> selected = new HashMap<>();
    private final boolean enabled = ExperimentalRenderController.enabled();
    private boolean collecting = true;

    private TerrainUpdateBatch() {
        this.previous = ACTIVE.get();
        for (int i = 0; i < LocalClientSlotRegistry.MAX_SLOTS; i++) {
            this.requests.add(new ArrayList<>());
        }
        ACTIVE.set(this);
    }

    public static TerrainUpdateBatch begin() {
        return new TerrainUpdateBatch();
    }

    public static boolean defer(
        ClientLevel level,
        SectionUpdateTracker tracker,
        SectionRenderDispatcher.RenderSection section,
        SectionUpdateTracker.SectionDirtyState dirty
    ) {
        TerrainUpdateBatch batch = ACTIVE.get();
        var client = LocalClientScope.currentClientOrNull();
        SharedTerrainStore store = TerrainStateStore.nullableTerrainStore();
        if (batch == null || !batch.enabled || !batch.collecting || client == null || client.level() != level || store == null) {
            return false;
        }
        LocalClientSlot slot = client.slot();
        long node = section.getSectionNode();
        TerrainGraphArea area = slot.renderState().terrain().viewArea();
        if (area == null || area.sectionAtNode(node) != section) {
            return false;
        }
        ClientLevel latestSource = store.latestUpdateSource(section);
        LocalClient source = snapshotSource(client, latestSource, node);
        if (source == null) {
            return true;
        }
        ClientLevel sourceLevel = source.level();
        TerrainKey key = new TerrainKey(level.dimension(), SectionPos.x(node), SectionPos.y(node), SectionPos.z(node));
        Request request = new Request(slot, level, source, sourceLevel, tracker, section, dirty, key, node, store, store.revision(section), dirty.isDirtyFromPlayer());
        batch.requests.get(slot.id()).add(request);
        Request current = batch.selected.get(key);
        if (current == null || !current.playerChanged && request.playerChanged) {
            batch.selected.put(key, request);
        }
        return true;
    }

    private static boolean canSnapshot(LocalClient client, long node) {
        ClientLevel level = client.level();
        SectionUpdateTracker tracker = client.slot().renderState().levelExtractionState().sectionUpdateTracker();
        if (level == null || tracker == null) {
            return false;
        }
        return level.getChunkSource().hasChunk(SectionPos.x(node), SectionPos.z(node))
            && level.getLightEngine().lightOnInColumn(SectionPos.getZeroNode(node))
            && tracker.hasAllNeighbors(level, node);
    }

    private static @Nullable LocalClient snapshotSource(
        LocalClient current,
        @Nullable ClientLevel latestSource,
        long node
    ) {
        if (latestSource != null) {
            for (LocalClient candidate : ClientRuntime.INSTANCE.clients().all()) {
                if (candidate.level() == latestSource
                    && LocalClientReadinessPolicy.worldReady(candidate)
                    && canSnapshot(candidate, node)) {
                    return candidate;
                }
            }
        }

        ClientLevel currentLevel = current.level();
        if (currentLevel == null) {
            return null;
        }
        if (LocalClientReadinessPolicy.worldReady(current) && canSnapshot(current, node)) {
            return current;
        }
        for (LocalClient candidate : ClientRuntime.INSTANCE.clients().all()) {
            ClientLevel candidateLevel = candidate.level();
            if (candidate != current
                && candidateLevel != null
                && candidateLevel.dimension().equals(currentLevel.dimension())
                && LocalClientReadinessPolicy.worldReady(candidate)
                && canSnapshot(candidate, node)) {
                return candidate;
            }
        }
        return null;
    }

    public void prepareSnapshots(Minecraft minecraft, List<LocalClientSlot> slots) {
        this.collecting = false;
        if (!this.enabled) {
            return;
        }
        Map<ClientLevel, RenderRegionCache> caches = new IdentityHashMap<>();
        for (LocalClientSlot slot : slots) {
            List<Request> slotRequests = this.requests.get(slot.id());
            if (slotRequests.isEmpty()) {
                continue;
            }
            long started = ExperimentalRenderMetrics.startTimer();
            try (
                LocalClientExecution.Scope ignoredClient = LocalClientExecution.enterForClient(minecraft, slot.id());
                WorldEngineStateScope ignoredEngines = WorldEngineStateScope.bind(minecraft, slot)
            ) {
                for (Request request : slotRequests) {
                    if (this.selected.get(request.key) != request || !request.isCurrent()) {
                        continue;
                    }
                    if (!TerrainCoordinator.shouldCoalescePendingBuild(slot, request.node, request.playerChanged)) {
                        try (
                            LocalClientExecution.Scope ignoredSource = LocalClientExecution.enterForClient(minecraft, request.source.slotId());
                            WorldEngineStateScope ignoredSourceEngines = WorldEngineStateScope.bind(minecraft, request.source.slot())
                        ) {
                            var additionalRenderers = ClientHooks.gatherAdditionalRenderers(request.section.getRenderOrigin(), request.sourceLevel);
                            if (!request.isCurrent()) {
                                continue;
                            }
                            var snapshot = request.store.snapshotVersion(request.section);
                            var region = caches.computeIfAbsent(request.sourceLevel, unused -> new RenderRegionCache())
                                .createRegion(request.sourceLevel, request.node);
                            if (!request.isCurrent()) {
                                continue;
                            }
                            ((TerrainSnapshotVersion)(Object)region).javaReforged$snapshotVersion(snapshot);
                            slot.renderState().levelRenderState().sectionUpdateRenderStates.add(
                                new SectionUpdateRenderState(request.node, request.playerChanged, region, additionalRenderers)
                            );
                        }
                    }
                    this.selected.remove(request.key);
                }
            } finally {
                ExperimentalRenderMetrics.recordExtraction(slot.id(), started);
            }
        }
        for (List<Request> slotRequests : this.requests) {
            for (Request request : slotRequests) {
                if (!this.selected.containsKey(request.key) && request.isCurrent()) {
                    request.dirty.setNotDirty();
                }
            }
        }
    }

    @Override
    public void close() {
        if (this.previous == null) {
            ACTIVE.remove();
        } else {
            ACTIVE.set(this.previous);
        }
        this.selected.clear();
        this.requests.forEach(List::clear);
    }

    private record Request(
        LocalClientSlot slot,
        ClientLevel level,
        LocalClient source,
        ClientLevel sourceLevel,
        SectionUpdateTracker tracker,
        SectionRenderDispatcher.RenderSection section,
        SectionUpdateTracker.SectionDirtyState dirty,
        TerrainKey key,
        long node,
        SharedTerrainStore store,
        long revision,
        boolean playerChanged
    ) {
        private boolean isCurrent() {
            TerrainGraphArea area = this.slot.renderState().terrain().viewArea();
            return TerrainStateStore.nullableTerrainStore() == this.store
                && this.slot.renderState().level() == this.level
                && ClientRuntime.INSTANCE.clients().clientOrNull(this.source.slotId()) == this.source
                && this.source.level() == this.sourceLevel
                && LocalClientReadinessPolicy.worldReady(this.source)
                && canSnapshot(this.source, this.node)
                && this.slot.renderState().levelExtractionState().sectionUpdateTracker() == this.tracker
                && this.tracker.getDirtyState(this.node) == this.dirty
                && this.dirty.isDirty()
                && this.dirty.isDirtyFromPlayer() == this.playerChanged
                && this.section.getSectionNode() == this.node
                && area != null && area.sectionAtNode(this.node) == this.section
                && this.store.revision(this.section) == this.revision;
        }
    }
}
