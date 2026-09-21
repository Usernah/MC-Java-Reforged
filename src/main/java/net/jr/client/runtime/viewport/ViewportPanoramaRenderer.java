package net.jr.client.runtime.viewport;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.PanoramaRenderState;
import com.mojang.blaze3d.systems.RenderPass;

public final class ViewportPanoramaRenderer {
    private static final List<Request> PENDING = new ArrayList<>();
    private static boolean rendering;

    private ViewportPanoramaRenderer() {
    }

    public static void extract(GuiGraphicsExtractor graphics, int width, int height) {
        Minecraft.getInstance().gameRenderer.panorama().extractRenderState(graphics, width, height);
    }

    public static void captureForActiveViewport(Runnable extraction) {
        if (!ViewportRenderScope.hasActiveViewport()) {
            extraction.run();
            return;
        }

        ViewportArea viewport = ViewportRenderScope.activeViewport();
        GuiRenderState guiState = Minecraft.getInstance().gameRenderer.gameRenderState().guiRenderState;
        PanoramaRenderState previousGlobalPanorama = guiState.panoramaRenderState;
        PanoramaRenderState viewportPanorama;
        try {
            extraction.run();
            viewportPanorama = guiState.panoramaRenderState;
        } finally {
            guiState.panoramaRenderState = previousGlobalPanorama;
        }
        if (viewportPanorama != null) {
            PENDING.add(new Request(viewport, viewportPanorama));
        }
    }

    public static void renderPending(CubeMap cubeMap) {
        if (PENDING.isEmpty()) {
            return;
        }

        List<Request> requests = List.copyOf(PENDING);
        PENDING.clear();
        rendering = true;
        try {
            for (Request request : requests) {
                ViewportRenderScope.run(
                    request.viewport(),
                    () -> cubeMap.render(10.0F, request.state().spin())
                );
            }
        } finally {
            rendering = false;
        }
    }

    public static boolean isRendering() {
        return rendering;
    }

    public static RenderPass constrain(RenderPass renderPass) {
        if (!rendering || !ViewportRenderScope.hasActiveViewport()) {
            return renderPass;
        }
        ViewportArea viewport = ViewportRenderScope.activeViewport();
        renderPass.setViewport(viewport.glX(), viewport.glY(), viewport.glWidth(), viewport.glHeight());
        renderPass.enableScissor(viewport.glX(), viewport.glY(), viewport.glWidth(), viewport.glHeight());
        return renderPass;
    }

    private record Request(ViewportArea viewport, PanoramaRenderState state) {
    }
}
