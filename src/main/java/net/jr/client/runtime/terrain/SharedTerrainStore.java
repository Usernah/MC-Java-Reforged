package net.jr.client.runtime.terrain;

import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.lang.ref.WeakReference;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

public final class SharedTerrainStore implements AutoCloseable {
    private static final int RETIRE_DELAY_FRAMES = 2;

    private final SectionRenderDispatcher dispatcher;
    private final Map<TerrainKey, Entry> sections = new ConcurrentHashMap<>();
    private final Map<SectionRenderDispatcher.RenderSection, TerrainKey> reverse =
        java.util.Collections.synchronizedMap(new IdentityHashMap<>());
    private final ConcurrentLinkedQueue<RetiredEntry> retired = new ConcurrentLinkedQueue<>();
    private final ArrayDeque<SectionRenderDispatcher.RenderSection> reusable = new ArrayDeque<>();
    private final AtomicInteger nextIndex = new AtomicInteger();
    private volatile long frame;
    private volatile boolean closed;

    public SharedTerrainStore(SectionRenderDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public SectionRenderDispatcher dispatcher() {
        return this.dispatcher;
    }

    public Diagnostics diagnostics() {
        return new Diagnostics(
            this.sections.size(),
            this.reverse.size(),
            this.retired.size(),
            this.reusable.size(),
            this.nextIndex.get()
        );
    }

    public void beginFrame() {
        this.ensureOpen();
        this.frame++;
        int queuedRetirements = this.retired.size();
        for (int i = 0; i < queuedRetirements; i++) {
            RetiredEntry retiredEntry = this.retired.poll();
            if (retiredEntry == null) {
                break;
            }
            if (retiredEntry.releaseFrame() > this.frame) {
                this.retired.offer(retiredEntry);
                continue;
            }
            Entry entry = retiredEntry.entry();
            if (
                retiredEntry.epoch() != entry.retirementEpoch()
                    || entry.references() != 0
                    || entry.tasks() != 0
                    || this.sections.get(retiredEntry.key()) != entry
            ) {
                continue;
            }
            this.sections.remove(retiredEntry.key(), entry);
            this.reverse.remove(entry.section());
            entry.section().reset();
            this.reusable.addLast(entry.section());
        }
    }

    public void updateReferences(int slotId, LocalTerrainView.Update update) {
        this.ensureOpen();
        int slotMask = slotMask(slotId);
        for (TerrainKey key : update.removed()) {
            Entry entry = this.sections.get(key);
            if (entry == null) {
                continue;
            }
            int remaining = entry.removeReference(slotMask);
            if (remaining == 0) {
                this.scheduleRetirement(key, entry);
            }
        }
        for (TerrainKey key : update.added()) {
            this.acquire(key, slotMask);
        }
    }

    public void invalidateCompiledGeometry() {
        this.ensureOpen();
        for (Entry entry : this.sections.values()) {
            entry.markDirty();
            entry.section().reset();
        }
        for (SectionRenderDispatcher.RenderSection section : this.reusable) {
            section.reset();
        }
    }

    public void releaseSlot(int slotId, LocalTerrainView view) {
        int slotMask = slotMask(slotId);
        view.forEachKey(key -> {
            Entry entry = this.sections.get(key);
            if (entry == null) {
                return;
            }
            int remaining = entry.removeReference(slotMask);
            if (remaining == 0) {
                this.scheduleRetirement(key, entry);
            }
        });
    }

    private Entry acquire(TerrainKey key, int slotMask) {
        Entry entry = this.sections.compute(key, (ignored, existing) -> {
            Entry result = existing;
            if (result == null) {
                SectionRenderDispatcher.RenderSection section = this.reusable.pollFirst();
                long sectionNode = SectionPos.asLong(key.sectionX(), key.sectionY(), key.sectionZ());
                if (section == null) {
                    section = this.dispatcher.new RenderSection(this.nextIndex.getAndIncrement(), sectionNode);
                } else {
                    section.setSectionNode(sectionNode);
                }
                result = new Entry(section);
                this.reverse.put(section, key);
            }
            result.invalidateRetirement();
            result.addReference(slotMask);
            return result;
        });
        return entry;
    }

    public void taskStarted(SectionRenderDispatcher.RenderSection section) {
        Entry entry = this.entry(section);
        if (entry != null) {
            entry.taskStarted();
            entry.invalidateRetirement();
        }
    }

    public void markDirty(ClientLevel level, long sectionNode) {
        Entry entry = this.sections.get(key(level, sectionNode));
        if (entry != null) {
            entry.markDirty(level);
        }
    }

    public @Nullable ClientLevel latestUpdateSource(SectionRenderDispatcher.RenderSection section) {
        Entry entry = this.entry(section);
        return entry == null ? null : entry.latestSource.get();
    }

    long revision(SectionRenderDispatcher.RenderSection section) {
        Entry entry = this.entry(section);
        return entry == null ? -1L : entry.revision();
    }

    public boolean needsRebuild(SectionRenderDispatcher.RenderSection section) {
        Entry entry = this.entry(section);
        return !this.closed && entry != null
            && (entry.publishedRevision != entry.revision()
                || section.getSectionMesh() == CompiledSectionMesh.UNCOMPILED);
    }

    public boolean hasPendingBuild(SectionRenderDispatcher.RenderSection section) {
        Entry entry = this.entry(section);
        BuildTicket ticket = entry == null ? null : entry.latestBuild;
        return ticket != null && !ticket.finished && ticket.isCurrent();
    }

    public @Nullable SnapshotVersion snapshotVersion(SectionRenderDispatcher.RenderSection section) {
        Entry entry = this.entry(section);
        return entry == null ? null : new SnapshotVersion(this, entry, entry.revision());
    }

    public @Nullable BuildTicket beginBuild(SectionRenderDispatcher.RenderSection section, @Nullable SnapshotVersion snapshot) {
        Entry entry = this.entry(section);
        if (entry == null || this.closed) {
            return null;
        }
        synchronized (entry) {
            long revision = snapshot == null ? entry.revision()
                : snapshot.store == this && snapshot.entry == entry ? snapshot.revision : -1;
            BuildTicket ticket = new BuildTicket(this, entry, revision);
            entry.latestBuild = ticket;
            return ticket;
        }
    }

    public record SnapshotVersion(SharedTerrainStore store, Entry entry, long revision) {}

    public static final class BuildTicket {
        private final SharedTerrainStore store;
        private final Entry entry;
        private final long revision;
        private volatile boolean meshOwned;
        private volatile boolean finished;
        private volatile boolean abandoned;

        private BuildTicket(SharedTerrainStore store, Entry entry, long revision) {
            this.store = store;
            this.entry = entry;
            this.revision = revision;
        }

        public boolean isCurrent() {
            return !this.abandoned && !this.store.closed && this.entry.references() != 0
                && this.entry.latestBuild == this && this.entry.revision() == this.revision
                && this.store.entry(this.entry.section()) == this.entry;
        }

        public void attachMesh() {
            this.meshOwned = true;
        }

        public void taskFinished() {
            if (!this.meshOwned) {
                this.abandon();
            }
        }

        public void abandon() {
            this.abandoned = true;
            this.finished = true;
        }

        public SectionMesh publish(SectionMesh candidate, Supplier<SectionMesh> swap) {
            synchronized (this.entry) {
                if (!this.isCurrent()) {
                    this.abandon();
                    return candidate;
                }
                SectionMesh previous = swap.get();
                this.entry.publishedRevision = this.revision;
                this.finished = true;
                return previous;
            }
        }
    }

    @Nullable AtomicInteger referenceMaskForTask(SectionRenderDispatcher.RenderSection section) {
        Entry entry = this.entry(section);
        return entry == null ? null : entry.references;
    }

    public void taskFinished(SectionRenderDispatcher.RenderSection section) {
        Entry entry = this.entry(section);
        if (entry == null) {
            return;
        }
        int tasks = entry.taskFinished();
        if (tasks == 0 && entry.references() == 0) {
            TerrainKey key = this.keyFor(section);
            if (key != null) {
                this.scheduleRetirement(key, entry);
            }
        }
    }

    public @Nullable SectionRenderDispatcher.RenderSection sectionForGraph(
        int slotId,
        LocalTerrainView view,
        BlockPos pos
    ) {
        return this.sectionForGraph(slotId, view, SectionPos.asLong(pos));
    }

    public @Nullable SectionRenderDispatcher.RenderSection sectionForGraph(
        int slotId,
        LocalTerrainView view,
        long sectionNode
    ) {
        ClientLevel level = view.level();
        if (level == null) {
            return null;
        }
        TerrainKey key = key(level, sectionNode);
        if (!view.contains(key)) {
            return null;
        }
        Entry entry = this.sections.get(key);
        return entry != null && entry.hasReference(slotMask(slotId)) ? entry.section() : null;
    }

    public @Nullable TerrainKey keyFor(SectionRenderDispatcher.RenderSection section) {
        return this.reverse.get(section);
    }

    private @Nullable Entry entry(SectionRenderDispatcher.RenderSection section) {
        TerrainKey key = this.keyFor(section);
        return key == null ? null : this.sections.get(key);
    }

    public boolean isReferencedBy(int slotId, LocalTerrainView view, SectionRenderDispatcher.RenderSection section) {
        TerrainKey key = this.keyFor(section);
        if (key == null || !view.contains(key)) {
            return false;
        }
        Entry entry = this.sections.get(key);
        return entry != null && entry.hasReference(slotMask(slotId));
    }

    private static TerrainKey key(ClientLevel level, long sectionNode) {
        return new TerrainKey(
            level.dimension(),
            SectionPos.x(sectionNode),
            SectionPos.y(sectionNode),
            SectionPos.z(sectionNode)
        );
    }

    private static int slotMask(int slotId) {
        if (slotId < 0 || slotId >= Integer.SIZE) {
            throw new IllegalArgumentException("Invalid local player slot " + slotId);
        }
        return 1 << slotId;
    }

    private void scheduleRetirement(TerrainKey key, Entry entry) {
        long epoch = entry.beginRetirement();
        this.retired.offer(new RetiredEntry(key, entry, this.frame + RETIRE_DELAY_FRAMES, epoch));
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        for (Entry entry : this.sections.values()) {
            entry.section().reset();
        }
        for (SectionRenderDispatcher.RenderSection section : this.reusable) {
            section.reset();
        }
        this.sections.clear();
        this.reverse.clear();
        this.retired.clear();
        this.reusable.clear();
    }

    public record Diagnostics(
        int liveSections,
        int reverseEntries,
        int queuedRetirements,
        int reusableSections,
        int allocatedSections
    ) {
        public static final Diagnostics EMPTY = new Diagnostics(0, 0, 0, 0, 0);
    }

    private void ensureOpen() {
        if (this.closed) {
            throw new IllegalStateException("Shared terrain store is closed");
        }
    }

    private static final class Entry {
        private final SectionRenderDispatcher.RenderSection section;
        private final AtomicInteger references = new AtomicInteger();
        private final AtomicInteger tasks = new AtomicInteger();
        private final AtomicLong retirementEpoch = new AtomicLong();
        private final AtomicLong revision = new AtomicLong();
        private volatile long publishedRevision = -1;
        private volatile BuildTicket latestBuild;
        private volatile WeakReference<ClientLevel> latestSource = new WeakReference<>(null);

        private Entry(SectionRenderDispatcher.RenderSection section) {
            this.section = section;
        }

        private SectionRenderDispatcher.RenderSection section() {
            return this.section;
        }

        private int references() {
            return this.references.get();
        }

        private int tasks() {
            return this.tasks.get();
        }

        private void taskStarted() {
            this.tasks.incrementAndGet();
        }

        private int taskFinished() {
            return this.tasks.updateAndGet(value -> Math.max(0, value - 1));
        }

        private long beginRetirement() {
            return this.retirementEpoch.incrementAndGet();
        }

        private void invalidateRetirement() {
            this.retirementEpoch.incrementAndGet();
        }

        private long retirementEpoch() {
            return this.retirementEpoch.get();
        }

        private synchronized void markDirty() {
            this.revision.incrementAndGet();
        }

        private synchronized void markDirty(ClientLevel source) {
            if (this.latestSource.get() != source) {
                this.latestSource = new WeakReference<>(source);
            }
            this.revision.incrementAndGet();
        }

        private long revision() {
            return this.revision.get();
        }

        private void addReference(int slotMask) {
            this.references.getAndUpdate(value -> value | slotMask);
        }

        private int removeReference(int slotMask) {
            return this.references.updateAndGet(value -> value & ~slotMask);
        }

        private boolean hasReference(int slotMask) {
            return (this.references.get() & slotMask) != 0;
        }
    }

    private record RetiredEntry(TerrainKey key, Entry entry, long releaseFrame, long epoch) {
    }
}
