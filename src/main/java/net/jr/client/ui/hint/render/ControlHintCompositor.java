package net.jr.client.ui.hint.render;

import com.mojang.blaze3d.textures.FilterMode;
import java.util.List;

import net.jr.api.client.split.SplitScreen;
import net.jr.client.runtime.context.LocalClientAcces;
import net.jr.client.runtime.context.LocalClient;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.ui.hint.ControlHintContext;
import net.jr.client.ui.hint.model.ResolvedControlHint;
import net.jr.client.ui.layout.render.UILayoutRenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Composes a complete hint strip before scaling it into its viewport. */
public final class ControlHintCompositor {
    private static final int BASE_WIDTH = 1920;
    private static final int BASE_HEIGHT = 1080;
    private static final int HD_BASE_WIDTH = 1280;
    private static final int HD_BASE_HEIGHT = 720;
    private static final int INTERNAL_RASTER_SCALE = 1;
    private static final float PIXEL_BIAS = 0.35F;
    private static final int HUD_TARGET = 0;
    private static final int MENU_TARGET = 1;
    private static final UILayoutRenderTarget[][] TARGETS =
        new UILayoutRenderTarget[LocalClientAcces.MAX_CLIENTS][2];

    private ControlHintCompositor() {
    }

    private static int referenceWidth() {
        return SplitScreen.isActive() ? HD_BASE_WIDTH : BASE_WIDTH;
    }

    private static int referenceHeight() {
        return SplitScreen.isActive() ? HD_BASE_HEIGHT : BASE_HEIGHT;
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
        LocalClient activeClient = LocalClientAcces.currentOrNull();
        if (activeClient == null) {
            return false;
        }

        HintCompositionLayout layout = resolveLayout(
            parentGraphics,
            activeClient.viewport(),
            ControlHintRenderer.compositionHeight(context, hints)
        );
        int targetKind = context.isHud() ? HUD_TARGET : MENU_TARGET;
        UILayoutRenderTarget target = target(activeClient.slotId(), targetKind);
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
            case SINGLE -> new LogicalViewport(referenceWidth(), referenceHeight());
            case TWO_VERTICAL -> new LogicalViewport(referenceWidth() / 2, referenceHeight());
            case TWO_HORIZONTAL -> new LogicalViewport(referenceWidth(), referenceHeight() / 2);
            case FOUR_GRID -> new LogicalViewport(referenceWidth() / 2, referenceHeight() / 2);
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
