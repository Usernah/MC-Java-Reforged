package net.jr.client.runtime.render.pass;

import com.mojang.blaze3d.textures.FilterMode;
import java.util.List;
import net.jr.api.client.split.SplitScreen;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.slot.ClientSlots;
import net.jr.client.runtime.viewport.GuiScaleCalculator;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.runtime.viewport.ViewportLayout;
import net.jr.client.ui.hint.ControlHintContext;
import net.jr.client.ui.hint.ControlHintPipeline;
import net.jr.client.ui.hint.model.ResolvedControlHint;
import net.jr.client.ui.hint.render.ControlHintRenderer;
import net.jr.client.ui.layout.render.UILayoutRenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public final class ControlHintRenderPass {
    private static final float FULL_HD_FRAMEBUFFER_SCALE = 0.25F;
    private static final float HD_FRAMEBUFFER_SCALE = 0.5F;
    private static final int INTERNAL_RASTER_SCALE = 1;
    private static final int HINT_FRAMEBUFFER_HEIGHT = 100;
    private static final float PIXEL_BIAS = 0.35F;
    private static final float HUD_BOTTOM_SAFE_GAP = 4.0F;
    private static final int HUD_TARGET = 0;
    private static final int MENU_TARGET = 1;
    private static final UILayoutRenderTarget[][] TARGETS =
        new UILayoutRenderTarget[ClientSlots.MAX_SLOTS][2];

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
            float framebufferScale = framebufferScale(null, null);
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
            slotId,
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
            compositionGraphics -> ControlHintRenderer.renderDirect(
                context,
                compositionGraphics,
                hints,
                layout.canvasWidth(),
                layout.canvasHeight()
            )
        );
    }

    private static UILayoutRenderTarget target(int slotId, int kind) {
        UILayoutRenderTarget target = TARGETS[slotId][kind];
        if (target == null) {
            target = new UILayoutRenderTarget();
            TARGETS[slotId][kind] = target;
        }
        return target;
    }

    private static HintCompositionLayout resolveLayout(
        GuiGraphicsExtractor parentGraphics,
        ViewportArea viewport,
        int slotId,
        int compositionHeight
    ) {
        float scale = framebufferScale(viewport, slotId);
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

    private static float framebufferScale(
        @Nullable ViewportArea viewport,
        @Nullable Integer slotId
    ) {
        float scale = SplitScreen.isActive()
            ? HD_FRAMEBUFFER_SCALE
            : FULL_HD_FRAMEBUFFER_SCALE;
        if (
                viewport == null
                        || slotId == null
                        || (
                        viewport.layout() != ViewportLayout.FOUR_GRID
                                && viewport.layout() != ViewportLayout.TWO_HORIZONTAL
                )
                        || !ClientRuntime.INSTANCE.viewports().hasWindowMetrics()
        ) {
            return scale;
        }

        var window = ClientRuntime.INSTANCE.viewports().windowMetrics();
        int requestedScale = ClientRuntime.INSTANCE.slots()
            .slot(slotId)
            .optionsState()
            .requestedGuiScale();
        int verticalGuiScale = GuiScaleCalculator.resolve(
            Math.max(1, window.framebufferWidth() / 2),
            window.framebufferHeight(),
            requestedScale,
            Minecraft.getInstance().isEnforceUnicode()
        );
        double activeGuiScale = viewport.effectiveGuiScale();
        if (!Double.isFinite(activeGuiScale) || activeGuiScale <= 0.0D) {
            return scale;
        }
        return scale * (float)(verticalGuiScale / activeGuiScale);
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
