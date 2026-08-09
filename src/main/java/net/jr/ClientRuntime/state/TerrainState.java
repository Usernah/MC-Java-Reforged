package net.jr.ClientRuntime.state;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.jr.ClientRuntime.terrain.SlotTerrainView;
import net.jr.ClientRuntime.terrain.GlobalTerrainStore;
import net.jr.ClientRuntime.terrain.TerrainGraphArea;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector4f;

public final class TerrainState {
    private final SectionOcclusionGraph sectionOcclusionGraph = new SectionOcclusionGraph();
    private final ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections = new ObjectArrayList<>(10000);
    private final ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleScratchSections = new ObjectArrayList<>(10000);
    private final ObjectArrayList<SectionRenderDispatcher.RenderSection> occlusionCandidates = new ObjectArrayList<>(10000);
    private final ObjectArrayList<SectionRenderDispatcher.RenderSection> nearbyVisibleSections = new ObjectArrayList<>(50);
    private final Map<ChunkSectionLayer, ObjectArrayList<SectionRenderDispatcher.RenderSection>> sectionsByLayer = new IdentityHashMap<>();
    private final SlotTerrainView view = new SlotTerrainView();
    @Nullable
    private TerrainGraphArea graphArea;
    private boolean graphReady;
    @Nullable
    private ChunkSectionLayer activeLayer;
    @Nullable
    private Vec3 cameraPosition;
    private final Vector4f[] frustumPoints = new Vector4f[8];
    private final Vector3d frustumPos = new Vector3d(0.0D, 0.0D, 0.0D);
    private boolean generateClouds = true;
    private int lastCameraSectionX = Integer.MIN_VALUE;
    private int lastCameraSectionY = Integer.MIN_VALUE;
    private int lastCameraSectionZ = Integer.MIN_VALUE;
    private double prevCamX = Double.MIN_VALUE;
    private double prevCamY = Double.MIN_VALUE;
    private double prevCamZ = Double.MIN_VALUE;
    private double prevCamRotX = Double.MIN_VALUE;
    private double prevCamRotY = Double.MIN_VALUE;
    private int prevCloudX = Integer.MIN_VALUE;
    private int prevCloudY = Integer.MIN_VALUE;
    private int prevCloudZ = Integer.MIN_VALUE;
    private Vec3 prevCloudColor = Vec3.ZERO;
    @Nullable
    private CloudStatus prevCloudsType;
    @Nullable
    private Frustum capturedFrustum;
    private double xTransparentOld;
    private double yTransparentOld;
    private double zTransparentOld;

