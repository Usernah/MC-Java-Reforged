package net.jr.client.runtime.render.pass;

import com.mojang.blaze3d.textures.FilterMode;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.jr.api.client.local.Client;
import net.jr.api.client.render.gui.HudRenderContext;
import net.jr.client.ui.render.composition.GuiCompositionScope;
import net.jr.client.ui.render.composition.GuiCompositionTarget;
import net.minecraft.client.gui.GuiGraphicsExtractor;


public final class HudOpacityPass {
    private static final int MAIN_TARGET = 0;
    private static final int TAIL_TARGET = 1;
    private static final Map<Integer, GuiCompositionTarget[]> TARGETS = new HashMap<>();

    private HudOpacityPass() {
    }

    public static void extract(
        HudRenderContext parentContext,
        Consumer<HudRenderContext> extractor
    ) {
        extract(parentContext, MAIN_TARGET, extractor);
    }

    public static void extractTail(
        HudRenderContext parentContext,
        Consumer<HudRenderContext> extractor
    ) {
        extract(parentContext, TAIL_TARGET, extractor);
    }

    private static void extract(
        HudRenderContext parentContext,
        int targetKind,
        Consumer<HudRenderContext> extractor
    ) {
        double transparency = parentContext.slot()
            .optionsState()
            .hudTransparency()
            .get();
        float alpha = 1.0F - (float)Math.clamp(transparency, 0.0D, 1.0D);
        if (alpha <= 0.0F) {
            return;
        }
        if (alpha >= 1.0F) {
            extractor.accept(parentContext);
            return;
        }

        GuiGraphicsExtractor parentGraphics = parentContext.graphics();
        GuiCompositionScope.Extraction parentExtraction =
            GuiCompositionScope.activeExtractionOrNull();
        int rasterScale = parentExtraction == null
            ? Math.max(1, parentContext.viewport().guiScale())
            : parentExtraction.rasterScale();
        target(Client.slotId(), targetKind).extractCompositionAndSubmitBlit(
            parentGraphics,
            Math.max(1, parentGraphics.guiWidth()),
            Math.max(1, parentGraphics.guiHeight()),
            rasterScale,
            0.0F,
            0.0F,
            1.0F,
            1.0F,
            FilterMode.NEAREST,
            Integer.MIN_VALUE,
            Integer.MIN_VALUE,
            premultipliedWhite(alpha),
            compositionGraphics -> extractor.accept(
                new CompositionContext(parentContext, compositionGraphics)
            )
        );
    }

    public static void closeAll() {
        for (GuiCompositionTarget[] targets : TARGETS.values()) {
            for (GuiCompositionTarget target : targets) {
                if (target != null) {
                    target.close();
                }
            }
        }
        TARGETS.clear();
    }

    private static GuiCompositionTarget target(int slotId, int kind) {
        GuiCompositionTarget[] targets = TARGETS.computeIfAbsent(
            slotId,
            ignored -> new GuiCompositionTarget[2]
        );
        GuiCompositionTarget target = targets[kind];
        if (target == null) {
            target = new GuiCompositionTarget();
            targets[kind] = target;
        }
        return target;
    }

    private static int premultipliedWhite(float alpha) {
        int channel = Math.clamp(Math.round(alpha * 255.0F), 0, 255);
        return channel << 24 | channel << 16 | channel << 8 | channel;
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
