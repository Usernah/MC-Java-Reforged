package net.jr.client.ui.hint.render;

import com.mojang.blaze3d.textures.FilterMode;
import java.util.List;

import net.jr.api.client.split.SplitScreen;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.ui.hint.ControlHintContext;
import net.jr.client.ui.hint.model.ResolvedControlHint;
import net.jr.client.ui.layout.render.UILayoutRenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Composes a complete hint strip before scaling it into its viewport. */
public final class ControlHintCompositor {


    public record FrameBufferMetrics (int width, int hegiht) {}

    private static final FrameBufferMetrics FB_HD = new FrameBufferMetrics(1280, 720);
    private static final FrameBufferMetrics FB_FHD = new FrameBufferMetrics(1920, 1080);

    private static FrameBufferMetrics activeMetrics() {
        // Aquí es donde el código decide cuál de tus dos moldes entregar
        return SplitScreen.isActive() ? FB_HD : FB_FHD;
    }


    private static final int INTERNAL_RASTER_SCALE = 1;
    private static final float PIXEL_BIAS = 0.35F;
    private static final int HUD_TARGET = 0;
    private static final int MENU_TARGET = 1;
    private static final UILayoutRenderTarget[][] TARGETS =
        new UILayoutRenderTarget[LocalClientSlotRegistry.MAX_SLOTS][2];

    private ControlHintCompositor() {
    }

    private static MetricsResolver resolveMetrics(boolean hd) {
        return hd
                ? MetricsResolver.HD
                : MetricsResolver.DEFAULT;
    }

    static boolean render(
        ControlHintContext context,
        GuiGraphicsExtractor parentGraphics,
        List<ResolvedControlHint> hints
    ) {
        Integer activeSlotId = SlotScope.idOrNull();
        if (activeSlotId == null || !ClientRuntime.INSTANCE.viewports().hasViewport(activeSlotId)) {
            return false;
        }

        HintCompositionLayout layout = resolveLayout(
            parentGraphics,
            ClientRuntime.INSTANCE.viewports().viewport(activeSlotId),
            ControlHintRenderer.compositionHeight(context, hints)
        );
        int targetKind = context.isHud() ? HUD_TARGET : MENU_TARGET;
        UILayoutRenderTarget target = target(activeSlotId, targetKind);
        target.extractCompositionAndSubmit(
            parentGraphics,
            layout.targetWidth(),
            layout.targetHeight(),
            INTERNAL_RASTER_SCALE,
            0.0F,
            layout.destinationY(),
            layout.scale(),
            layout.scale(),
            FilterMode.LINEAR,
            PIXEL_BIAS,
            compositionGraphics -> ControlHintRenderer.renderIcons(
                context,
                compositionGraphics,
                hints,
                layout.canvasWidth(),
                layout.canvasHeight()
            )
        );

        parentGraphics.pose().pushMatrix();
        parentGraphics.pose().translate(0.0F, layout.destinationY());
        parentGraphics.pose().scale(layout.scale(), layout.scale());
        try {
            ControlHintRenderer.renderText(
                context,
                parentGraphics,
                hints,
                layout.canvasWidth(),
                layout.canvasHeight()
            );
        } finally {
            parentGraphics.pose().popMatrix();
        }
        return true;
    }

    public static void closeAll() {
        for (int slot = 0; slot < TARGETS.length; slot++) {
            for (int kind = 0; kind < TARGETS[slot].length; kind++) {
                UILayoutRenderTarget target = TARGETS[slot][kind];
                if (target != null) {
                    target.close();
                    TARGETS[slot][kind] = null;
                }
            }
        }
    }

    private static UILayoutRenderTarget target(int slotId, int kind) {
        UILayoutRenderTarget target = TARGETS[slotId][kind];
        if (target == null) {
            target = new UILayoutRenderTarget();
            TARGETS[slotId][kind] = target;
        }
        return target;
    }

    private static LogicalViewport logicalViewport(ViewportArea viewport) {
        return switch (viewport.layout()) {
            case SINGLE -> new LogicalViewport(activeMetrics().width, activeMetrics().hegiht);
            case TWO_VERTICAL -> new LogicalViewport(activeMetrics().width / 2, activeMetrics().hegiht());
            case TWO_HORIZONTAL -> new LogicalViewport(activeMetrics().width, activeMetrics().hegiht / 2);
            case FOUR_GRID -> new LogicalViewport(activeMetrics().width / 2, activeMetrics().hegiht / 2);
        };
    }

    private static HintCompositionLayout resolveLayout(
        GuiGraphicsExtractor parentGraphics,
        ViewportArea viewport,
        int compositionHeight
    ) {
        LogicalViewport reference = logicalViewport(viewport);
        float scale = Math.min(
            parentGraphics.guiWidth() / (float)reference.width(),
            parentGraphics.guiHeight() / (float)reference.height()
        );
        if (!Float.isFinite(scale) || scale <= 0.0F) {
            scale = 1.0F;
        }

        float canvasWidth = parentGraphics.guiWidth() / scale;
        float canvasHeight = compositionHeight;
        int targetWidth = Math.max(1, (int)Math.ceil(canvasWidth));
        int targetHeight = Math.max(1, compositionHeight);
        float destinationY = parentGraphics.guiHeight() - canvasHeight * scale;
        return new HintCompositionLayout(
            targetWidth,
            targetHeight,
            canvasWidth,
            canvasHeight,
            destinationY,
            scale
        );
    }

    private record LogicalViewport(int width, int height) {
    }

    private record HintCompositionLayout(
        int targetWidth,
        int targetHeight,
        float canvasWidth,
        float canvasHeight,
        float destinationY,
        float scale
    ) {
    }
}
