package net.jr.client.runtime.render.experimental;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.DeviceInfo;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import javax.annotation.Nullable;
import net.jr.client.render.pipeline.ModRenderPipelines;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.slot.ClientSlots;
import net.jr.mixin.accessors.LevelRendererAccessor;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Util;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;


public final class TerrainMultiDraw {
    private static final int INSTANCE_STRIDE = 16;
    private static final int INDIRECT_COMMAND_STRIDE = 20;
    private static final int MAX_SECTIONS = 32768;
    private static final int MAX_COMMANDS = MAX_SECTIONS * 2;
    private static final int MAX_DRAWS_PER_SUBMIT = 128;
    private static final GpuBufferSlice[] NO_SECTION_UNIFORMS = new GpuBufferSlice[0];
    private static final int TERRAIN_BATCH_UBO_SIZE = new Std140SizeCalculator().putMat4f().putIVec2().get();
    private static final int BUFFER_USAGE_CPU_VERTEX = GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_HINT_CLIENT_STORAGE | GpuBuffer.USAGE_VERTEX;
    private static final int BUFFER_USAGE_CPU_INDIRECT = GpuBuffer.USAGE_MAP_WRITE
        | GpuBuffer.USAGE_HINT_CLIENT_STORAGE
        | GpuBuffer.USAGE_INDIRECT_PARAMETERS;
    private static final int BUFFER_USAGE_CPU_UNIFORM = GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_UNIFORM;

    private static final SlotBuffers[] SLOT_BUFFERS = new SlotBuffers[ClientSlots.MAX_SLOTS];
    private static final SlotCache[] SLOT_CACHES = new SlotCache[ClientSlots.MAX_SLOTS];
    private static final long[] SLOT_INVALIDATION_GENERATIONS = new long[ClientSlots.MAX_SLOTS];
    private static final Map<ChunkSectionsToRender, Prepared> PREPARED = new IdentityHashMap<>();

    private static int frameLogicalDraws;
    private static int frameSubmits;
    private static int frameBatches;
    private static int frameFallbacks;
    private static int frameCacheHits;
    private static int frameCacheRebuilds;
    private static String status = "waiting";

    private TerrainMultiDraw() {
    }

    public static void beginFrame() {
        frameLogicalDraws = 0;
        frameSubmits = 0;
        frameBatches = 0;
        frameFallbacks = 0;
        frameCacheHits = 0;
        frameCacheRebuilds = 0;
        if (!PREPARED.isEmpty()) {
            var orphaned = Collections.newSetFromMap(new IdentityHashMap<SlotBuffers, Boolean>());
            for (Prepared prepared : PREPARED.values()) {
                if (orphaned.add(prepared.buffers)) {
                    prepared.buffers.terrainBatch.rotate();
                    prepared.buffers.sectionInstances.rotate();
                    prepared.buffers.indirectCommands.rotate();
                }
            }
            PREPARED.clear();
            frameFallbacks += orphaned.size();
        }
        if (!ExperimentalRenderController.enabled()) {
            status = "off";
            closeBuffers();
        }
    }

