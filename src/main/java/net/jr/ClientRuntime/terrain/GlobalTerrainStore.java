package net.jr.ClientRuntime.terrain;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;
import net.jr.Java_reforged;
import net.jr.mixin.SSM.RenderSectionSSAccessor;
import net.jr.ClientRuntime.runtime.LocalPlayers;
import net.jr.ClientRuntime.slot.PlayerSlots;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;

public final class GlobalTerrainStore {
    private static final double BACKGROUND_PRIORITY = 1_000_000_000_000.0D;
    private static final long MISSING_OWNER_LOG_INTERVAL_NANOS = 1_000_000_000L;
    private static final boolean LOG_MISSING_OWNER = Boolean.getBoolean("split.terrainOwnerWaitLog");

    private final SectionRenderDispatcher dispatcher;
    private final LevelRenderer levelRenderer;
    private final Map<TerrainKey, Entry> entries = new ConcurrentHashMap<>();
    private final Map<SectionRenderDispatcher.RenderSection, Entry> sections = new ConcurrentHashMap<>();
    private final ArrayDeque<SectionRenderDispatcher.RenderSection> freeSections = new ArrayDeque<>();
    private final ArrayDeque<Integer> freeIndices = new ArrayDeque<>();
    private final ConcurrentLinkedQueue<SectionRenderDispatcher.RenderSection> visibleDirtyQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<SectionRenderDispatcher.RenderSection> backgroundDirtyQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<SectionRenderDispatcher.RenderSection> interactiveDirtyQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Entry> pendingSections = new ConcurrentLinkedQueue<>();
    private final Set<SectionRenderDispatcher.RenderSection> queuedVisibleSections = ConcurrentHashMap.newKeySet();
    private final Set<SectionRenderDispatcher.RenderSection> queuedBackgroundSections = ConcurrentHashMap.newKeySet();
    private final Set<SectionRenderDispatcher.RenderSection> queuedInteractiveSections = ConcurrentHashMap.newKeySet();
    private final Set<Entry> queuedPendingSections = ConcurrentHashMap.newKeySet();
    private final Set<Entry> retiringSections = ConcurrentHashMap.newKeySet();
    private final AtomicLong nextMissingOwnerLogNanos = new AtomicLong();
    private int nextIndex;
    private long frameId;
    private boolean closed;

    public GlobalTerrainStore(SectionRenderDispatcher dispatcher, LevelRenderer levelRenderer) {
        this.dispatcher = dispatcher;
        this.levelRenderer = levelRenderer;
    }

    public SectionRenderDispatcher dispatcher() {
        return this.dispatcher;
    }

    public LevelRenderer levelRenderer() {
        return this.levelRenderer;
    }

    public void beginFrame() {
        this.requireOpen();
        this.frameId++;
    }

    public void updateReferences(int slotId, ClientLevel level, SlotTerrainView.Update update) {
        this.requireOpen();
        this.releaseReferences(slotId, update.removed());
        this.addReferences(slotId, level, update.added());
    }

    public void addReferences(int slotId, ClientLevel level, Iterable<TerrainKey> keys) {
        this.requireOpen();
        for (TerrainKey key : keys) {
            Entry entry = this.entries.computeIfAbsent(key, Entry::new);
            SlotEntry slotEntry = entry.slot(slotId);
            slotEntry.level = level;
            this.refreshChunkAvailability(slotEntry);
            this.enqueuePending(entry);
        }
    }

    public void releaseReferences(int slotId, Iterable<TerrainKey> keys) {
        this.requireOpen();
        for (TerrainKey key : keys) {
            this.release(slotId, key);
        }
    }

    public MaterializationResult materializePending(int limit) {
        int materialized = 0;
        int skippedWithoutLoadedOwner = 0;
        int skippedStale = 0;
        for (int count = 0; count < limit; count++) {
            Entry entry = this.pendingSections.poll();
            if (entry == null) {
                return new MaterializationResult(limit, materialized, skippedWithoutLoadedOwner, skippedStale, this.pendingSections.size());
            }
            if (!this.queuedPendingSections.remove(entry)
                || !entry.hasReferences()
                || entry.retiring) {
                skippedStale++;
                continue;
            }
            int previousSectionCount = this.sections.size();
            this.ensureSection(entry);
            if (this.sections.size() != previousSectionCount) {
                materialized++;
            } else if (entry.section == null && !entry.retiring && entry.hasReferences() && this.selectOwner(entry) == null) {
                skippedWithoutLoadedOwner++;
            }
        }
        return new MaterializationResult(limit, materialized, skippedWithoutLoadedOwner, skippedStale, this.pendingSections.size());
    }

