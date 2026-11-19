package net.jr.client.runtime.render.pass;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.jr.client.runtime.ClientRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.jspecify.annotations.Nullable;


public final class LegacyGammaFramePass {
    private static @Nullable TextureTarget presentationTarget;
    private static boolean renderFrameActive;
    private static boolean presentationReady;

    private LegacyGammaFramePass() {
    }

    public static void beginFrame() {
        renderFrameActive = true;
        presentationReady = false;
    }

    public static void endFrame() {
        renderFrameActive = false;
    }

    public static void process(GameRenderer gameRenderer) {
        RenderSystem.assertOnRenderThread();



        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isGameLoadFinished() || minecraft.gui.overlay() != null) {
            return;
        }

        RenderTarget backbuffer = gameRenderer.mainRenderTarget();
        TextureTarget presentation = ensurePresentationTarget(backbuffer.width, backbuffer.height);
        double strength = ClientRuntime.INSTANCE.slots().primary().optionsState().legacyGamma().get();
        presentationReady = LegacyGammaPass.process(0, backbuffer, presentation, strength);
    }

    public static @Nullable RenderTarget presentationTargetOrNull() {
        return !renderFrameActive && presentationReady ? presentationTarget : null;
    }

    public static RenderTarget lastPresentationTargetOr(RenderTarget fallback) {
        return presentationTarget == null ? fallback : presentationTarget;
    }

    public static void close() {
        RenderSystem.assertOnRenderThread();
        if (presentationTarget != null) {
            presentationTarget.destroyBuffers();
            presentationTarget = null;
        }
        LegacyGammaPass.close();
        renderFrameActive = false;
        presentationReady = false;
    }

    private static TextureTarget ensurePresentationTarget(int width, int height) {
        if (presentationTarget == null) {
            presentationTarget = new TextureTarget(
                "Java Reforged final presentation",
                width,
                height,
                false,
                GpuFormat.RGBA8_UNORM
            );
        } else if (presentationTarget.width != width || presentationTarget.height != height) {
            presentationTarget.resize(width, height);
        }
        return presentationTarget;
    }
}