    public TerrainState() {
        for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            this.sectionsByLayer.put(layer, new ObjectArrayList<>(10000));
        }
    }

    public SectionOcclusionGraph sectionOcclusionGraph() {
        return this.sectionOcclusionGraph;
    }

    public TerrainGraphArea prepareGraph(
        GlobalTerrainStore store,
        ClientLevel level,
        int viewDistance,
        LevelRenderer levelRenderer,
        int slotId
    ) {
        if (this.graphArea == null || !this.graphArea.matches(store, this.view, level, viewDistance, slotId)) {
            this.graphArea = TerrainGraphArea.create(
                store.dispatcher(),
                level,
                viewDistance,
                this.sectionOcclusionGraph,
                store,
                this.view,
                slotId
            );
            this.sectionOcclusionGraph.waitAndReset(this.graphArea);
            this.graphReady = false;
        }
        return this.graphArea;
    }

    public boolean consumeGraphUpdate() {
        boolean updated = this.sectionOcclusionGraph.consumeFrustumUpdate();
        if (updated) {
            this.graphReady = true;
        }
        return updated;
    }

    public boolean isGraphReady() {
        return this.graphReady;
    }

    public ObjectArrayList<SectionRenderDispatcher.RenderSection> occlusionCandidates() {
        return this.occlusionCandidates;
    }

    public void onChunkLoaded(ChunkPos chunkPos) {
        if (this.graphArea != null) {
            this.invalidateGraph();
        }
    }

    public void invalidateGraph() {
        this.sectionOcclusionGraph.invalidate();
        this.graphReady = false;
    }

    public void onSectionCompiled(SectionRenderDispatcher.RenderSection section) {
        if (this.graphArea != null) {
            this.sectionOcclusionGraph.schedulePropagationFrom(section);
        }
    }

    public void releaseGraph(GlobalTerrainStore store) {
        if (this.graphArea != null && this.graphArea.belongsTo(store)) {
            this.sectionOcclusionGraph.waitAndReset(null);
            this.graphArea = null;
            this.graphReady = false;
            this.occlusionCandidates.clear();
        }
    }

    public ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections() {
        return this.visibleSections;
    }

    public ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleScratchSections() {
        return this.visibleScratchSections;
    }

    public ObjectArrayList<SectionRenderDispatcher.RenderSection> nearbyVisibleSections() {
        return this.nearbyVisibleSections;
    }

    public void replaceVisibleSections(ObjectArrayList<SectionRenderDispatcher.RenderSection> sections) {
        this.visibleSections.clear();
        this.visibleSections.addAll(sections);
    }

    public ObjectArrayList<SectionRenderDispatcher.RenderSection> sectionsForActivePass() {
        if (this.activeLayer == null) {
            return this.visibleSections;
        }
        ObjectArrayList<SectionRenderDispatcher.RenderSection> sections = this.sectionsByLayer.get(this.activeLayer);
        if (sections == null) {
            throw new IllegalStateException("No terrain section list exists for layer " + this.activeLayer);
        }
        return sections;
    }

    public void rebuildLayerSections() {
        for (ObjectArrayList<SectionRenderDispatcher.RenderSection> sections : this.sectionsByLayer.values()) {
            sections.clear();
        }
        for (SectionRenderDispatcher.RenderSection section : this.visibleSections) {
            SectionMesh compiled = section.getSectionMesh();
            for (Map.Entry<ChunkSectionLayer, ObjectArrayList<SectionRenderDispatcher.RenderSection>> entry : this.sectionsByLayer.entrySet()) {
                if (!compiled.isEmpty(entry.getKey())) {
                    entry.getValue().add(section);
                }
            }
        }
    }

    public int layerSize(ChunkSectionLayer renderType) {
        ObjectArrayList<SectionRenderDispatcher.RenderSection> sections = this.sectionsByLayer.get(renderType);
        return sections == null ? 0 : sections.size();
    }

    public int compiledVisibleCount() {
        int count = 0;
        for (SectionRenderDispatcher.RenderSection section : this.visibleSections) {
            if (section.getSectionMesh() != CompiledSectionMesh.UNCOMPILED) {
                count++;
            }
        }
        return count;
    }

    public int nonEmptyVisibleCount() {
        int count = 0;
        for (SectionRenderDispatcher.RenderSection section : this.visibleSections) {
            if (section.getSectionMesh().hasRenderableLayers()) {
                count++;
            }
        }
        return count;
    }

    public void beginLayer(ChunkSectionLayer renderType) {
        if (this.activeLayer != null) {
            throw new IllegalStateException("Terrain layer " + this.activeLayer + " is already active");
        }
        if (!this.sectionsByLayer.containsKey(renderType)) {
            throw new IllegalStateException("Unknown terrain layer " + renderType);
        }
        this.activeLayer = renderType;
    }

    public void endLayer(ChunkSectionLayer renderType) {
        if (this.activeLayer != renderType) {
            throw new IllegalStateException("Ending terrain layer " + renderType + " while " + this.activeLayer + " is active");
        }
        this.activeLayer = null;
    }

    public SlotTerrainView view() {
        return this.view;
    }

    public Vec3 cameraPosition() {
        if (this.cameraPosition == null) {
            throw new IllegalStateException("Terrain camera position has not been captured");
        }
        return this.cameraPosition;
    }

    @Nullable
    public Vec3 nullableCameraPosition() {
        return this.cameraPosition;
    }

    public void setCameraPosition(Vec3 cameraPosition) {
        this.cameraPosition = cameraPosition;
    }

    public Vector4f[] frustumPoints() {
        return this.frustumPoints;
    }

    public Vector3d frustumPos() {
        return this.frustumPos;
    }

    public boolean generateClouds() {
        return this.generateClouds;
    }

    public void setGenerateClouds(boolean generateClouds) {
        this.generateClouds = generateClouds;
    }

    public int lastCameraSectionX() {
        return this.lastCameraSectionX;
    }

    public void setLastCameraSectionX(int lastCameraSectionX) {
        this.lastCameraSectionX = lastCameraSectionX;
    }

    public int lastCameraSectionY() {
        return this.lastCameraSectionY;
    }

    public void setLastCameraSectionY(int lastCameraSectionY) {
        this.lastCameraSectionY = lastCameraSectionY;
    }

    public int lastCameraSectionZ() {
        return this.lastCameraSectionZ;
    }

    public void setLastCameraSectionZ(int lastCameraSectionZ) {
        this.lastCameraSectionZ = lastCameraSectionZ;
    }

    public double prevCamX() {
        return this.prevCamX;
    }

    public void setPrevCamX(double prevCamX) {
        this.prevCamX = prevCamX;
    }

    public double prevCamY() {
        return this.prevCamY;
    }

    public void setPrevCamY(double prevCamY) {
        this.prevCamY = prevCamY;
    }

    public double prevCamZ() {
        return this.prevCamZ;
    }

    public void setPrevCamZ(double prevCamZ) {
        this.prevCamZ = prevCamZ;
    }

    public double prevCamRotX() {
        return this.prevCamRotX;
    }

    public void setPrevCamRotX(double prevCamRotX) {
        this.prevCamRotX = prevCamRotX;
    }

    public double prevCamRotY() {
        return this.prevCamRotY;
    }

    public void setPrevCamRotY(double prevCamRotY) {
        this.prevCamRotY = prevCamRotY;
    }

    public int prevCloudX() {
        return this.prevCloudX;
    }

    public void setPrevCloudX(int prevCloudX) {
        this.prevCloudX = prevCloudX;
    }

    public int prevCloudY() {
        return this.prevCloudY;
    }

    public void setPrevCloudY(int prevCloudY) {
        this.prevCloudY = prevCloudY;
    }

    public int prevCloudZ() {
        return this.prevCloudZ;
    }

    public void setPrevCloudZ(int prevCloudZ) {
        this.prevCloudZ = prevCloudZ;
    }

    public Vec3 prevCloudColor() {
        return this.prevCloudColor;
    }

    public void setPrevCloudColor(Vec3 prevCloudColor) {
        this.prevCloudColor = prevCloudColor;
    }

    @Nullable
    public CloudStatus prevCloudsType() {
        return this.prevCloudsType;
    }

    public void setPrevCloudsType(@Nullable CloudStatus prevCloudsType) {
        this.prevCloudsType = prevCloudsType;
    }

    @Nullable
    public Frustum capturedFrustum() {
        return this.capturedFrustum;
    }

    public void setCapturedFrustum(@Nullable Frustum capturedFrustum) {
        this.capturedFrustum = capturedFrustum;
    }

    public double xTransparentOld() {
        return this.xTransparentOld;
    }

    public void setXTransparentOld(double xTransparentOld) {
        this.xTransparentOld = xTransparentOld;
    }

    public double yTransparentOld() {
        return this.yTransparentOld;
    }

    public void setYTransparentOld(double yTransparentOld) {
        this.yTransparentOld = yTransparentOld;
    }

    public double zTransparentOld() {
        return this.zTransparentOld;
    }

    public void setZTransparentOld(double zTransparentOld) {
        this.zTransparentOld = zTransparentOld;
    }

    public void clear() {
        this.visibleSections.clear();
        this.nearbyVisibleSections.clear();
        this.occlusionCandidates.clear();
        this.view.clear();
        for (ObjectArrayList<SectionRenderDispatcher.RenderSection> sections : this.sectionsByLayer.values()) {
            sections.clear();
        }
        this.activeLayer = null;
        this.sectionOcclusionGraph.waitAndReset(null);
        this.graphArea = null;
        this.graphReady = false;
        this.cameraPosition = null;
        this.generateClouds = true;
        this.lastCameraSectionX = Integer.MIN_VALUE;
        this.lastCameraSectionY = Integer.MIN_VALUE;
        this.lastCameraSectionZ = Integer.MIN_VALUE;
        this.prevCamX = Double.MIN_VALUE;
        this.prevCamY = Double.MIN_VALUE;
        this.prevCamZ = Double.MIN_VALUE;
        this.prevCamRotX = Double.MIN_VALUE;
        this.prevCamRotY = Double.MIN_VALUE;
        this.prevCloudX = Integer.MIN_VALUE;
        this.prevCloudY = Integer.MIN_VALUE;
        this.prevCloudZ = Integer.MIN_VALUE;
        this.prevCloudColor = Vec3.ZERO;
        this.prevCloudsType = null;
        this.capturedFrustum = null;
        this.xTransparentOld = 0.0D;
        this.yTransparentOld = 0.0D;
        this.zTransparentOld = 0.0D;
    }
}