    public void releaseSlot(int slotId, SlotTerrainView view) {
        if (!view.belongsTo(this)) {
            view.clear();
            return;
        }
        for (TerrainKey key : new HashSet<>(view.sections())) {
            this.release(slotId, key);
        }
        view.clear();
    }

    public void collectVisible(
        int slotId,
        SlotTerrainView view,
        Iterable<SectionRenderDispatcher.RenderSection> candidates,
        ObjectArrayList<SectionRenderDispatcher.RenderSection> target
    ) {
        target.clear();
        this.appendVisible(slotId, view, candidates, target);
    }

    public void appendVisible(
        int slotId,
        SlotTerrainView view,
        Iterable<SectionRenderDispatcher.RenderSection> candidates,
        ObjectArrayList<SectionRenderDispatcher.RenderSection> target
    ) {
        for (SectionRenderDispatcher.RenderSection section : candidates) {
            Entry entry = this.sections.get(section);
            SlotEntry slotEntry = entry == null ? null : entry.slot(slotId);
            if (entry != null
                && entry.section == section
                && slotEntry.level != null
                && this.refreshChunkAvailability(slotEntry)
                && !entry.retiring
                && view.contains(entry.key)) {
                this.claimOwnerForSlot(slotEntry);
                slotEntry.lastVisibleFrame = this.frameId;
                this.promoteDirty(section);
                target.add(section);
            }
        }
    }

    public void retainVisible(int slotId, SlotTerrainView view, ObjectArrayList<SectionRenderDispatcher.RenderSection> target) {
        for (int index = target.size() - 1; index >= 0; index--) {
            SectionRenderDispatcher.RenderSection section = target.get(index);
            Entry entry = this.sections.get(section);
            SlotEntry slotEntry = entry == null ? null : entry.slot(slotId);
            if (entry != null
                && entry.section == section
                && slotEntry.level != null
                && this.refreshChunkAvailability(slotEntry)
                && !entry.retiring
                && view.contains(entry.key)) {
                this.claimOwnerForSlot(slotEntry);
                slotEntry.lastVisibleFrame = this.frameId;
                this.promoteDirty(section);
            } else {
                target.remove(index);
            }
        }
    }

    public void collectBootstrapVisible(
        int slotId,
        SlotTerrainView view,
        Frustum frustum,
        ObjectArrayList<SectionRenderDispatcher.RenderSection> target
    ) {
        target.clear();
        for (TerrainKey key : view.sections()) {
            Entry entry = this.entries.get(key);
            SlotEntry slotEntry = entry == null ? null : entry.slot(slotId);
            boolean slotChunkAvailable = slotEntry != null
                && slotEntry.level != null
                && this.refreshChunkAvailability(slotEntry);
            SectionRenderDispatcher.RenderSection section = entry == null ? null : entry.section;
            if (slotEntry != null
                && section != null
                && slotEntry.level != null
                && slotChunkAvailable
                && !entry.retiring
                && frustum.isVisible(section.getBoundingBox())) {
                this.claimOwnerForSlot(slotEntry);
                slotEntry.lastVisibleFrame = this.frameId;
                this.promoteDirty(section);
                target.add(section);
            }
        }
    }

    public void collectVisibleByKeys(
        int slotId,
        SlotTerrainView view,
        Frustum frustum,
        ObjectArrayList<SectionRenderDispatcher.RenderSection> target
    ) {
        target.clear();
        for (TerrainKey key : view.sections()) {
            Entry entry = this.entries.get(key);
            if (entry == null) {
                continue;
            }
            SlotEntry slotEntry = entry.slot(slotId);
            boolean slotChunkAvailable = slotEntry.level != null && this.refreshChunkAvailability(slotEntry);
            SectionRenderDispatcher.RenderSection section = entry.section;
            if (section != null
                && slotEntry.level != null
                && slotChunkAvailable
                && !entry.retiring
                && frustum.isVisible(section.getBoundingBox())) {
                this.claimOwnerForSlot(slotEntry);
                slotEntry.lastVisibleFrame = this.frameId;
                this.promoteDirty(section);
                target.add(section);
            }
        }
    }

    public boolean isActive(SectionRenderDispatcher.RenderSection section) {
        Entry entry = this.sections.get(section);
        return entry != null && entry.section == section && entry.hasReferences() && !entry.retiring;
    }

