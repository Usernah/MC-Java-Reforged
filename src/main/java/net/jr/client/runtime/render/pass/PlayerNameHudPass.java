package net.jr.client.runtime.render.pass;

import com.mojang.blaze3d.textures.FilterMode;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.jr.api.client.local.Client;
import net.jr.api.client.render.gui.HudRenderContext;
import net.jr.client.ui.render.composition.GuiCompositionTarget;
import net.jr.client.ui.render.composition.UiFramebufferLayout;
import net.jr.client.ui.render.composition.UiFramebufferPresentation;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class PlayerNameHudPass {
    private static final float PIXEL_BIAS = 0.35F;
    private static final int RASTER_SCALE = 3;
    private static final Map<Integer, GuiCompositionTarget> TARGETS = new HashMap<>();

    private PlayerNameHudPass() {
    }

    public static void extract(
        HudRenderContext parentContext,
        Consumer<HudRenderContext> extractor
    ) {
        UiFramebufferLayout layout = UiFramebufferLayout.resolve(
            parentContext.viewport(),
            0.75F
        );
        float sampleFootprint = UiFramebufferPresentation.sampleFootprint(
            parentContext.viewport(),
            layout.scale(),
            RASTER_SCALE
        );
        target(Client.slotId()).extractCompositionAndSubmit(
            parentContext.graphics(),
            layout.canvasWidth(),
            layout.canvasHeight(),
            RASTER_SCALE,
            0.0F,
            0.0F,
            layout.scale(),
            layout.scale(),
            FilterMode.LINEAR,
            PIXEL_BIAS,
            sampleFootprint,
            sampleFootprint,
            Integer.MIN_VALUE,
            Integer.MIN_VALUE,
            compositionGraphics -> extractor.accept(
                new CompositionContext(parentContext, compositionGraphics)
            )
        );
    }

    public static void closeAll() {
        for (GuiCompositionTarget target : TARGETS.values()) {
            target.close();
        }
        TARGETS.clear();
    }

    private static GuiCompositionTarget target(int slotId) {
        return TARGETS.computeIfAbsent(slotId, ignored -> new GuiCompositionTarget());
    }

    private record CompositionContext(
        HudRenderContext parent,
        GuiGraphicsExtractor graphics
    ) implements HudRenderContext {
        @Override
        public net.jr.client.runtime.slot.Slots slot() {
            return this.parent.slot();
        }

        @Override
        public net.jr.client.runtime.viewport.ViewportArea viewport() {
            return this.parent.viewport();
        }

        @Override
        public net.minecraft.client.DeltaTracker deltaTracker() {
            return this.parent.deltaTracker();
        }
    }
}