    public static @Nullable ChunkSectionsToRender prepare(LevelRenderer levelRenderer, Matrix4fc modelViewMatrix) {
        if (!ExperimentalRenderController.enabled()) {
            return null;
        }
        if (SharedConstants.DEBUG_HOTKEYS && Minecraft.getInstance().wireframe) {
            status = "wireframe fallback";
            frameFallbacks++;
            return null;
        }

        DeviceInfo deviceInfo = RenderSystem.getDevice().getDeviceInfo();
        if (!supported(deviceInfo)) {
            status = "unsupported " + deviceInfo.backendName();
            frameFallbacks++;
            return null;
        }

        LevelRendererAccessor renderer = (LevelRendererAccessor)(Object)levelRenderer;
        SectionRenderDispatcher dispatcher = renderer.javaReforged$getSectionRenderDispatcher();
        if (dispatcher == null) {
            return null;
        }

        int visibleCount = renderer.javaReforged$getVisibleSections().size();
        if (visibleCount > MAX_SECTIONS || visibleCount * 2 > MAX_COMMANDS) {
            status = "capacity fallback";
            frameFallbacks++;
            return null;
        }

        int slotId = SlotScope.requireId();
        SlotBuffers buffers = buffers(slotId);
        GpuTextureView blockAtlas = Minecraft.getInstance()
            .getTextureManager()
            .getTexture(TextureAtlas.LOCATION_BLOCKS)
            .getTextureView();
        int textureAtlasWidth = blockAtlas.getWidth(0);
        int textureAtlasHeight = blockAtlas.getHeight(0);
        SlotCache cache = cache(slotId);
        long invalidationGeneration = SLOT_INVALIDATION_GENERATIONS[slotId];
        Topology topology = cache.topology;
        if (topology == null
            || topology.invalidationGeneration != invalidationGeneration
            || !topology.matches(dispatcher, renderer.javaReforged$getVisibleSections())) {
            topology = buildTopology(topology, dispatcher, renderer.javaReforged$getVisibleSections(), invalidationGeneration);
            cache.topology = topology;
            frameCacheRebuilds++;
        } else {
            frameCacheHits++;
        }

        long now = Util.getMillis();
        Matrix4f frameModelView = cache.frameModelView.set(modelViewMatrix);
        DynamicUniforms.ChunkSectionInfo[] sectionInfos = cache.sectionInfos(topology.translucentSectionCount);
        float[] visibilities = cache.visibilities(topology.sectionCount);
        for (int index = 0; index < topology.sectionCount; index++) {
            SectionEntry entry = topology.sections[index];
            float visibility = entry.section.getVisibility(now);
            visibilities[index] = visibility;
        }


        for (int index = 0; index < topology.translucentSectionCount; index++) {
            int sectionIndex = topology.translucentSections[index];
            SectionEntry entry = topology.sections[sectionIndex];
            sectionInfos[index] = new DynamicUniforms.ChunkSectionInfo(
                frameModelView,
                entry.x,
                entry.y,
                entry.z,
                visibilities[sectionIndex],
                textureAtlasWidth,
                textureAtlasHeight
            );
        }

        GpuBufferSlice[] chunkSectionInfos = sectionInfos.length == 0
            ? NO_SECTION_UNIFORMS
            : RenderSystem.getDynamicUniforms().writeChunkSections(sectionInfos);
        ChunkSectionsToRender result = new ChunkSectionsToRender(
            blockAtlas,
            topology.vanillaGroups,
            topology.largestIndexCount,
            chunkSectionInfos
        );
        Prepared prepared = upload(
            buffers,
            modelViewMatrix,
            textureAtlasWidth,
            textureAtlasHeight,
            topology,
            visibilities
        );
        PREPARED.put(result, prepared);
        status = deviceInfo.backendName();
        return result;
    }

