package net.alnv.javareforged.mixin.SSM;

import net.alnv.javareforged.ClientRuntime.runtime.*;
import net.alnv.javareforged.ClientRuntime.state.FovState;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.neoforged.neoforge.client.ClientHooks;
import org.joml.Matrix4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererSSMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private boolean panoramicMode;

    @Shadow
    private float zoom;

    @Shadow
    private float zoomX;

    @Shadow
    private float zoomY;

    @Shadow
    public abstract float getDepthFar();

    @Redirect(
        method = "renderLevel",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/GameRenderer;mainCamera:Lnet/minecraft/client/Camera;",
            opcode = Opcodes.GETFIELD
        )
    )
    private Camera splitTest$useSlotCamera(GameRenderer gameRenderer) {
        return Client.camera();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;tickFov()V"))
    private void splitTest$tickFovBySlot(GameRenderer gameRenderer) {
        Fovs.tickConnectedClients();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;tick()V"))
    private void splitTest$tickCamerasByClient(Camera ignoredVanillaCamera) {
        Cameras.tickConnectedClients();
    }

    @Inject(method = "getFov", at = @At("HEAD"), cancellable = true)
    private void splitTest$getSlotFov(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Double> cir) {
        if (this.panoramicMode) {
            cir.setReturnValue(90.0D);
            return;
        }

        double fov = 70.0D;
        if (useFovSetting) {
            FovState state = Client.render().fovState();
            fov = this.minecraft.options.fov().get().intValue();
            fov *= Mth.lerp(partialTick, state.oldModifier(), state.modifier());
        }

        if (camera.getEntity() instanceof LivingEntity livingEntity && livingEntity.isDeadOrDying()) {
            float deathTime = Math.min((float)livingEntity.deathTime + partialTick, 20.0F);
            fov /= (double)((1.0F - 500.0F / (deathTime + 500.0F)) * 2.0F + 1.0F);
        }

        FogType fogType = camera.getFluidInCamera();
        if (fogType == FogType.LAVA || fogType == FogType.WATER) {
            fov *= Mth.lerp(this.minecraft.options.fovEffectScale().get(), 1.0D, 0.85714287F);
        }

        cir.setReturnValue(ClientHooks.getFieldOfView((GameRenderer)(Object)this, camera, partialTick, fov, useFovSetting));
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;tick()V"))
    private void splitTest$tickHandsBySlot(ItemInHandRenderer renderer) {
        Hands.tickConnectedClients(renderer);
    }

    @Redirect(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V")
    )
    private void splitTest$renderHudBySlot(Gui gui, GuiGraphics graphics, DeltaTracker deltaTracker) {
        HudPass.render(gui, graphics, deltaTracker);
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/client/ClientHooks;drawScreen(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/gui/GuiGraphics;IIF)V"
            )
    )
    private void splitTest$drawScreenThroughSlots(
            Screen screen,
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        Screens.drawVanilla(
                screen,
                graphics,
                mouseX,
                mouseY,
                partialTick
        );
    }

    @Inject(method = "getProjectionMatrix", at = @At("HEAD"), cancellable = true)
    private void splitTest$useViewportProjection(double fovDegrees, CallbackInfoReturnable<Matrix4f> cir) {
        // Capturar el FOV para el slot activo
        Client.setFov((float) fovDegrees);

        Matrix4f matrix = ViewportPass.projectionMatrix(fovDegrees, this.zoom, this.zoomX, this.zoomY, this.getDepthFar());
        if (matrix != null) {
            cir.setReturnValue(matrix);
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;tickRain(Lnet/minecraft/client/Camera;)V"))
    private void splitTest$tickRainBySlot(LevelRenderer levelRenderer, Camera camera) {
        WeatherSounds.tickConnectedClients(levelRenderer);
    }

    @Redirect(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V")
    )
    private void splitTest$renderLevelForVisibleSlots(GameRenderer gameRenderer, DeltaTracker deltaTracker) {
        WorldPasses.renderLevelForVisibleSlots(gameRenderer, deltaTracker);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void splitTest$skipWorldTickWhenLevelIsGone(CallbackInfo ci) {
        ClientLevel slotLevel = Client.level();
        ClientLevel rendererLevel = ((LevelRendererSSAccessor)this.minecraft.levelRenderer).splitTest$getLevel();

        if (slotLevel == null || rendererLevel == null) {
            ci.cancel();
        }
    }

    @Inject(method = "getMainCamera", at = @At("HEAD"), cancellable = true)
    private void splitTest$getCurrentClientCamera(CallbackInfoReturnable<Camera> cir) {
        if (Client.currentOrNull() != null) {
            cir.setReturnValue(Client.camera());
        }
    }

    @Inject(method = "processBlurEffect", at = @At("HEAD"), cancellable = true)
    private void splitTest$processVanillaBackgroundBlurForCurrentClient(float partialTick, CallbackInfo ci) {
        if (VanillaBackgroundBlur.process(partialTick)) {
            ci.cancel();
        }
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void splitTest$closeVanillaBackgroundBlurChains(CallbackInfo ci) {
        VanillaBackgroundBlur.closeAll();
    }

}
