package net.jr.ClientRuntime.terrain;

import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

/**
 * The single owner of Minecraft's terrain RenderSections.
 *
 * <p>Every player owns a logical camera window, but overlapping windows resolve
 * to the same RenderSection instance and therefore to the same compiled mesh and
 * GPU allocations. Retired sections are delayed before reuse so an asynchronous
 * occlusion graph can finish reading the previous frame safely.</p>
 */
public final class GlobalTerrainStore implements AutoCloseable {
    private static final int RETIRE_DELAY_FRAMES = 2;

    private final SectionRenderDispatcher dispatcher;
    private final Map<TerrainKey, Entry> sections = new ConcurrentHashMap<>();
    private final Map<SectionRenderDispatcher.RenderSection, TerrainKey> reverse =
        java.util.Collections.synchronizedMap(new IdentityHashMap<>());
    private final ConcurrentLinkedQueue<RetiredEntry> retired = new ConcurrentLinkedQueue<>();
    private final ArrayDeque<SectionRenderDispatcher.RenderSection> reusable = new ArrayDeque<>();
    private final AtomicInteger nextIndex = new AtomicInteger();
    private volatile long frame;
    private boolean closed;

    public GlobalTerrainStore(SectionRenderDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public SectionRenderDispatcher dispatcher() {
        return this.dispatcher;
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

    public void updateReferences(int slotId, SlotTerrainView.Update update) {
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

    public void releaseSlot(int slotId, SlotTerrainView view) {
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
        SlotTerrainView view,
        BlockPos pos
    ) {
        return this.sectionForGraph(slotId, view, SectionPos.asLong(pos));
    }

    public @Nullable SectionRenderDispatcher.RenderSection sectionForGraph(
        int slotId,
        SlotTerrainView view,
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

    public boolean isReferencedBy(int slotId, SlotTerrainView view, SectionRenderDispatcher.RenderSection section) {
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