    public static boolean render(ChunkSectionsToRender sections, ChunkSectionLayerGroup group, GpuSampler sampler) {
        if (group != ChunkSectionLayerGroup.OPAQUE) {
            return false;
        }

        Prepared prepared = PREPARED.get(sections);
        if (prepared == null) {
            return false;
        }
        RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        GpuBuffer defaultIndexBuffer = prepared.maxIndicesRequired == 0 ? null : autoIndices.getBuffer(prepared.maxIndicesRequired);
        IndexType defaultIndexType = prepared.maxIndicesRequired == 0 ? null : autoIndices.type();
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget renderTarget = group.outputTarget();

        try (RenderPass renderPass = RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(
                () -> "JR terrain multi-draw opaque",
                renderTarget.getColorTextureView(),
                Optional.empty(),
                renderTarget.getDepthTextureView(),
                OptionalDouble.empty()
            )) {
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.bindTexture("Sampler0", sections.textureView(), sampler);
            renderPass.bindTexture("Sampler2", minecraft.gameRenderer.lightmap(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            renderPass.setUniform("TerrainBatch", prepared.terrainBatch);
            renderPass.setVertexBuffer(1, prepared.sectionInstances);

            for (ChunkSectionLayer layer : ChunkSectionLayerGroup.OPAQUE.layers()) {
                RenderPipeline pipeline = layer == ChunkSectionLayer.SOLID
                    ? ModRenderPipelines.SOLID_TERRAIN_MULTIDRAW
                    : ModRenderPipelines.CUTOUT_TERRAIN_MULTIDRAW;
                renderPass.setPipeline(pipeline);
                for (Batch batch : prepared.batches.get(layer)) {
                    GpuBuffer indexBuffer = batch.indexBuffer == null ? defaultIndexBuffer : batch.indexBuffer;
                    IndexType indexType = batch.indexType == null ? defaultIndexType : batch.indexType;
                    if (indexBuffer == null || indexType == null) {
                        continue;
                    }
                    renderPass.setVertexBuffer(0, batch.vertexBuffer.slice());
                    renderPass.setIndexBuffer(indexBuffer, indexType);
                    renderPass.drawIndexedIndirect(prepared.indirectCommands.slice(batch.commandOffset, (long)batch.drawCount * INDIRECT_COMMAND_STRIDE), batch.drawCount);
                    recordBatch(batch.drawCount);
                }
            }
        } finally {
            finish(sections, prepared);
        }
        return true;
    }

    public static void finishVanilla(ChunkSectionsToRender sections, ChunkSectionLayerGroup group) {
        if (group != ChunkSectionLayerGroup.OPAQUE) {
            return;
        }
        Prepared prepared = PREPARED.get(sections);
        if (prepared != null) {
            finish(sections, prepared);
        }
    }

    public static int preparedLogicalDrawCount(ChunkSectionsToRender sections) {
        Prepared prepared = PREPARED.get(sections);
        return prepared == null ? -1 : prepared.logicalDrawCount;
    }

    public static Snapshot snapshot() {
        return new Snapshot(
            status,
            frameLogicalDraws,
            frameSubmits,
            frameBatches,
            frameFallbacks,
            frameCacheHits,
            frameCacheRebuilds
        );
    }

    public static void invalidateSlot(int slotId) {
        if (slotId >= 0 && slotId < SLOT_INVALIDATION_GENERATIONS.length) {
            SLOT_INVALIDATION_GENERATIONS[slotId]++;
        }
    }

    public static void invalidateSection(SectionRenderDispatcher.RenderSection section) {
        for (int slotId = 0; slotId < SLOT_CACHES.length; slotId++) {
            SlotCache cache = SLOT_CACHES[slotId];
            if (cache != null && cache.topology != null && cache.topology.containsVisible(section)) {
                SLOT_INVALIDATION_GENERATIONS[slotId]++;
            }
        }
    }

    public static void close() {
        PREPARED.clear();
        closeBuffers();
        status = "closed";
    }

    private static Topology buildTopology(
        @Nullable Topology reusable,
        SectionRenderDispatcher dispatcher,
        List<SectionRenderDispatcher.RenderSection> visibleSections,
        long invalidationGeneration
    ) {
        Topology topology = reusable == null ? new Topology() : reusable;
        topology.beginRebuild(dispatcher, invalidationGeneration, visibleSections.size());
        int largestIndexCount = 0;
        dispatcher.lock();
        try {
            for (int visibleIndex = 0; visibleIndex < visibleSections.size(); visibleIndex++) {
                SectionRenderDispatcher.RenderSection section = visibleSections.get(visibleIndex);
                SectionMesh sectionMesh = section.getSectionMesh();
                topology.setVisible(visibleIndex, section, section.getSectionNode(), sectionMesh);
                BlockPos renderOffset = section.getRenderOrigin();
                int sectionIndex = -1;

                for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
                    SectionMesh.SectionDraw draw = sectionMesh.getSectionDraw(layer);
                    SectionRenderDispatcher.RenderSectionBufferSlice slice = dispatcher.getRenderSectionSlice(sectionMesh, layer);
                    if (slice == null || draw == null || draw.hasCustomIndexBuffer() && slice.indexBuffer() == null) {
                        continue;
                    }
                    topology.logicalDrawCount++;

                    if (sectionIndex == -1) {
                        sectionIndex = topology.addSection(
                            section,
                            renderOffset.getX(),
                            renderOffset.getY(),
                            renderOffset.getZ()
                        );
                    }

                    VertexFormat vertexFormat = layer.pipeline().getVertexFormatBinding(0);
                    GpuBuffer vertexBuffer = slice.vertexBuffer();
                    int combinedHash = 173;
                    if (layer != ChunkSectionLayer.TRANSLUCENT) {
                        combinedHash = 31 * combinedHash + vertexBuffer.hashCode();
                    }

                    int firstIndex = 0;
                    GpuBuffer indexBuffer = null;
                    IndexType indexType = null;
                    if (!draw.hasCustomIndexBuffer()) {
                        largestIndexCount = Math.max(largestIndexCount, draw.indexCount());
                    } else {
                        indexBuffer = slice.indexBuffer();
                        indexType = draw.indexType();
                        if (layer != ChunkSectionLayer.TRANSLUCENT) {
                            combinedHash = 31 * combinedHash + indexBuffer.hashCode();
                            combinedHash = 31 * combinedHash + indexType.hashCode();
                        }
                        firstIndex = (int)(slice.indexBufferOffset() / indexType.bytes);
                    }

                    int baseVertex = (int)(slice.vertexBufferOffset() / vertexFormat.getVertexSize());
                    if (layer == ChunkSectionLayer.TRANSLUCENT) {
                        int translucentIndex = topology.addTranslucentSection(sectionIndex);
                        topology.drawList(layer, combinedHash).add(new RenderPass.Draw<>(
                            0,
                            vertexBuffer,
                            indexBuffer,
                            indexType,
                            firstIndex,
                            draw.indexCount(),
                            baseVertex,
                            (sectionUbos, uploader) -> uploader.upload("ChunkSection", sectionUbos[translucentIndex])
                        ));
                    }

                    if (layer != ChunkSectionLayer.TRANSLUCENT) {
                        topology.commandGroup(layer, vertexBuffer, indexBuffer, indexType)
                            .add(draw.indexCount(), firstIndex, baseVertex, sectionIndex);
                    }
                }
            }
        } finally {
            dispatcher.unlock();
        }
        topology.finishBuild(largestIndexCount);
        return topology;
    }

    private static Prepared upload(
        SlotBuffers buffers,
        Matrix4fc modelViewMatrix,
        int textureAtlasWidth,
        int textureAtlasHeight,
        Topology topology,
        float[] visibilities
    ) {
        GpuBuffer uniformBuffer = buffers.terrainBatch.currentBuffer();
        try (GpuBufferSlice.MappedView mapped = uniformBuffer.map(false, true)) {
            ByteBuffer data = mapped.data().order(ByteOrder.nativeOrder());
            Std140Builder.intoBuffer(data)
                .putMat4f(modelViewMatrix)
                .putIVec2(textureAtlasWidth, textureAtlasHeight);
        }

        GpuBuffer instanceBuffer = buffers.sectionInstances.currentBuffer();
        try (GpuBufferSlice.MappedView mapped = instanceBuffer.map(0L, (long)Math.max(1, topology.sectionCount) * INSTANCE_STRIDE, false, true)) {
            ByteBuffer data = mapped.data().order(ByteOrder.nativeOrder());
            for (int index = 0; index < topology.sectionCount; index++) {
                SectionEntry entry = topology.sections[index];
                data.putInt(entry.x).putInt(entry.y).putInt(entry.z).putFloat(visibilities[index]);
            }
        }

        GpuBuffer commandBuffer = buffers.indirectCommands.currentBuffer();
        try (GpuBufferSlice.MappedView mapped = commandBuffer.map(0L, (long)Math.max(1, topology.commandCount) * INDIRECT_COMMAND_STRIDE, false, true)) {
            ByteBuffer data = mapped.data().order(ByteOrder.nativeOrder());
            data.asIntBuffer().put(topology.commandWords, 0, topology.commandCount * 5);
        }

        return new Prepared(
            buffers,
            uniformBuffer.slice(0L, TERRAIN_BATCH_UBO_SIZE),
            instanceBuffer.slice(0L, (long)Math.max(1, topology.sectionCount) * INSTANCE_STRIDE),
            commandBuffer.slice(0L, (long)Math.max(1, topology.commandCount) * INDIRECT_COMMAND_STRIDE),
            topology.batches,
            topology.largestIndexCount,
            topology.logicalDrawCount
        );
    }

    private static void finish(ChunkSectionsToRender sections, Prepared prepared) {
        PREPARED.remove(sections);
        prepared.buffers.terrainBatch.rotate();
        prepared.buffers.sectionInstances.rotate();
        prepared.buffers.indirectCommands.rotate();
    }

    private static void recordBatch(int logicalDraws) {
        frameLogicalDraws += logicalDraws;
        frameSubmits++;
        frameBatches++;
    }

    private static boolean supported(DeviceInfo info) {
        var features = info.features();
        if (!features.drawIndirect() || !features.multiDrawIndirect() || !features.nonZeroFirstInstance()) {
            return false;
        }

        String backend = info.backendName().toLowerCase(Locale.ROOT);
        String vendor = info.vendorName().toLowerCase(Locale.ROOT);
        String name = info.name().toLowerCase(Locale.ROOT);
        return !backend.contains("opengl") || !vendor.contains("intel") || name.contains("arc");
    }

    private static SlotBuffers buffers(int slotId) {
        SlotBuffers buffers = SLOT_BUFFERS[slotId];
        if (buffers == null) {
            buffers = new SlotBuffers(slotId);
            SLOT_BUFFERS[slotId] = buffers;
        }
        return buffers;
    }

    private static SlotCache cache(int slotId) {
        SlotCache cache = SLOT_CACHES[slotId];
        if (cache == null) {
            cache = new SlotCache();
            SLOT_CACHES[slotId] = cache;
        }
        return cache;
    }

    private static void closeBuffers() {
        for (int index = 0; index < SLOT_BUFFERS.length; index++) {
            SlotBuffers buffers = SLOT_BUFFERS[index];
            if (buffers != null) {
                buffers.close();
                SLOT_BUFFERS[index] = null;
            }
            SLOT_CACHES[index] = null;
            SLOT_INVALIDATION_GENERATIONS[index]++;
        }
        PREPARED.clear();
    }

    public record Snapshot(
        String status,
        int logicalDraws,
        int submits,
        int batches,
        int fallbacks,
        int cacheHits,
        int cacheRebuilds
    ) {
    }

    private static final class SectionEntry {
        private SectionRenderDispatcher.RenderSection section;
        private int x;
        private int y;
        private int z;

        private void set(SectionRenderDispatcher.RenderSection section, int x, int y, int z) {
            this.section = section;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static final class VisibleEntry {
        private SectionRenderDispatcher.RenderSection section;
        private long sectionNode;
        private SectionMesh sectionMesh;

        private void set(SectionRenderDispatcher.RenderSection section, long sectionNode, SectionMesh sectionMesh) {
            this.section = section;
            this.sectionNode = sectionNode;
            this.sectionMesh = sectionMesh;
        }
    }

    private static final class Topology {
        private SectionRenderDispatcher dispatcher;
        private long invalidationGeneration;
        private VisibleEntry[] visibleSections = new VisibleEntry[256];
        private int visibleSectionCount;
        private SectionEntry[] sections = new SectionEntry[256];
        private int sectionCount;
        private int[] translucentSections = new int[256];
        private int translucentSectionCount;
        private final EnumMap<ChunkSectionLayer, Int2ObjectOpenHashMap<List<RenderPass.Draw<GpuBufferSlice[]>>>> vanillaGroups = new EnumMap<>(ChunkSectionLayer.class);
        private final List<ArrayList<RenderPass.Draw<GpuBufferSlice[]>>> drawListPool = new ArrayList<>();
        private int drawListCursor;
        private final EnumMap<ChunkSectionLayer, List<CommandGroup>> commandGroups = new EnumMap<>(ChunkSectionLayer.class);
        private final EnumMap<ChunkSectionLayer, List<Batch>> batches = new EnumMap<>(ChunkSectionLayer.class);
        private final List<Batch> batchPool = new ArrayList<>();
        private int batchPoolCursor;
        private int[] commandWords = new int[1024];
        private int commandCount;
        private int largestIndexCount;
        private int logicalDrawCount;

        private Topology() {
            for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
                this.vanillaGroups.put(layer, new Int2ObjectOpenHashMap<>());
                if (layer != ChunkSectionLayer.TRANSLUCENT) {
                    this.commandGroups.put(layer, new ArrayList<>());
                    this.batches.put(layer, new ArrayList<>());
                }
            }
        }

        private void beginRebuild(
            SectionRenderDispatcher dispatcher,
            long invalidationGeneration,
            int visibleSectionCount
        ) {
            this.dispatcher = dispatcher;
            this.invalidationGeneration = invalidationGeneration;
            this.visibleSectionCount = visibleSectionCount;
            this.sectionCount = 0;
            this.translucentSectionCount = 0;
            this.commandCount = 0;
            this.logicalDrawCount = 0;
            this.drawListCursor = 0;
            this.batchPoolCursor = 0;
            this.ensureVisibleCapacity(visibleSectionCount);
            for (var map : this.vanillaGroups.values()) {
                map.clear();
            }
            for (ArrayList<RenderPass.Draw<GpuBufferSlice[]>> draws : this.drawListPool) {
                draws.clear();
            }
            for (List<CommandGroup> groups : this.commandGroups.values()) {
                for (CommandGroup group : groups) {
                    group.clear();
                }
            }
            for (List<Batch> layerBatches : this.batches.values()) {
                layerBatches.clear();
            }
        }

        private void setVisible(
            int index,
            SectionRenderDispatcher.RenderSection section,
            long sectionNode,
            SectionMesh sectionMesh
        ) {
            VisibleEntry entry = this.visibleSections[index];
            if (entry == null) {
                entry = new VisibleEntry();
                this.visibleSections[index] = entry;
            }
            entry.set(section, sectionNode, sectionMesh);
        }

        private int addSection(SectionRenderDispatcher.RenderSection section, int x, int y, int z) {
            this.ensureSectionCapacity(this.sectionCount + 1);
            SectionEntry entry = this.sections[this.sectionCount];
            if (entry == null) {
                entry = new SectionEntry();
                this.sections[this.sectionCount] = entry;
            }
            entry.set(section, x, y, z);
            return this.sectionCount++;
        }

        private int addTranslucentSection(int sectionIndex) {
            if (this.translucentSectionCount == this.translucentSections.length) {
                this.translucentSections = java.util.Arrays.copyOf(
                    this.translucentSections, grow(this.translucentSections.length, this.translucentSectionCount + 1)
                );
            }
            int index = this.translucentSectionCount++;
            this.translucentSections[index] = sectionIndex;
            return index;
        }

        private List<RenderPass.Draw<GpuBufferSlice[]>> drawList(ChunkSectionLayer layer, int hash) {
            List<RenderPass.Draw<GpuBufferSlice[]>> existing = this.vanillaGroups.get(layer).get(hash);
            if (existing != null) {
                return existing;
            }
            ArrayList<RenderPass.Draw<GpuBufferSlice[]>> draws;
            if (this.drawListCursor < this.drawListPool.size()) {
                draws = this.drawListPool.get(this.drawListCursor);
            } else {
                draws = new ArrayList<>();
                this.drawListPool.add(draws);
            }
            this.drawListCursor++;
            this.vanillaGroups.get(layer).put(hash, draws);
            return draws;
        }

        private CommandGroup commandGroup(
            ChunkSectionLayer layer,
            GpuBuffer vertexBuffer,
            @Nullable GpuBuffer indexBuffer,
            @Nullable IndexType indexType
        ) {
            List<CommandGroup> groups = this.commandGroups.get(layer);
            for (CommandGroup group : groups) {
                if (group.active && group.matches(vertexBuffer, indexBuffer, indexType)) {
                    return group;
                }
            }
            for (CommandGroup group : groups) {
                if (!group.active) {
                    group.begin(vertexBuffer, indexBuffer, indexType);
                    return group;
                }
            }
            CommandGroup group = new CommandGroup();
            group.begin(vertexBuffer, indexBuffer, indexType);
            groups.add(group);
            return group;
        }

        private void finishBuild(int largestIndexCount) {
            this.largestIndexCount = largestIndexCount;
            int totalCommands = 0;
            for (List<CommandGroup> groups : this.commandGroups.values()) {
                for (CommandGroup group : groups) {
                    if (group.active) {
                        totalCommands += group.size;
                    }
                }
            }
            this.ensureCommandCapacity(totalCommands * 5);
            int wordOffset = 0;
            long byteOffset = 0L;
            for (ChunkSectionLayer layer : ChunkSectionLayerGroup.OPAQUE.layers()) {
                for (CommandGroup group : this.commandGroups.get(layer)) {
                    if (!group.active) {
                        continue;
                    }
                    for (int start = 0; start < group.size; start += MAX_DRAWS_PER_SUBMIT) {
                        int drawCount = Math.min(MAX_DRAWS_PER_SUBMIT, group.size - start);
                        long batchOffset = byteOffset;
                        for (int index = start; index < start + drawCount; index++) {
                            int commandOffset = index * 4;
                            this.commandWords[wordOffset++] = group.words[commandOffset];
                            this.commandWords[wordOffset++] = 1;
                            this.commandWords[wordOffset++] = group.words[commandOffset + 1];
                            this.commandWords[wordOffset++] = group.words[commandOffset + 2];
                            this.commandWords[wordOffset++] = group.words[commandOffset + 3];
                            byteOffset += INDIRECT_COMMAND_STRIDE;
                        }
                        this.batches.get(layer).add(this.obtainBatch().set(
                            group.vertexBuffer,
                            group.indexBuffer,
                            group.indexType,
                            batchOffset,
                            drawCount
                        ));
                    }
                }
            }
            this.commandCount = totalCommands;
        }

        private Batch obtainBatch() {
            if (this.batchPoolCursor < this.batchPool.size()) {
                return this.batchPool.get(this.batchPoolCursor++);
            }
            Batch batch = new Batch();
            this.batchPool.add(batch);
            this.batchPoolCursor++;
            return batch;
        }

        private void ensureVisibleCapacity(int required) {
            if (required > this.visibleSections.length) {
                this.visibleSections = java.util.Arrays.copyOf(this.visibleSections, grow(this.visibleSections.length, required));
            }
        }

        private void ensureSectionCapacity(int required) {
            if (required > this.sections.length) {
                this.sections = java.util.Arrays.copyOf(this.sections, grow(this.sections.length, required));
            }
        }

        private void ensureCommandCapacity(int requiredWords) {
            if (requiredWords > this.commandWords.length) {
                this.commandWords = java.util.Arrays.copyOf(this.commandWords, grow(this.commandWords.length, requiredWords));
            }
        }

        private static int grow(int current, int required) {
            int grown = Math.max(1, current);
            while (grown < required) {
                grown = Math.min(MAX_COMMANDS * 5, grown << 1);
            }
            return grown;
        }

        private boolean matches(
            SectionRenderDispatcher currentDispatcher,
            List<SectionRenderDispatcher.RenderSection> currentVisibleSections
        ) {
            if (this.dispatcher != currentDispatcher || this.visibleSectionCount != currentVisibleSections.size()) {
                return false;
            }
            for (int index = 0; index < this.visibleSectionCount; index++) {
                VisibleEntry cached = this.visibleSections[index];
                SectionRenderDispatcher.RenderSection current = currentVisibleSections.get(index);
                if (cached.section != current
                    || cached.sectionNode != current.getSectionNode()
                    || cached.sectionMesh != current.getSectionMesh()) {
                    return false;
                }
            }
            for (List<Batch> layerBatches : this.batches.values()) {
                for (Batch batch : layerBatches) {
                    if (batch.vertexBuffer.isClosed() || batch.indexBuffer != null && batch.indexBuffer.isClosed()) {
                        return false;
                    }
                }
            }
            return true;
        }

        private boolean containsVisible(SectionRenderDispatcher.RenderSection section) {
            for (int index = 0; index < this.visibleSectionCount; index++) {
                if (this.visibleSections[index].section == section) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class SlotCache {
        private @Nullable Topology topology;
        private final Matrix4f frameModelView = new Matrix4f();
        private DynamicUniforms.ChunkSectionInfo[] sectionInfos = new DynamicUniforms.ChunkSectionInfo[0];
        private float[] visibilities = new float[0];

        private DynamicUniforms.ChunkSectionInfo[] sectionInfos(int count) {
            if (this.sectionInfos.length != count) {
                this.sectionInfos = new DynamicUniforms.ChunkSectionInfo[count];
            }
            return this.sectionInfos;
        }

        private float[] visibilities(int count) {
            if (this.visibilities.length < count) {
                this.visibilities = new float[Math.max(count, this.visibilities.length << 1)];
            }
            return this.visibilities;
        }
    }

    private static final class CommandGroup {
        private GpuBuffer vertexBuffer;
        private @Nullable GpuBuffer indexBuffer;
        private @Nullable IndexType indexType;
        private int[] words = new int[64];
        private int size;
        private boolean active;

        private void begin(GpuBuffer vertexBuffer, @Nullable GpuBuffer indexBuffer, @Nullable IndexType indexType) {
            this.vertexBuffer = vertexBuffer;
            this.indexBuffer = indexBuffer;
            this.indexType = indexType;
            this.size = 0;
            this.active = true;
        }

        private void clear() {
            this.size = 0;
            this.active = false;
        }

        private boolean matches(GpuBuffer vertexBuffer, @Nullable GpuBuffer indexBuffer, @Nullable IndexType indexType) {
            return this.vertexBuffer == vertexBuffer && this.indexBuffer == indexBuffer && this.indexType == indexType;
        }

        private void add(int indexCount, int firstIndex, int baseVertex, int baseInstance) {
            int offset = this.size * 4;
            if (offset + 4 > this.words.length) {
                this.words = java.util.Arrays.copyOf(this.words, Math.max(offset + 4, this.words.length << 1));
            }
            this.words[offset] = indexCount;
            this.words[offset + 1] = firstIndex;
            this.words[offset + 2] = baseVertex;
            this.words[offset + 3] = baseInstance;
            this.size++;
        }
    }

    private static final class Batch {
        private GpuBuffer vertexBuffer;
        private @Nullable GpuBuffer indexBuffer;
        private @Nullable IndexType indexType;
        private long commandOffset;
        private int drawCount;

        private Batch set(
            GpuBuffer vertexBuffer,
            @Nullable GpuBuffer indexBuffer,
            @Nullable IndexType indexType,
            long commandOffset,
            int drawCount
        ) {
            this.vertexBuffer = vertexBuffer;
            this.indexBuffer = indexBuffer;
            this.indexType = indexType;
            this.commandOffset = commandOffset;
            this.drawCount = drawCount;
            return this;
        }
    }

    private record Prepared(
        SlotBuffers buffers,
        GpuBufferSlice terrainBatch,
        GpuBufferSlice sectionInstances,
        GpuBufferSlice indirectCommands,
        EnumMap<ChunkSectionLayer, List<Batch>> batches,
        int maxIndicesRequired,
        int logicalDrawCount
    ) {
    }

    private static final class SlotBuffers implements AutoCloseable {
        private final MappableRingBuffer terrainBatch;
        private final MappableRingBuffer sectionInstances;
        private final MappableRingBuffer indirectCommands;

        private SlotBuffers(int slotId) {
            this.terrainBatch = new MappableRingBuffer(
                () -> "JR terrain multi-draw UBO slot " + slotId,
                BUFFER_USAGE_CPU_UNIFORM,
                TERRAIN_BATCH_UBO_SIZE
            );
            this.sectionInstances = new MappableRingBuffer(
                () -> "JR terrain multi-draw instances slot " + slotId,
                BUFFER_USAGE_CPU_VERTEX,
                MAX_SECTIONS * INSTANCE_STRIDE
            );
            this.indirectCommands = new MappableRingBuffer(
                () -> "JR terrain multi-draw commands slot " + slotId,
                BUFFER_USAGE_CPU_INDIRECT,
                MAX_COMMANDS * INDIRECT_COMMAND_STRIDE
            );
        }

        @Override
        public void close() {
            this.terrainBatch.close();
            this.sectionInstances.close();
            this.indirectCommands.close();
        }
    }
}
