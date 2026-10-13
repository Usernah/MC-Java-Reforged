package net.jr.client.runtime.render.pass;

import com.mojang.blaze3d.textures.FilterMode;
import java.util.List;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.slot.ClientSlots;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.ui.hint.ControlHintContext;
import net.jr.client.ui.hint.ControlHintPipeline;
import net.jr.client.ui.hint.model.ResolvedControlHint;
import net.jr.client.ui.hint.render.ControlHintRenderer;
import net.jr.client.ui.render.composition.GuiCompositionTarget;
import net.jr.client.ui.presentation.UiPresentation;
import net.jr.client.ui.presentation.UiResolution;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public final class ControlHintRenderPass {
    private static final int INTERNAL_RASTER_SCALE = 1;
    private static final int HINT_FRAMEBUFFER_HEIGHT = 100;
    private static final float SPLIT_GUI_SCALE_REFERENCE = 2.0F;
    private static final float FULL_HD_GUI_SCALE_REFERENCE = 3.0F;
    private static final float PIXEL_BIAS = 0.35F;
    private static final float HUD_BOTTOM_SAFE_GAP = 4.0F;
    private static final int HUD_TARGET = 0;
    private static final int MENU_TARGET = 1;
    private static final GuiCompositionTarget[][] TARGETS =
        new GuiCompositionTarget[ClientSlots.MAX_SLOTS][2];

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
                GuiCompositionTarget target = TARGETS[slot][kind];
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
            float framebufferScale = framebufferScale();
            graphics.pose().pushMatrix();
            graphics.pose().scale(framebufferScale, framebufferScale);
            try {
                ControlHintRenderer.renderDirect(
                    result.context(),
                    graphics,
                    result.hints(),
                    graphics.guiWidth() / framebufferScale,
                    graphics.guiHeight() / framebufferScale
                );
            } finally {
                graphics.pose().popMatrix();
            }
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
        GuiCompositionTarget target = target(slotId, targetKind);
        target.extractCompositionAndSubmit(
            parentGraphics,
            layout.targetWidth(),
            layout.targetHeight(),
            INTERNAL_RASTER_SCALE,
            0.0F,
            layout.destinationY(),
            layout.scale(),
            layout.scale(),
            FilterMode.NEAREST,
            PIXEL_BIAS,
            layout.sampleFootprint(),
            layout.sampleFootprint(),
            compositionGraphics -> ControlHintRenderer.renderDirect(
                context,
                compositionGraphics,
                hints,
                layout.canvasWidth(),
                layout.canvasHeight()
            )
        );
    }

    private static GuiCompositionTarget target(int slotId, int kind) {
        GuiCompositionTarget target = TARGETS[slotId][kind];
        if (target == null) {
            target = new GuiCompositionTarget();
            TARGETS[slotId][kind] = target;
        }
        return target;
    }

    private static HintCompositionLayout resolveLayout(
        GuiGraphicsExtractor parentGraphics,
        ViewportArea viewport,
        int compositionHeight
    ) {
        float scale = framebufferScale(viewport);
        float canvasWidth = parentGraphics.guiWidth() / scale;
        float canvasHeight = compositionHeight;
        int targetWidth = Math.max(1, (int)Math.ceil(canvasWidth));
        int targetHeight = Math.max(1, compositionHeight);
        float destinationY = parentGraphics.guiHeight() - canvasHeight * scale;
        double effectiveGuiScale = viewport.effectiveGuiScale();
        if (!Double.isFinite(effectiveGuiScale) || effectiveGuiScale <= 0.0D) {
            effectiveGuiScale = Minecraft.getInstance().getWindow().getGuiScale();
        }
        float physicalDestinationScale = scale * (float)effectiveGuiScale;
        float sampleFootprint = physicalDestinationScale > 0.0F
            ? Math.max(1.0F, INTERNAL_RASTER_SCALE / physicalDestinationScale)
            : 1.0F;
        return new HintCompositionLayout(
            targetWidth,
            targetHeight,
            canvasWidth,
            canvasHeight,
            destinationY,
            scale,
            sampleFootprint
        );
    }

    private static float framebufferScale() {
        return UiPresentation.glyphResolution().framebufferScale();
    }

    private static float framebufferScale(ViewportArea viewport) {
        float scale = framebufferScale();
        if (!UiPresentation.isSplitScreen()) {
            return scale;
        }

        double effectiveGuiScale = viewport.effectiveGuiScale();
        if (!Double.isFinite(effectiveGuiScale) || effectiveGuiScale <= 0.0D) {
            return scale;
        }
        var window = Minecraft.getInstance().getWindow();
        float referenceScale = window.getWidth() >= UiResolution.FULL_HD.referenceWidth()
                && window.getHeight() >= UiResolution.FULL_HD.referenceHeight()
            ? FULL_HD_GUI_SCALE_REFERENCE
            : SPLIT_GUI_SCALE_REFERENCE;
        return scale * referenceScale / (float)effectiveGuiScale;
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
        float scale,
        float sampleFootprint
    ) {
    }
}
