package net.jr.client.runtime.render.pass;

import com.mojang.blaze3d.textures.FilterMode;
import java.util.HashMap;
import java.util.Map;
import net.jr.api.client.render.gui.GlobalRenderContext;
import net.jr.api.client.systemui.SystemUI;
import net.jr.api.client.systemui.SystemUiRenderContext;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.runtime.viewport.ViewportGuiScale;
import net.jr.client.systemui.SystemUiManager;
import net.jr.client.ui.render.composition.GuiCompositionTarget;
import net.minecraft.client.Minecraft;

public final class SystemUiRenderPass {
    private static final int FULL_WINDOW_TARGET = -1;
    private static final Map<Integer, GuiCompositionTarget> TARGETS = new HashMap<>();

    private SystemUiRenderPass() {
    }

    public static void extract(GlobalRenderContext context) {
        SystemUiManager manager = SystemUiManager.get();
        manager.updateClosures();
        for (var entry : manager.viewportOverlays().entrySet()) {
            int slotId = entry.getKey();
            if (!ClientRuntime.INSTANCE.viewports().hasViewport(slotId)) {
                continue;
            }
            ViewportArea viewport = ClientRuntime.INSTANCE.viewports().viewport(slotId);
            float sharedGuiScale = (float)Minecraft.getInstance().getWindow().getGuiScale();
            float destinationX = viewport.x() / sharedGuiScale;
            float destinationY = viewport.y() / sharedGuiScale;
            float localToShared = (float)(viewport.effectiveGuiScale() / sharedGuiScale);
            float screenScale = ViewportGuiScale.screenScale(viewport);
            float framebufferScale = SystemUI.framebufferScale(
                viewport.layout().viewportCount() > 1
            );
            int localWidth = ViewportGuiScale.screenLogicalWidth(viewport);
            int localHeight = ViewportGuiScale.screenLogicalHeight(viewport);
            int scissorLeft = (int)Math.floor(destinationX);
            int scissorTop = (int)Math.floor(destinationY);
            int scissorRight = (int)Math.ceil(
                destinationX + viewport.width() / sharedGuiScale
            );
            int scissorBottom = (int)Math.ceil(
                destinationY + viewport.height() / sharedGuiScale
            );
            context.graphics().enableScissor(
                scissorLeft,
                scissorTop,
                scissorRight,
                scissorBottom
            );
            try {
                submit(
                    context,
                    entry.getValue(),
                    target(slotId),
                    canvasSize(localWidth, framebufferScale),
                    canvasSize(localHeight, framebufferScale),
                    destinationX,
                    destinationY,
                    framebufferScale * localToShared * screenScale
                );
            } finally {
                context.graphics().disableScissor();
            }
        }

        SystemUiManager.ActiveOverlay fullWindow = manager.fullWindowOverlay();
        if (fullWindow != null) {
            submit(
                context,
                fullWindow,
                target(FULL_WINDOW_TARGET),
                canvasSize(context.guiWidth(), SystemUI.FRAMEBUFFER_SCALE),
                canvasSize(context.guiHeight(), SystemUI.FRAMEBUFFER_SCALE),
                0.0F,
                0.0F,
                SystemUI.FRAMEBUFFER_SCALE
            );
        }
    }

    private static void submit(
        GlobalRenderContext parent,
        SystemUiManager.ActiveOverlay active,
        GuiCompositionTarget target,
        int canvasWidth,
        int canvasHeight,
        float destinationX,
        float destinationY,
        float destinationScale
    ) {
        target.extractCompositionAndSubmitBlit(
            parent.graphics(),
            canvasWidth,
            canvasHeight,
            SystemUI.FRAMEBUFFER_RASTER_SCALE,
            destinationX,
            destinationY,
            destinationScale,
            destinationScale,
            FilterMode.LINEAR,
            Integer.MIN_VALUE,
            Integer.MIN_VALUE,
            0xFFFFFFFF,
            graphics -> active.overlay().render(new SystemUiRenderContext(
                graphics,
                parent.deltaTracker(),
                active.handle().target(),
                canvasWidth,
                canvasHeight
            ))
        );
    }

    private static int canvasSize(int destinationSize, float framebufferScale) {
        return Math.max(1, (int)Math.ceil(destinationSize / framebufferScale));
    }

    private static GuiCompositionTarget target(int id) {
        return TARGETS.computeIfAbsent(id, ignored -> new GuiCompositionTarget());
    }

    public static void closeAll() {
        for (GuiCompositionTarget target : TARGETS.values()) {
            target.close();
        }
        TARGETS.clear();
        SystemUiManager.get().clear();
    }
}