    @Nullable
    public SectionRenderDispatcher.RenderSection sectionAt(ClientLevel level, BlockPos pos) {
        TerrainKey key = new TerrainKey(
            level.dimension(),
            SectionPos.blockToSectionCoord(pos.getX()),
            SectionPos.blockToSectionCoord(pos.getY()),
            SectionPos.blockToSectionCoord(pos.getZ())
        );
        Entry entry = this.entries.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.slotForLevel(level) == null) {
            return null;
        }
        if (entry.section == null) {
            this.enqueuePending(entry);
            return null;
        }
        return entry.section;
    }

    @Nullable
    public SectionRenderDispatcher.RenderSection sectionForGraph(int slotId, SlotTerrainView view, BlockPos pos) {
        ClientLevel level = view.level();
        if (level == null) {
            return null;
        }
        TerrainKey key = new TerrainKey(
            level.dimension(),
            SectionPos.blockToSectionCoord(pos.getX()),
            SectionPos.blockToSectionCoord(pos.getY()),
            SectionPos.blockToSectionCoord(pos.getZ())
        );
        if (!view.contains(key)) {
            return null;
        }
        Entry entry = this.entries.get(key);
        if (entry == null) {
            return null;
        }
        SlotEntry slotEntry = entry.slot(slotId);
        if (slotEntry.level == null || !this.refreshChunkAvailability(slotEntry)) {
            return null;
        }
        if (entry.section == null) {
            this.enqueuePending(entry);
            return null;
        }
        this.claimOwnerForSlot(slotEntry);
        return !entry.retiring ? entry.section : null;
    }

    public TerrainKey keyFor(SectionRenderDispatcher.RenderSection section) {
        return this.requireEntry(section).key;
    }

    public int slotIdFor(SectionRenderDispatcher.RenderSection section) {
        Entry entry = this.requireEntry(section);
        this.updateOwner(entry);
        return entry.ownerSlotId;
    }

    public void onChunkLoadedForSlot(int slotId, ClientLevel level, net.minecraft.world.level.ChunkPos chunkPos) {
        for (int sectionY = level.getMinSectionY(); sectionY <= level.getMaxSectionY(); sectionY++) {
            Entry entry = this.entries.get(new TerrainKey(level.dimension(), chunkPos.x(), sectionY, chunkPos.z()));
            if (entry == null) {
                continue;
            }
            SlotEntry slotEntry = entry.slot(slotId);
            if (slotEntry.level != level) {
                continue;
            }
            slotEntry.chunkAvailable = true;
            if (entry.section != null && entry.ownerLevel != null) {
                this.markDirty(entry, false);
                this.enqueueDirty(entry.section, true);
            } else if (entry.section == null && !entry.retiring) {
                this.enqueuePending(entry);
            }
        }
    }

    public void setDirty(ClientLevel level, int sectionX, int sectionY, int sectionZ, boolean playerChanged) {
        Entry entry = this.entries.get(new TerrainKey(level.dimension(), sectionX, sectionY, sectionZ));
        if (entry == null) {
            return;
        }
        SlotEntry slotEntry = entry.slotForLevel(level);
        if (slotEntry == null) {
            return;
        }
        SectionRenderDispatcher.RenderSection section = entry.section;
        if (section == null) {
            this.enqueuePending(entry);
            return;
        }
        this.markDirty(entry, playerChanged);
        this.enqueueDirty(section, playerChanged || this.isRecentlyVisible(section));
    }

    public DirtyBatch drainDirtySections(int limit) {
        List<SectionRenderDispatcher.RenderSection> dirty = new ArrayList<>();
        boolean visibleWorkPending = false;
        SectionRenderDispatcher.RenderSection section;
        while (dirty.size() < limit && (section = this.pollDirty(this.interactiveDirtyQueue, this.queuedInteractiveSections)) != null) {
            dirty.add(section);
            visibleWorkPending = true;
        }
        while (dirty.size() < limit && (section = this.pollDirty(this.visibleDirtyQueue, this.queuedVisibleSections)) != null) {
            dirty.add(section);
            visibleWorkPending = true;
        }
        while (dirty.size() < limit && (section = this.pollDirty(this.backgroundDirtyQueue, this.queuedBackgroundSections)) != null) {
            dirty.add(section);
        }
        return new DirtyBatch(dirty, visibleWorkPending);
    }

    public List<SectionRenderDispatcher.RenderSection> dirtyReferencedSections() {
        List<SectionRenderDispatcher.RenderSection> dirty = new ArrayList<>();
        for (Entry entry : this.entries.values()) {
            SectionRenderDispatcher.RenderSection section = entry.section;
            if (section != null
                && entry.ownerLevel != null
                && !entry.retiring
                && entry.dirty) {
                dirty.add(section);
            } else if (section == null && entry.hasReferences() && !entry.retiring) {
                this.enqueuePending(entry);
            }
        }
        return dirty;
    }

    public int entryCount() {
        return this.entries.size();
    }

    public int referencedEntryCount() {
        int count = 0;
        for (Entry entry : this.entries.values()) {
            if (entry.hasReferences()) {
                count++;
            }
        }
        return count;
    }

    public int unmaterializedReferencedCount() {
        int count = 0;
        for (Entry entry : this.entries.values()) {
            if (entry.hasReferences() && entry.section == null) {
                count++;
            }
        }
        return count;
    }

    public int sectionCount() {
        return this.sections.size();
    }

    public int pendingCount() {
        return this.pendingSections.size();
    }

    public int ownerWaitCount() {
        int count = 0;
        for (Entry entry : this.entries.values()) {
            if (entry.hasReferences() && entry.section == null && this.selectOwner(entry) == null) {
                count++;
            }
        }
        return count;
    }

    public int dirtyQueueCount() {
        return this.queuedInteractiveSections.size()
            + this.queuedVisibleSections.size()
            + this.queuedBackgroundSections.size();
    }

    public int activeTaskCount() {
        int count = 0;
        for (Entry entry : this.entries.values()) {
            count += entry.activeTasks.get();
        }
        return count;
    }

    public void deferDirty(SectionRenderDispatcher.RenderSection section) {
        Entry entry = this.sections.get(section);
        if (this.isActive(section) && entry != null && entry.dirty) {
            this.enqueueDirty(section, entry.dirtyFromPlayer || this.isRecentlyVisible(section));
        }
    }

    public boolean deferUntilOwner(SectionRenderDispatcher.RenderSection section, String reason) {
        Entry entry = this.sections.get(section);
        if (entry == null || entry.section != section || entry.retiring || !entry.hasReferences()) {
            return false;
        }
        if (this.updateOwner(entry)) {
            return false;
        }
        this.markDirty(entry, false);
        this.enqueueDirty(section, this.isRecentlyVisible(section));
        this.logMissingOwner(entry, section, reason);
        return true;
    }

    public ClientLevel ownerFor(SectionRenderDispatcher.RenderSection section) {
        Entry entry = this.requireEntry(section);
        this.updateOwner(entry);
        if (entry.ownerLevel == null) {
            throw new IllegalStateException("RenderSection has no ClientLevel owner: " + section.getRenderOrigin());
        }
        return entry.ownerLevel;
    }

    @Nullable
    public ClientLevel ownerOrNull(SectionRenderDispatcher.RenderSection section) {
        Entry entry = this.sections.get(section);
        if (entry == null || entry.section != section || entry.retiring) {
            return null;
        }
        this.updateOwner(entry);
        return entry.ownerLevel;
    }

    public Vec3 cameraPositionFor(SectionRenderDispatcher.RenderSection section) {
        Entry entry = this.requireEntry(section);
        if (entry.retiring) {
            if (entry.retainedCameraPosition == null) {
                throw new IllegalStateException("Retiring RenderSection has no retained camera position: " + section.getRenderOrigin());
            }
            return entry.retainedCameraPosition;
        }
        Vec3 cameraPosition = this.cameraPositionOrNull(entry, section);
        if (cameraPosition == null) {
            throw new IllegalStateException("RenderSection has no requesting camera position: " + section.getRenderOrigin());
        }
        return cameraPosition;
    }

    public double compilationPriority(SectionRenderDispatcher.RenderSection section) {
        Entry entry = this.sections.get(section);
        if (entry == null || entry.section != section || entry.ownerLevel == null || entry.retiring) {
            return Double.MAX_VALUE;
        }
        Vec3 cameraPosition = this.ownerCameraPositionOrNull(entry);
        if (cameraPosition == null) {
            return Double.MAX_VALUE;
        }
        Vec3 center = Vec3.atCenterOf(section.getRenderOrigin());
        double dx = cameraPosition.x - center.x;
        double dz = cameraPosition.z - center.z;
        double verticalTieBreak = Math.abs(cameraPosition.y - center.y) / 1_000_000.0D;
        double visibilityPriority = entry.lastVisibleFrame() >= this.frameId - 1L ? 0.0D : BACKGROUND_PRIORITY;
        return visibilityPriority + dx * dx + dz * dz + verticalTieBreak;
    }

    public boolean isRecentlyVisible(SectionRenderDispatcher.RenderSection section) {
        return this.requireEntry(section).lastVisibleFrame() >= this.frameId - 1L;
    }

    public boolean hasActiveTasks(SectionRenderDispatcher.RenderSection section) {
        return this.requireEntry(section).activeTasks.get() > 0;
    }

    public boolean isDirty(SectionRenderDispatcher.RenderSection section) {
        return this.requireEntry(section).dirty;
    }

    public boolean isDirtyFromPlayer(SectionRenderDispatcher.RenderSection section) {
        return this.requireEntry(section).dirtyFromPlayer;
    }

    public void markCompileStarted(SectionRenderDispatcher.RenderSection section) {
        this.markNotDirty(this.requireEntry(section));
    }

    public boolean hasRequestingCamera(SectionRenderDispatcher.RenderSection section) {
        Entry entry = this.sections.get(section);
        return entry != null
            && entry.section == section
            && !entry.retiring
            && this.cameraPositionOrNull(entry, section) != null;
    }

    public void taskStarted(SectionRenderDispatcher.RenderSection section) {
        Entry entry = this.requireEntry(section);
        if (entry.retiring) {
            throw new IllegalStateException("Terrain task started while section is retiring: " + section.getRenderOrigin());
        }
        Vec3 cameraPosition = this.cameraPositionOrNull(entry, section);
        if (cameraPosition == null) {
            throw new IllegalStateException("Terrain task started before any requesting camera was captured: " + section.getRenderOrigin());
        }
        entry.retainedCameraPosition = cameraPosition;
        entry.activeTasks.incrementAndGet();
    }

    public void taskFinished(SectionRenderDispatcher.RenderSection section) {
        Entry entry = this.requireEntry(section);
        int remaining = entry.activeTasks.decrementAndGet();
        if (remaining < 0) {
            throw new IllegalStateException("Terrain section finished more tasks than it started: " + section.getRenderOrigin());
        }
        if (remaining == 0 && !entry.retiring && entry.dirty) {
            this.markDirty(entry, false);
            this.enqueueDirty(section, this.isRecentlyVisible(section));
        }
    }

    public void finishRetirements() {
        for (Entry entry : this.retiringSections) {
            if (entry.activeTasks.get() == 0) {
                this.reconcile(entry);
            }
        }
    }

    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        for (SectionRenderDispatcher.RenderSection section : this.sections.keySet()) {
            section.reset();
        }
        for (SectionRenderDispatcher.RenderSection section : this.freeSections) {
            section.reset();
        }
        this.entries.clear();
        this.sections.clear();
        this.freeSections.clear();
        this.freeIndices.clear();
        this.visibleDirtyQueue.clear();
        this.backgroundDirtyQueue.clear();
        this.interactiveDirtyQueue.clear();
        this.pendingSections.clear();
        this.queuedVisibleSections.clear();
        this.queuedBackgroundSections.clear();
        this.queuedInteractiveSections.clear();
        this.queuedPendingSections.clear();
        this.retiringSections.clear();
    }

    private void release(int slotId, TerrainKey key) {
        Entry entry = this.entries.get(key);
        if (entry == null) {
            throw new IllegalStateException("Player slot released an untracked terrain section " + key);
        }
        SlotEntry slotEntry = entry.slot(slotId);
        slotEntry.level = null;
        slotEntry.chunkAvailable = false;
        if (entry.activeTasks.get() > 0 && !entry.hasReferences()) {
            this.retire(entry);
        } else {
            this.reconcile(entry);
        }
    }

    private void ensureSection(Entry entry) {
        if (entry.section != null || entry.retiring || !entry.hasReferences()) {
            return;
        }
        if (!this.updateOwner(entry)) {
            return;
        }
        TerrainKey key = entry.key;
        entry.section = this.acquireSection(
            SectionPos.sectionToBlockCoord(key.sectionX()),
            SectionPos.sectionToBlockCoord(key.sectionY()),
            SectionPos.sectionToBlockCoord(key.sectionZ())
        );
        this.sections.put(entry.section, entry);
        if (!this.completeIfEmpty(entry)) {
            this.markDirty(entry, false);
            this.enqueueDirty(entry.section, false);
        }
    }

    private boolean completeIfEmpty(Entry entry) {
        ClientLevel owner = entry.ownerLevel;
        SectionRenderDispatcher.RenderSection section = entry.section;
        if (owner == null || section == null) {
            throw new IllegalStateException("Terrain section was created without an owner");
        }
        LevelChunk chunk = owner.getChunkSource().getChunk(entry.key.sectionX(), entry.key.sectionZ(), false);
        if (chunk == null) {
            return false;
        }
        int sectionIndex = owner.getSectionIndexFromSectionY(entry.key.sectionY());
        LevelChunkSection[] levelSections = chunk.getSections();
        if (sectionIndex < 0 || sectionIndex >= levelSections.length || !levelSections[sectionIndex].hasOnlyAir()) {
            return false;
        }
        RenderSectionSSAccessor accessor = (RenderSectionSSAccessor)(Object)section;
        accessor.splitTest$setSectionMesh(CompiledSectionMesh.EMPTY);
        this.markNotDirty(entry);
        return true;
    }

    private void retire(Entry entry) {
        if (entry.retiring || entry.section == null) {
            return;
        }
        entry.retiring = true;
        this.retiringSections.add(entry);
        this.queuedPendingSections.remove(entry);
        this.queuedInteractiveSections.remove(entry.section);
        this.queuedVisibleSections.remove(entry.section);
        this.queuedBackgroundSections.remove(entry.section);
        this.removeFromVisibleLists(entry.section);
        ((RenderSectionSSAccessor)(Object)entry.section).splitTest$cancelTasks();
    }

    private void reconcile(Entry entry) {
        if (entry.activeTasks.get() != 0 && !entry.hasReferences()) {
            this.retire(entry);
            return;
        }
        this.retiringSections.remove(entry);
        if (!entry.hasReferences()) {
            this.queuedPendingSections.remove(entry);
            this.releaseSection(entry);
            entry.retiring = false;
            entry.retainedCameraPosition = null;
            entry.ownerLevel = null;
            entry.ownerSlotId = -1;
            this.removeEntryIfDead(entry);
            return;
        }
        entry.retiring = false;
        this.updateOwner(entry);
        if (entry.section == null) {
            this.enqueuePending(entry);
        }
    }

    private void releaseSection(Entry entry) {
        SectionRenderDispatcher.RenderSection section = entry.section;
        if (section == null) {
            return;
        }
        this.queuedInteractiveSections.remove(section);
        this.queuedVisibleSections.remove(section);
        this.queuedBackgroundSections.remove(section);
        this.removeFromVisibleLists(section);
        this.sections.remove(section);
        this.freeSections.addLast(section);
        entry.section = null;
    }

    private void removeEntryIfDead(Entry entry) {
        if (!entry.hasReferences() && !entry.hasLiveSections()) {
            this.entries.remove(entry.key, entry);
        }
    }

    private void enqueueDirty(SectionRenderDispatcher.RenderSection section, boolean visible) {
        Entry entry = this.sections.get(section);
        if (!this.isActive(section) || entry == null || !entry.dirty) {
            return;
        }
        if (entry.dirtyFromPlayer) {
            if (this.queuedInteractiveSections.add(section)) {
                this.interactiveDirtyQueue.add(section);
            }
            return;
        }
        if (visible) {
            if (this.queuedVisibleSections.add(section)) {
                this.visibleDirtyQueue.add(section);
            }
        } else if (this.queuedBackgroundSections.add(section)) {
            this.backgroundDirtyQueue.add(section);
        }
    }

    private void promoteDirty(SectionRenderDispatcher.RenderSection section) {
        Entry entry = this.sections.get(section);
        if (this.isActive(section) && entry != null && entry.dirty) {
            this.enqueueDirty(section, true);
        }
    }

    private void enqueuePending(Entry entry) {
        if (entry.section == null && entry.hasReferences() && !entry.retiring && this.queuedPendingSections.add(entry)) {
            this.pendingSections.add(entry);
        }
    }

    private boolean refreshChunkAvailability(SlotEntry slotEntry) {
        slotEntry.chunkAvailable = slotEntry.hasLoadedChunk();
        return slotEntry.chunkAvailable;
    }

    private void claimOwnerForSlot(SlotEntry slotEntry) {
        if (!slotEntry.isAvailableOwner()) {
            return;
        }
        Entry entry = slotEntry.entry;
        entry.ownerSlotId = slotEntry.slotId;
        entry.ownerLevel = slotEntry.level;
    }

    private SectionRenderDispatcher.RenderSection acquireSection(int blockX, int blockY, int blockZ) {
        SectionRenderDispatcher.RenderSection section = this.freeSections.pollFirst();
        if (section != null) {
            section.setSectionNode(SectionPos.asLong(
                SectionPos.blockToSectionCoord(blockX),
                SectionPos.blockToSectionCoord(blockY),
                SectionPos.blockToSectionCoord(blockZ)
            ));
            return section;
        }
        int index = this.freeIndices.isEmpty() ? this.nextIndex++ : this.freeIndices.removeFirst();
        return this.dispatcher.new RenderSection(index, SectionPos.asLong(
            SectionPos.blockToSectionCoord(blockX),
            SectionPos.blockToSectionCoord(blockY),
            SectionPos.blockToSectionCoord(blockZ)
        ));
    }

    @Nullable
    private SectionRenderDispatcher.RenderSection pollDirty(
        ConcurrentLinkedQueue<SectionRenderDispatcher.RenderSection> queue,
        Set<SectionRenderDispatcher.RenderSection> queued
    ) {
        SectionRenderDispatcher.RenderSection section;
        while ((section = queue.poll()) != null) {
            if (!queued.remove(section) || !this.isActive(section)) {
                continue;
            }
            Entry entry = this.sections.get(section);
            if (entry != null && !entry.dirty
                && section.getSectionMesh() == CompiledSectionMesh.UNCOMPILED
                && !this.hasActiveTasks(section)) {
                this.markDirty(entry, false);
            }
            if (entry != null && entry.dirty) {
                return section;
            }
        }
        return null;
    }

    public record DirtyBatch(List<SectionRenderDispatcher.RenderSection> sections, boolean visible) {
    }

    public record MaterializationResult(
        int limit,
        int materialized,
        int skippedWithoutLoadedOwner,
        int skippedStale,
        int remainingPending
    ) {
    }

    private void removeFromVisibleLists(SectionRenderDispatcher.RenderSection section) {
        for (int slotId = 0; slotId < PlayerSlots.MAX_SLOTS; slotId++) {
            LocalPlayers.INSTANCE.slots().slot(slotId).renderState().terrain().visibleSections().remove(section);
        }
    }

    @Nullable
    private Vec3 ownerCameraPositionOrNull(Entry entry) {
        int ownerSlotId = entry.ownerSlotId;
        Vec3 cameraPosition = ownerSlotId >= 0
            ? LocalPlayers.INSTANCE.slots().slot(ownerSlotId).renderState().terrain().nullableCameraPosition()
            : null;
        if (cameraPosition != null) {
            return cameraPosition;
        }
        return entry.retainedCameraPosition;
    }

    @Nullable
    private Vec3 cameraPositionOrNull(Entry entry, SectionRenderDispatcher.RenderSection section) {
        this.updateOwner(entry);
        int ownerSlotId = entry.ownerSlotId;
        Vec3 cameraPosition = ownerSlotId >= 0
            ? LocalPlayers.INSTANCE.slots().slot(ownerSlotId).renderState().terrain().nullableCameraPosition()
            : null;
        if (cameraPosition != null) {
            return cameraPosition;
        }
        return entry.retainedCameraPosition;
    }

    @Nullable
    private SlotEntry selectOwner(Entry entry) {
        int ownerSlotId = entry.ownerSlotId;
        if (ownerSlotId >= 0 && ownerSlotId < entry.slots.length) {
            SlotEntry currentOwner = entry.slot(ownerSlotId);
            if (currentOwner.isAvailableOwner()) {
                return currentOwner;
            }
        }
        for (SlotEntry slotEntry : entry.slots) {
            if (slotEntry.isAvailableOwner()) {
                return slotEntry;
            }
        }
        return null;
    }

    private boolean updateOwner(Entry entry) {
        SlotEntry owner = this.selectOwner(entry);
        if (owner == null) {
            entry.ownerSlotId = -1;
            entry.ownerLevel = null;
            return false;
        }
        entry.ownerSlotId = owner.slotId;
        entry.ownerLevel = owner.level;
        return true;
    }

    private void logMissingOwner(Entry entry, SectionRenderDispatcher.RenderSection section, String reason) {
        if (!LOG_MISSING_OWNER) {
            return;
        }
        long now = System.nanoTime();
        long next = this.nextMissingOwnerLogNanos.get();
        if (now < next || !this.nextMissingOwnerLogNanos.compareAndSet(next, now + MISSING_OWNER_LOG_INTERVAL_NANOS)) {
            return;
        }
        Java_reforged.LOGGER.warn(
            "RenderSection sin ClientLevel owner; compile diferido. reason={} origin={} key={} refs={} activeTasks={} dirty={} retiring={}",
            reason,
            section.getRenderOrigin(),
            entry.key,
            entry.referenceMask(),
            entry.activeTasks.get(),
            entry.dirty,
            entry.retiring
        );
    }

    private Entry requireEntry(SectionRenderDispatcher.RenderSection section) {
        Entry entry = this.sections.get(section);
        if (entry == null) {
            throw new IllegalStateException("RenderSection is not active: " + section.getRenderOrigin());
        }
        return entry;
    }

    private void markDirty(Entry entry, boolean fromPlayer) {
        boolean wasDirty = entry.dirty;
        entry.dirty = true;
        entry.dirtyFromPlayer = fromPlayer || (wasDirty && entry.dirtyFromPlayer);
    }

    private void markNotDirty(Entry entry) {
        entry.dirty = false;
        entry.dirtyFromPlayer = false;
    }

    private void requireOpen() {
        if (this.closed) {
            throw new IllegalStateException("Global terrain store is closed");
        }
    }

    private static final class Entry {
        private final TerrainKey key;
        private final SlotEntry[] slots = new SlotEntry[PlayerSlots.MAX_SLOTS];
        private final AtomicInteger activeTasks = new AtomicInteger();
        @Nullable
        private volatile ClientLevel ownerLevel;
        @Nullable
        private volatile SectionRenderDispatcher.RenderSection section;
        @Nullable
        private volatile Vec3 retainedCameraPosition;
        private volatile int ownerSlotId = -1;
        private volatile boolean retiring;
        private volatile boolean dirty = true;
        private volatile boolean dirtyFromPlayer;

        private Entry(TerrainKey key) {
            this.key = key;
            for (int slotId = 0; slotId < this.slots.length; slotId++) {
                this.slots[slotId] = new SlotEntry(this, slotId);
            }
        }

        private SlotEntry slot(int slotId) {
            if (slotId < 0 || slotId >= this.slots.length) {
                throw new IllegalArgumentException("Invalid terrain slot " + slotId);
            }
            return this.slots[slotId];
        }

        @Nullable
        private SlotEntry slotForLevel(ClientLevel level) {
            for (SlotEntry slotEntry : this.slots) {
                if (slotEntry.level == level) {
                    return slotEntry;
                }
            }
            return null;
        }

        private boolean hasReferences() {
            for (SlotEntry slotEntry : this.slots) {
                if (slotEntry.level != null) {
                    return true;
                }
            }
            return false;
        }

        private boolean hasLiveSections() {
            return this.section != null || this.retiring;
        }

        private int referenceMask() {
            int mask = 0;
            for (SlotEntry slotEntry : this.slots) {
                if (slotEntry.level != null) {
                    mask |= 1 << slotEntry.slotId;
                }
            }
            return mask;
        }

        private int referenceCount() {
            int count = 0;
            for (SlotEntry slotEntry : this.slots) {
                if (slotEntry.level != null) {
                    count++;
                }
            }
            return count;
        }

        private long lastVisibleFrame() {
            long lastVisibleFrame = Long.MIN_VALUE;
            for (SlotEntry slotEntry : this.slots) {
                lastVisibleFrame = Math.max(lastVisibleFrame, slotEntry.lastVisibleFrame);
            }
            return lastVisibleFrame;
        }
    }

    private static final class SlotEntry {
        private final Entry entry;
        private final int slotId;
        @Nullable
        private volatile ClientLevel level;
        private volatile boolean chunkAvailable;
        private long lastVisibleFrame = Long.MIN_VALUE;

        private SlotEntry(Entry entry, int slotId) {
            this.entry = entry;
            this.slotId = slotId;
        }

        private boolean hasLoadedChunk() {
            ClientLevel owner = this.level;
            return owner != null && owner.getChunk(this.entry.key.sectionX(), this.entry.key.sectionZ(), ChunkStatus.FULL, false) != null;
        }

        private boolean isAvailableOwner() {
            this.chunkAvailable = this.hasLoadedChunk();
            return this.chunkAvailable;
        }
    }
}
