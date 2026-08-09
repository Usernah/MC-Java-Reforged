package net.alnv.javareforged.mixin.SSM;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.alnv.javareforged.ClientRuntime.runtime.*;
import net.alnv.javareforged.ClientRuntime.slot.PlayerSlot;
import net.alnv.javareforged.ClientRuntime.state.RenderState;
import net.alnv.javareforged.client.render.world.CloudRenderer;
import net.alnv.javareforged.client.render.world.SkyRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererSSMixin {

    @Shadow
    @Nullable
    private Frustum cullingFrustum;

    @Shadow
    private int rainSoundTime;

    @Shadow
    private int ticks;

    @Redirect(
        method = "renderSky",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;drawWithShader(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/ShaderInstance;)V",
            ordinal = 0
        )
    )
    private void javareforged$renderSharedSkyMesh(
        VertexBuffer vanillaSkyBuffer,
        Matrix4f modelViewMatrix,
        Matrix4f projectionMatrix,
        ShaderInstance shader
    ) {
        SkyRenderer.get().renderSky(modelViewMatrix, projectionMatrix, shader);
    }

    @Redirect(
        method = "renderSky",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;drawWithShader(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/ShaderInstance;)V",
            ordinal = 1
        )
    )
    private void javareforged$renderSharedStarMesh(
        VertexBuffer vanillaStarBuffer,
        Matrix4f modelViewMatrix,
        Matrix4f projectionMatrix,
        ShaderInstance shader
    ) {
        SkyRenderer.get().renderStars(modelViewMatrix, projectionMatrix, shader);
    }

    @Redirect(
        method = "renderSky",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;drawWithShader(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/ShaderInstance;)V",
            ordinal = 2
        )
    )
    private void javareforged$renderSharedDarkSkyMesh(
        VertexBuffer vanillaDarkSkyBuffer,
        Matrix4f modelViewMatrix,
        Matrix4f projectionMatrix,
        ShaderInstance shader
    ) {
        SkyRenderer.get().renderDarkSky(modelViewMatrix, projectionMatrix, shader);
    }

    @Redirect(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderClouds(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FDDD)V"
        )
    )
    private void javareforged$renderSharedCloudMesh(
        LevelRenderer levelRenderer,
        PoseStack poseStack,
        Matrix4f frustumMatrix,
        Matrix4f projectionMatrix,
        float partialTick,
        double cameraX,
        double cameraY,
        double cameraZ
    ) {
        CloudRenderer.get().render(
            frustumMatrix,
            projectionMatrix,
            partialTick,
            cameraX,
            cameraY,
            cameraZ,
            this.ticks
        );
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void javareforged$releaseSharedCloudMesh(CallbackInfo ci) {
        TransparencyPass.reset((LevelRenderer)(Object)this);
        CloudRenderer.get().close();
        SkyRenderer.get().close();
    }

    @Inject(method = "onResourceManagerReload", at = @At("HEAD"))
    private void splitTest$resetSlotTransparencyChains(ResourceManager resourceManager, CallbackInfo ci) {
        TransparencyPass.reset((LevelRenderer)(Object)this);
        VanillaBackgroundBlur.closeAll();
    }

    @Inject(method = "setBlocksDirty", at = @At("HEAD"), cancellable = true)
    private void splitTest$setBlocksDirty(
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ,
        CallbackInfo ci
    ) {
        TerrainMarks.setBlocksDirty(minX, minY, minZ, maxX, maxY, maxZ);
        ci.cancel();
    }

    @Inject(method = "setSectionDirtyWithNeighbors", at = @At("HEAD"), cancellable = true)
    private void splitTest$setSectionDirtyWithNeighbors(int sectionX, int sectionY, int sectionZ, CallbackInfo ci) {
        TerrainMarks.setSectionDirtyWithNeighbors(sectionX, sectionY, sectionZ);
        ci.cancel();
    }

    @Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"), cancellable = true)
    private void splitTest$setSectionDirty(
        int sectionX,
        int sectionY,
        int sectionZ,
        boolean reRenderOnMainThread,
        CallbackInfo ci
    ) {
        TerrainMarks.setSectionDirty(sectionX, sectionY, sectionZ, reRenderOnMainThread);
        ci.cancel();
    }

    @Redirect(
            method = "prepareCullFrustum",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;cullingFrustum:Lnet/minecraft/client/renderer/culling/Frustum;", opcode = Opcodes.PUTFIELD)
    )
    private void splitTest$captureCullingFrustum(LevelRenderer levelRenderer, Frustum frustum) {
        LevelRendererFields.bindCullingFrustum(frustum);
        this.cullingFrustum = null;
    }

    @Redirect(
            method = {"prepareCullFrustum", "renderLevel", "getFrustum"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;cullingFrustum:Lnet/minecraft/client/renderer/culling/Frustum;", opcode = Opcodes.GETFIELD)
    )
    private Frustum splitTest$readCullingFrustum(LevelRenderer levelRenderer) {
        return LevelRendererFields.cullingFrustum();
    }

    @Inject(method = "renderSectionLayer", at = @At("HEAD"))
    private void splitTest$beginTerrainLayer(
            RenderType renderType,
            double x,
            double y,
            double z,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci
    ) {
        LevelRendererFields.beginLayer(renderType);
    }

    @Inject(method = "renderSectionLayer", at = @At("RETURN"))
    private void splitTest$endTerrainLayer(
            RenderType renderType,
            double x,
            double y,
            double z,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci
    ) {
        LevelRendererFields.endLayer(renderType);
    }

    @Inject(method = "addRecentlyCompiledSection", at = @At("HEAD"), cancellable = true)
    private void splitTest$sectionCompiled(SectionRenderDispatcher.RenderSection section, CallbackInfo ci) {
        TerrainSectionOwners.addRecentlyCompiledSection(section);
        ci.cancel();
    }

    @Inject(method = "onChunkLoaded", at = @At("HEAD"), cancellable = true)
    private void splitTest$chunkLoaded(ChunkPos chunkPos, CallbackInfo ci) {
        TerrainCoordinator.onChunkLoaded(chunkPos);
        ci.cancel();
    }

    @Redirect(
            method = "addParticleInternal(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;getPosition()Lnet/minecraft/world/phys/Vec3;"
            )
    )
    private Vec3 splitTest$useActiveSlotCameraForParticleDistance(Camera camera) {
        Integer slotId = ActiveSlot.idOrNull();
        if (slotId == null) {
            return camera.getPosition();
        }

        PlayerSlot slot = LocalPlayers.INSTANCE.slots().slot(slotId);

        if (slot.renderState().camera().isInitialized()) {
            return slot.renderState().camera().getPosition();
        }

        Entity cameraEntity = slot.renderState().cameraEntity();
        if (cameraEntity == null) {
            cameraEntity = slot.gameplayState().player();
        }

        if (cameraEntity != null) {
            return cameraEntity.getEyePosition();
        }

        return camera.getPosition();
    }

    @Redirect(
            method = "tickRain",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;rainSoundTime:I", opcode = Opcodes.GETFIELD)
    )
    private int splitTest$getSlotRainSoundTime(LevelRenderer levelRenderer) {
        RenderState state = this.activeSlotRenderStateOrNull();
        return state != null ? state.rainSoundTime() : this.rainSoundTime;
    }

    @Redirect(
            method = "tickRain",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;rainSoundTime:I", opcode = Opcodes.PUTFIELD)
    )
    private void splitTest$setSlotRainSoundTime(LevelRenderer levelRenderer, int rainSoundTime) {
        RenderState state = this.activeSlotRenderStateOrNull();
        if (state == null) {
            this.rainSoundTime = rainSoundTime;
            return;
        }
        state.setRainSoundTime(rainSoundTime);
    }

    private RenderState activeSlotRenderStateOrNull() {
        Integer slotId = ActiveSlot.idOrNull();
        if (slotId == null) {
            return null;
        }
        return LocalPlayers.INSTANCE.slots().slot(slotId).renderState();
    }

    @Inject(method = "setLevel", at = @At("HEAD"), cancellable = true)
    private void splitTest$ignoreSecondarySlotWorldResets(ClientLevel level, CallbackInfo ci) {
        Integer slotId = ActiveSlot.idOrNull();
        if (slotId != null && slotId != 0) {
            ci.cancel();
        }
    }

    @Inject(method = "compileSections", at = @At("HEAD"), cancellable = true)
    private void splitTest$compileGlobalTerrain(Camera camera, CallbackInfo ci) {
        TerrainCoordinator.compileSections(camera);
        ci.cancel();
    }

    @Inject(method = "setupRender", at = @At("HEAD"), cancellable = true)
    private void splitTest$setupSlotTerrain(Camera camera, Frustum frustum, boolean captured, boolean spectator, CallbackInfo ci) {
        TerrainCoordinator.setup(camera, frustum);
        ci.cancel();
    }

    @Redirect(
            method = "renderLevel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;pollLightUpdates()V")
    )
    private void splitTest$pollLightOnlyDuringTerrainUpdate(ClientLevel level) {
        TerrainCoordinator.pollLightUpdates(level);
    }

    @Redirect(
            method = "renderLevel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/lighting/LevelLightEngine;runLightUpdates()I")
    )
    private int splitTest$runLightOnlyDuringTerrainUpdate(LevelLightEngine lightEngine) {
        return TerrainCoordinator.runLightUpdates(lightEngine);
    }

    @Redirect(
            method = "allChanged",
            at = @At(value = "NEW", target = "net/minecraft/client/renderer/ViewArea")
    )
    private ViewArea splitTest$createGlobalTerrainView(
            SectionRenderDispatcher dispatcher,
            Level level,
            int viewDistance,
            LevelRenderer levelRenderer
    ) {
        return TerrainCoordinator.createViewArea(dispatcher, level, viewDistance, levelRenderer);
    }

    @Redirect(
            method = {
                    "setLevel", "allChanged", "getSectionStatistics", "getTotalSections", "setupRender", "setSectionDirty",
                    "isSectionCompiled"
            },
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;viewArea:Lnet/minecraft/client/renderer/ViewArea;", opcode = Opcodes.GETFIELD)
    )
    private ViewArea splitTest$readViewArea(LevelRenderer levelRenderer) {
        return LevelRendererFields.nullableViewArea();
    }

    @Redirect(
            method = {"setLevel", "allChanged"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;viewArea:Lnet/minecraft/client/renderer/ViewArea;", opcode = Opcodes.PUTFIELD)
    )
    private void splitTest$writeViewArea(LevelRenderer levelRenderer, @Nullable ViewArea viewArea) {
        LevelRendererFields.setViewArea(viewArea);
    }

    @Redirect(
            method = {
                    "setLevel", "allChanged", "countRenderedSections", "setupRender", "applyFrustum", "renderLevel",
                    "renderSectionLayer", "renderDebug", "compileSections", "iterateVisibleBlockEntities"
            },
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;visibleSections:Lit/unimi/dsi/fastutil/objects/ObjectArrayList;", opcode = Opcodes.GETFIELD)
    )
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> splitTest$readVisibleSections(LevelRenderer levelRenderer) {
        return LevelRendererFields.visibleSections();
    }

    @Redirect(
            method = {"setLevel", "setupRender"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;lastCameraSectionX:I", opcode = Opcodes.GETFIELD)
    )
    private int splitTest$readLastCameraSectionX(LevelRenderer levelRenderer) {
        return LevelRendererFields.lastCameraSectionX();
    }

    @Redirect(
            method = {"setLevel", "setupRender"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;lastCameraSectionX:I", opcode = Opcodes.PUTFIELD)
    )
    private void splitTest$writeLastCameraSectionX(LevelRenderer levelRenderer, int value) {
        LevelRendererFields.setLastCameraSectionX(value);
    }

    @Redirect(
            method = {"setLevel", "setupRender"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;lastCameraSectionY:I", opcode = Opcodes.GETFIELD)
    )
    private int splitTest$readLastCameraSectionY(LevelRenderer levelRenderer) {
        return LevelRendererFields.lastCameraSectionY();
    }

    @Redirect(
            method = {"setLevel", "setupRender"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;lastCameraSectionY:I", opcode = Opcodes.PUTFIELD)
    )
    private void splitTest$writeLastCameraSectionY(LevelRenderer levelRenderer, int value) {
        LevelRendererFields.setLastCameraSectionY(value);
    }

    @Redirect(
            method = {"setLevel", "setupRender"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;lastCameraSectionZ:I", opcode = Opcodes.GETFIELD)
    )
    private int splitTest$readLastCameraSectionZ(LevelRenderer levelRenderer) {
        return LevelRendererFields.lastCameraSectionZ();
    }

    @Redirect(
            method = {"setLevel", "setupRender"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;lastCameraSectionZ:I", opcode = Opcodes.PUTFIELD)
    )
    private void splitTest$writeLastCameraSectionZ(LevelRenderer levelRenderer, int value) {
        LevelRendererFields.setLastCameraSectionZ(value);
    }

    @Redirect(method = "setupRender", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;prevCamX:D", opcode = Opcodes.GETFIELD))
    private double splitTest$readPrevCamX(LevelRenderer levelRenderer) {
        return LevelRendererFields.prevCamX();
    }

    @Redirect(method = "setupRender", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;prevCamX:D", opcode = Opcodes.PUTFIELD))
    private void splitTest$writePrevCamX(LevelRenderer levelRenderer, double value) {
        LevelRendererFields.setPrevCamX(value);
    }

    @Redirect(method = "setupRender", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;prevCamY:D", opcode = Opcodes.GETFIELD))
    private double splitTest$readPrevCamY(LevelRenderer levelRenderer) {
        return LevelRendererFields.prevCamY();
    }

    @Redirect(method = "setupRender", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;prevCamY:D", opcode = Opcodes.PUTFIELD))
    private void splitTest$writePrevCamY(LevelRenderer levelRenderer, double value) {
        LevelRendererFields.setPrevCamY(value);
    }

    @Redirect(method = "setupRender", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;prevCamZ:D", opcode = Opcodes.GETFIELD))
    private double splitTest$readPrevCamZ(LevelRenderer levelRenderer) {
        return LevelRendererFields.prevCamZ();
    }

    @Redirect(method = "setupRender", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;prevCamZ:D", opcode = Opcodes.PUTFIELD))
    private void splitTest$writePrevCamZ(LevelRenderer levelRenderer, double value) {
        LevelRendererFields.setPrevCamZ(value);
    }

    @Redirect(method = "setupRender", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;prevCamRotX:D", opcode = Opcodes.GETFIELD))
    private double splitTest$readPrevCamRotX(LevelRenderer levelRenderer) {
        return LevelRendererFields.prevCamRotX();
    }

    @Redirect(method = "setupRender", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;prevCamRotX:D", opcode = Opcodes.PUTFIELD))
    private void splitTest$writePrevCamRotX(LevelRenderer levelRenderer, double value) {
        LevelRendererFields.setPrevCamRotX(value);
    }

    @Redirect(method = "setupRender", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;prevCamRotY:D", opcode = Opcodes.GETFIELD))
    private double splitTest$readPrevCamRotY(LevelRenderer levelRenderer) {
        return LevelRendererFields.prevCamRotY();
    }

    @Redirect(method = "setupRender", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;prevCamRotY:D", opcode = Opcodes.PUTFIELD))
    private void splitTest$writePrevCamRotY(LevelRenderer levelRenderer, double value) {
        LevelRendererFields.setPrevCamRotY(value);
    }

    @Redirect(
            method = {"captureFrustum", "renderLevel", "renderDebug", "killFrustum", "getFrustum"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;capturedFrustum:Lnet/minecraft/client/renderer/culling/Frustum;", opcode = Opcodes.GETFIELD)
    )
    private Frustum splitTest$readCapturedFrustum(LevelRenderer levelRenderer) {
        return LevelRendererFields.capturedFrustum();
    }

    @Redirect(
            method = {"captureFrustum", "killFrustum"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;capturedFrustum:Lnet/minecraft/client/renderer/culling/Frustum;", opcode = Opcodes.PUTFIELD)
    )
    private void splitTest$writeCapturedFrustum(LevelRenderer levelRenderer, @Nullable Frustum value) {
        LevelRendererFields.setCapturedFrustum(value);
    }

    @Redirect(
            method = {"captureFrustum", "addFrustumVertex", "addFrustumQuad"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;frustumPoints:[Lorg/joml/Vector4f;", opcode = Opcodes.GETFIELD)
    )
    private Vector4f[] splitTest$readFrustumPoints(LevelRenderer levelRenderer) {
        return LevelRendererFields.frustumPoints();
    }

    @Redirect(
            method = {"captureFrustum", "renderDebug"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;frustumPos:Lorg/joml/Vector3d;", opcode = Opcodes.GETFIELD)
    )
    private Vector3d splitTest$readFrustumPos(LevelRenderer levelRenderer) {
        return LevelRendererFields.frustumPos();
    }

    @Redirect(method = "renderSectionLayer", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;xTransparentOld:D", opcode = Opcodes.GETFIELD))
    private double splitTest$readXTransparentOld(LevelRenderer levelRenderer) {
        return LevelRendererFields.xTransparentOld();
    }

    @Redirect(method = "renderSectionLayer", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;xTransparentOld:D", opcode = Opcodes.PUTFIELD))
    private void splitTest$writeXTransparentOld(LevelRenderer levelRenderer, double value) {
        LevelRendererFields.setXTransparentOld(value);
    }

    @Redirect(method = "renderSectionLayer", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;yTransparentOld:D", opcode = Opcodes.GETFIELD))
    private double splitTest$readYTransparentOld(LevelRenderer levelRenderer) {
        return LevelRendererFields.yTransparentOld();
    }

    @Redirect(method = "renderSectionLayer", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;yTransparentOld:D", opcode = Opcodes.PUTFIELD))
    private void splitTest$writeYTransparentOld(LevelRenderer levelRenderer, double value) {
        LevelRendererFields.setYTransparentOld(value);
    }

    @Redirect(method = "renderSectionLayer", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;zTransparentOld:D", opcode = Opcodes.GETFIELD))
    private double splitTest$readZTransparentOld(LevelRenderer levelRenderer) {
        return LevelRendererFields.zTransparentOld();
    }

    @Redirect(method = "renderSectionLayer", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;zTransparentOld:D", opcode = Opcodes.PUTFIELD))
    private void splitTest$writeZTransparentOld(LevelRenderer levelRenderer, double value) {
        LevelRendererFields.setZTransparentOld(value);
    }

    @Redirect(method = {"allChanged", "renderClouds", "needsUpdate"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;generateClouds:Z", opcode = Opcodes.GETFIELD))
    private boolean splitTest$readGenerateClouds(LevelRenderer levelRenderer) {
        return LevelRendererFields.generateClouds();
    }

    @Redirect(method = {"allChanged", "renderClouds", "needsUpdate"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;generateClouds:Z", opcode = Opcodes.PUTFIELD))
    private void splitTest$writeGenerateClouds(LevelRenderer levelRenderer, boolean value) {
        LevelRendererFields.setGenerateClouds(value);
    }

    @Redirect(method = "renderClouds", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;prevCloudX:I", opcode = Opcodes.GETFIELD))
    private int splitTest$readPrevCloudX(LevelRenderer levelRenderer) {
        return LevelRendererFields.prevCloudX();
    }

    @Redirect(method = "renderClouds", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;prevCloudX:I", opcode = Opcodes.PUTFIELD))
    private void splitTest$writePrevCloudX(LevelRenderer levelRenderer, int value) {
        LevelRendererFields.setPrevCloudX(value);
    }

    @Redirect(method = "renderClouds", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;prevCloudY:I", opcode = Opcodes.GETFIELD))
    private int splitTest$readPrevCloudY(LevelRenderer levelRenderer) {
        return LevelRendererFields.prevCloudY();
    }

    @Redirect(method = "renderClouds", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;prevCloudY:I", opcode = Opcodes.PUTFIELD))
    private void splitTest$writePrevCloudY(LevelRenderer levelRenderer, int value) {
        LevelRendererFields.setPrevCloudY(value);
    }

    @Redirect(method = "renderClouds", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;prevCloudZ:I", opcode = Opcodes.GETFIELD))
    private int splitTest$readPrevCloudZ(LevelRenderer levelRenderer) {
        return LevelRendererFields.prevCloudZ();
    }

    @Redirect(method = "renderClouds", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;prevCloudZ:I", opcode = Opcodes.PUTFIELD))
    private void splitTest$writePrevCloudZ(LevelRenderer levelRenderer, int value) {
        LevelRendererFields.setPrevCloudZ(value);
    }

    @Redirect(method = "renderClouds", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;prevCloudColor:Lnet/minecraft/world/phys/Vec3;", opcode = Opcodes.GETFIELD))
    private Vec3 splitTest$readPrevCloudColor(LevelRenderer levelRenderer) {
        return LevelRendererFields.prevCloudColor();
    }

    @Redirect(method = "renderClouds", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;prevCloudColor:Lnet/minecraft/world/phys/Vec3;", opcode = Opcodes.PUTFIELD))
    private void splitTest$writePrevCloudColor(LevelRenderer levelRenderer, Vec3 value) {
        LevelRendererFields.setPrevCloudColor(value);
    }

    @Redirect(method = "renderClouds", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;prevCloudsType:Lnet/minecraft/client/CloudStatus;", opcode = Opcodes.GETFIELD))
    private CloudStatus splitTest$readPrevCloudsType(LevelRenderer levelRenderer) {
        return LevelRendererFields.prevCloudsType();
    }

    @Redirect(method = "renderClouds", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;prevCloudsType:Lnet/minecraft/client/CloudStatus;", opcode = Opcodes.PUTFIELD))
    private void splitTest$writePrevCloudsType(LevelRenderer levelRenderer, @Nullable CloudStatus value) {
        LevelRendererFields.setPrevCloudsType(value);
    }
}
