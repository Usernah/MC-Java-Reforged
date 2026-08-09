package net.jr.ClientRuntime.runtime;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import javax.annotation.Nullable;
import net.jr.ClientRuntime.state.TerrainState;
import net.jr.ClientRuntime.terrain.GlobalTerrainStore;
import net.jr.ClientRuntime.terrain.TerrainViewArea;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector4f;

public final class LevelRendererFields {
    private static final TerrainState BOOTSTRAP_TERRAIN = new TerrainState();
    @Nullable
    private static Frustum bootstrapCullingFrustum;
    @Nullable
    private static TerrainViewArea viewArea;

    private LevelRendererFields() {
    }

    public static void bindCullingFrustum(Frustum frustum) {
        LocalClient client = Client.currentOrNull();
        if (client == null) {
            bootstrapCullingFrustum = frustum;
            return;
        }
        client.render().setFrustum(frustum);
    }

    public static Frustum cullingFrustum() {
        LocalClient client = Client.currentOrNull();
        Frustum frustum = client != null ? client.frustum() : bootstrapCullingFrustum;
        if (frustum == null) {
            throw new IllegalStateException("Active player slot has no culling frustum");
        }
        return frustum;
    }

    public static TerrainState terrain() {
        LocalClient client = Client.currentOrNull();
        return client != null ? client.render().terrain() : BOOTSTRAP_TERRAIN;
    }

    public static TerrainState primaryTerrain() {
        return Client.render(0).terrain();
    }

    public static ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections() {
        return terrain().visibleSections();
    }

    public static ObjectArrayList<SectionRenderDispatcher.RenderSection> nearbyVisibleSections() {
        return terrain().nearbyVisibleSections();
    }

    public static void beginLayer(ChunkSectionLayer renderType) {
        terrain().beginLayer(renderType);
    }

    public static void endLayer(ChunkSectionLayer renderType) {
        terrain().endLayer(renderType);
    }

    public static ViewArea viewArea() {
        if (viewArea == null) {
            throw new IllegalStateException("Global terrain has no ViewArea");
        }
        return viewArea;
    }

    public static ViewArea nullableViewArea() {
        return viewArea;
    }

    public static void setViewArea(@Nullable ViewArea viewArea) {
        if (viewArea == null) {
            LevelRendererFields.viewArea = null;
            return;
        }
        if (!(viewArea instanceof TerrainViewArea terrainViewArea)) {
            throw new IllegalStateException("LevelRenderer tried to install a non-global ViewArea");
        }
        LevelRendererFields.viewArea = terrainViewArea;
    }

    public static boolean hasTerrainStore() {
        return viewArea != null;
    }

    public static GlobalTerrainStore terrainStore() {
        if (viewArea == null) {
            throw new IllegalStateException("Global terrain store is not ready");
        }
        return viewArea.store();
    }

    public static boolean generateClouds() {
        return terrain().generateClouds();
    }

    public static void setGenerateClouds(boolean generateClouds) {
        terrain().setGenerateClouds(generateClouds);
    }

    public static int lastCameraSectionX() {
        return terrain().lastCameraSectionX();
    }

    public static void setLastCameraSectionX(int value) {
        terrain().setLastCameraSectionX(value);
    }

    public static int lastCameraSectionY() {
        return terrain().lastCameraSectionY();
    }

    public static void setLastCameraSectionY(int value) {
        terrain().setLastCameraSectionY(value);
    }

    public static int lastCameraSectionZ() {
        return terrain().lastCameraSectionZ();
    }

    public static void setLastCameraSectionZ(int value) {
        terrain().setLastCameraSectionZ(value);
    }

    public static double prevCamX() {
        return terrain().prevCamX();
    }

    public static void setPrevCamX(double value) {
        terrain().setPrevCamX(value);
    }

    public static double prevCamY() {
        return terrain().prevCamY();
    }

    public static void setPrevCamY(double value) {
        terrain().setPrevCamY(value);
    }

    public static double prevCamZ() {
        return terrain().prevCamZ();
    }

    public static void setPrevCamZ(double value) {
        terrain().setPrevCamZ(value);
    }

    public static double prevCamRotX() {
        return terrain().prevCamRotX();
    }

    public static void setPrevCamRotX(double value) {
        terrain().setPrevCamRotX(value);
    }

    public static double prevCamRotY() {
        return terrain().prevCamRotY();
    }

    public static void setPrevCamRotY(double value) {
        terrain().setPrevCamRotY(value);
    }

    public static int prevCloudX() {
        return terrain().prevCloudX();
    }

    public static void setPrevCloudX(int value) {
        terrain().setPrevCloudX(value);
    }

    public static int prevCloudY() {
        return terrain().prevCloudY();
    }

    public static void setPrevCloudY(int value) {
        terrain().setPrevCloudY(value);
    }

    public static int prevCloudZ() {
        return terrain().prevCloudZ();
    }

    public static void setPrevCloudZ(int value) {
        terrain().setPrevCloudZ(value);
    }

    public static Vec3 prevCloudColor() {
        return terrain().prevCloudColor();
    }

    public static void setPrevCloudColor(Vec3 value) {
        terrain().setPrevCloudColor(value);
    }

    public static CloudStatus prevCloudsType() {
        return terrain().prevCloudsType();
    }

    public static void setPrevCloudsType(CloudStatus value) {
        terrain().setPrevCloudsType(value);
    }

    public static Frustum capturedFrustum() {
        return terrain().capturedFrustum();
    }

    public static void setCapturedFrustum(Frustum value) {
        terrain().setCapturedFrustum(value);
    }

    public static Vector4f[] frustumPoints() {
        return terrain().frustumPoints();
    }

    public static Vector3d frustumPos() {
        return terrain().frustumPos();
    }

    public static double xTransparentOld() {
        return terrain().xTransparentOld();
    }

    public static void setXTransparentOld(double value) {
        terrain().setXTransparentOld(value);
    }

    public static double yTransparentOld() {
        return terrain().yTransparentOld();
    }

    public static void setYTransparentOld(double value) {
        terrain().setYTransparentOld(value);
    }

    public static double zTransparentOld() {
        return terrain().zTransparentOld();
    }

    public static void setZTransparentOld(double value) {
        terrain().setZTransparentOld(value);
    }
}
