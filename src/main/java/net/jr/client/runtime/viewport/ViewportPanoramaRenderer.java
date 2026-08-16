package net.jr.client.runtime.viewport;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.PanoramaRenderState;

/** Collects panorama requests whose destination is one local player's viewport. */
public final class ViewportPanoramaRenderer {
    private static final List<Request> PENDING = new ArrayList<>();
    private static boolean rendering;

    private ViewportPanoramaRenderer() {
    }

    public static void extract(GuiGraphicsExtractor graphics, int width, int height) {
        ViewportArea viewport = ViewportRenderScope.activeViewport();
        Minecraft minecraft = Minecraft.getInstance();
        GuiRenderState guiState = minecraft.gameRenderer.gameRenderState().guiRenderState;
        PanoramaRenderState previousGlobalPanorama = guiState.panoramaRenderState;

        minecraft.gameRenderer.panorama().extractRenderState(graphics, width, height);
        PanoramaRenderState viewportPanorama = guiState.panoramaRenderState;
        guiState.panoramaRenderState = previousGlobalPanorama;

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

    private record Request(ViewportArea viewport, PanoramaRenderState state) {
    }
}
