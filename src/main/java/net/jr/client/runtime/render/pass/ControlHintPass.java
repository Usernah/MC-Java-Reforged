package net.jr.client.runtime.render.pass;

import com.mojang.blaze3d.textures.FilterMode;
import java.util.List;
import java.util.function.Consumer;
import net.jr.client.runtime.ClientRuntime;
import net.jr.api.client.local.Client;
import net.jr.client.runtime.context.SlotExecution;
import net.jr.client.runtime.slot.ClientSlots;
import net.jr.client.ui.hint.ControlHintContext;
import net.jr.client.ui.hint.ControlHintPipeline;
import net.jr.client.ui.hint.model.ResolvedControlHint;
import net.jr.client.ui.hint.render.ControlHintRenderer;
import net.jr.client.ui.presentation.UiPresentation;
import net.jr.client.ui.render.composition.GuiCompositionTarget;
import net.jr.client.ui.render.composition.HintFramebufferLayout;
import net.jr.client.ui.render.composition.UiFramebufferLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public final class ControlHintPass {
    private static final int HINT_FRAMEBUFFER_HEIGHT = 100;
    private static final float PIXEL_BIAS = 1.0F;
    private static final float HUD_BOTTOM_SAFE_GAP = 4.0F;
    private static final int HUD_TARGET = 0;
    private static final int MENU_TARGET = 1;
    private static final GuiCompositionTarget[][] TARGETS =
        new GuiCompositionTarget[ClientSlots.MAX_SLOTS][2];

    private ControlHintPass() {
    }

    @Nullable
    public static PreparedHud prepareHud(GuiGraphicsExtractor graphics) {
        if (!Client.current().options().showControlHints().get()) {
            return null;
        }
        ControlHintPipeline.Result result = ControlHintPipeline.resolveHud();
        HintFramebufferLayout layout = resolveLayout(graphics);
        if (layout == null) {
            return null;
        }

        int slotId = SlotExecution.currentSlotId();
        float hudScale = UiFramebufferLayout.resolve(
            ClientRuntime.INSTANCE.viewports().viewport(slotId)
        ).scale();
        float relativeScale = layout.scale()
            / Math.max(hudScale, Float.MIN_NORMAL);
        float bottomInset =
            ControlHintRenderer.reservedScreenBottomHeight() * relativeScale
                + HUD_BOTTOM_SAFE_GAP;
        return new PreparedHud(result, layout, bottomInset);
    }

    public static void renderHud(
        PreparedHud prepared,
        GuiGraphicsExtractor graphics,
        float opacity
    ) {
        ControlHintPipeline.Result result = prepared.result();
        if (result == null || result.hints().isEmpty() || opacity <= 0.0F) {
            return;
        }
        renderComposed(
            result.context(),
            graphics,
            result.hints(),
            prepared.layout(),
            premultipliedWhite(opacity)
        );
    }

    public static void renderScreen(Screen screen, GuiGraphicsExtractor graphics) {
        if (!Client.current().options().showControlHints().get()) {
            return;
        }
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
        HintFramebufferLayout layout = resolveLayout(graphics);
        if (layout == null) {
            float framebufferScale = UiPresentation.framebufferScale().value();
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
        renderComposed(
            result.context(),
            graphics,
            result.hints(),
            layout,
            0xFFFFFFFF
        );
    }

    @Nullable
    private static HintFramebufferLayout resolveLayout(
        GuiGraphicsExtractor parentGraphics
    ) {
        Integer slotId = SlotExecution.currentSlotIdOrNull();
        if (slotId == null || !ClientRuntime.INSTANCE.viewports().hasViewport(slotId)) {
            return null;
        }

        return HintFramebufferLayout.resolve(
            ClientRuntime.INSTANCE.viewports().viewport(slotId),
            parentGraphics.guiHeight(),
            1.0F,
            HINT_FRAMEBUFFER_HEIGHT
        );
    }

    private static void renderComposed(
        ControlHintContext context,
        GuiGraphicsExtractor parentGraphics,
        List<ResolvedControlHint> hints,
        HintFramebufferLayout layout,
        int compositeColor
    ) {
        int slotId = SlotExecution.currentSlotId();
        int targetKind = context.isHud() ? HUD_TARGET : MENU_TARGET;
        GuiCompositionTarget target = target(slotId, targetKind);
        Consumer<GuiGraphicsExtractor> extractor = compositionGraphics ->
            ControlHintRenderer.renderDirect(
                context,
                compositionGraphics,
                hints,
                layout.canvasWidth(),
                layout.canvasHeight()
            );
        target.extractCompositionAndSubmit(
            parentGraphics,
            layout.canvasWidth(),
            layout.canvasHeight(),
            layout.rasterScale(),
            0.0F,
            layout.destinationY(),
            layout.scale(),
            layout.scale(),
            FilterMode.LINEAR,
            PIXEL_BIAS,
            layout.sampleFootprint(),
            layout.sampleFootprint(),
            Integer.MIN_VALUE,
            Integer.MIN_VALUE,
            compositeColor,
            extractor
        );
    }

    private static int premultipliedWhite(float opacity) {
        int channel = Math.clamp(Math.round(opacity * 255.0F), 0, 255);
        return channel << 24 | channel << 16 | channel << 8 | channel;
    }

    private static GuiCompositionTarget target(int slotId, int kind) {
        GuiCompositionTarget target = TARGETS[slotId][kind];
        if (target == null) {
            target = new GuiCompositionTarget();
            TARGETS[slotId][kind] = target;
        }
        return target;
    }

    public record PreparedHud(
        @Nullable ControlHintPipeline.Result result,
        HintFramebufferLayout layout,
        float bottomInset
    ) {
    }
}
