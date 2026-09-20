package net.jr.client.runtime.render.pass;

import com.mojang.blaze3d.textures.FilterMode;
import java.util.List;
import net.jr.api.client.split.SplitScreen;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.slot.LocalClientSlotRegistry;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.ui.hint.ControlHintContext;
import net.jr.client.ui.hint.ControlHintPipeline;
import net.jr.client.ui.hint.model.ResolvedControlHint;
import net.jr.client.ui.hint.render.ControlHintRenderer;
import net.jr.client.ui.layout.render.UILayoutRenderTarget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public final class ControlHintRenderPass {
    private static final FramebufferMetrics HD = new FramebufferMetrics(1280, 720);
    private static final FramebufferMetrics FULL_HD = new FramebufferMetrics(1920, 1080);
    private static final int INTERNAL_RASTER_SCALE = 1;
    private static final int HINT_FRAMEBUFFER_HEIGHT = 100;
    private static final float PIXEL_BIAS = 0.35F;
    private static final float HUD_BOTTOM_SAFE_GAP = 4.0F;
    private static final int HUD_TARGET = 0;
    private static final int MENU_TARGET = 1;
    private static final UILayoutRenderTarget[][] TARGETS =
        new UILayoutRenderTarget[LocalClientSlotRegistry.MAX_SLOTS][2];

    private ControlHintRenderPass() {
    }

    @Nullable
    public static PreparedHud prepareHud(GuiGraphicsExtractor graphics) {
        ControlHintPipeline.Result result = ControlHintPipeline.resolveHud();
        HintCompositionLayout layout = resolveLayout(graphics);
        if (layout == null) {
            return null;
        }

        float bottomInset =
            ControlHintRenderer.reservedScreenBottomHeight() * layout.scale()
                + HUD_BOTTOM_SAFE_GAP;
        return new PreparedHud(result, layout, bottomInset);
    }

    public static void renderHud(PreparedHud prepared, GuiGraphicsExtractor graphics) {
        ControlHintPipeline.Result result = prepared.result();
        if (result == null || result.hints().isEmpty()) {
            return;
        }
        renderComposed(
            result.context(),
            graphics,
            result.hints(),
            prepared.layout()
        );
    }

    public static void renderScreen(Screen screen, GuiGraphicsExtractor graphics) {
        renderResolved(ControlHintPipeline.resolveScreen(screen), graphics);
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

    private static void renderResolved(
        @Nullable ControlHintPipeline.Result result,
        GuiGraphicsExtractor graphics
    ) {
        if (result == null || result.hints().isEmpty()) {
            return;
        }
        HintCompositionLayout layout = resolveLayout(graphics);
        if (layout == null) {
            ControlHintRenderer.renderDirect(
                result.context(),
                graphics,
                result.hints(),
                graphics.guiWidth(),
                graphics.guiHeight()
            );
            return;
        }
        renderComposed(result.context(), graphics, result.hints(), layout);
    }

    @Nullable
    private static HintCompositionLayout resolveLayout(
        GuiGraphicsExtractor parentGraphics
    ) {
        Integer slotId = SlotScope.idOrNull();
        if (slotId == null || !ClientRuntime.INSTANCE.viewports().hasViewport(slotId)) {
            return null;
        }

        return resolveLayout(
            parentGraphics,
            ClientRuntime.INSTANCE.viewports().viewport(slotId),
            HINT_FRAMEBUFFER_HEIGHT
        );
    }

    private static void renderComposed(
        ControlHintContext context,
        GuiGraphicsExtractor parentGraphics,
        List<ResolvedControlHint> hints,
        HintCompositionLayout layout
    ) {
        int slotId = SlotScope.requireId();
        int targetKind = context.isHud() ? HUD_TARGET : MENU_TARGET;
        UILayoutRenderTarget target = target(slotId, targetKind);
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
        FramebufferMetrics metrics = SplitScreen.isActive() ? HD : FULL_HD;
        return switch (viewport.layout()) {
            case SINGLE -> new LogicalViewport(metrics.width(), metrics.height());
            case TWO_VERTICAL -> new LogicalViewport(metrics.width() / 2, metrics.height());
            case TWO_HORIZONTAL -> new LogicalViewport(metrics.width(), metrics.height() / 2);
            case FOUR_GRID -> new LogicalViewport(metrics.width() / 2, metrics.height() / 2);
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

    private record FramebufferMetrics(int width, int height) {
    }

    private record LogicalViewport(int width, int height) {
    }

    public record PreparedHud(
        @Nullable ControlHintPipeline.Result result,
        HintCompositionLayout layout,
        float bottomInset
    ) {
    }

    public record HintCompositionLayout(
        int targetWidth,
        int targetHeight,
        float canvasWidth,
        float canvasHeight,
        float destinationY,
        float scale
    ) {
    }
}
