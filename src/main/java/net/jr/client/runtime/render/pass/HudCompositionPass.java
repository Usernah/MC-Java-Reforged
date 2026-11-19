package net.jr.client.runtime.render.pass;

import com.mojang.blaze3d.textures.FilterMode;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.jr.api.client.local.Client;
import net.jr.api.client.render.gui.HudRenderContext;
import net.jr.client.ui.render.composition.GuiCompositionTarget;
import net.jr.client.ui.render.composition.UiFramebufferLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;


public final class HudCompositionPass {
    private static final float PIXEL_BIAS = 0.35F;
    private static final int MAIN_TARGET = 0;
    private static final int TAIL_TARGET = 1;
    private static final Map<Integer, GuiCompositionTarget[]> TARGETS = new HashMap<>();
    private static final ThreadLocal<ArrayDeque<Float>> ACTIVE_SCALES =
        ThreadLocal.withInitial(ArrayDeque::new);

    private HudCompositionPass() {
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
        UiFramebufferLayout layout =
            UiFramebufferLayout.resolve(parentContext.viewport());
        target(Client.slotId(), targetKind).extractCompositionAndSubmit(
            parentContext.graphics(),
            layout.canvasWidth(),
            layout.canvasHeight(),
            layout.rasterScale(),
            0.0F,
            0.0F,
            layout.scale(),
            layout.scale(),
            FilterMode.LINEAR,
            PIXEL_BIAS,
            layout.sampleFootprint(),
            layout.sampleFootprint(),
            Integer.MIN_VALUE,
            Integer.MIN_VALUE,
            0xFFFFFFFF,
            compositionGraphics -> {
                ArrayDeque<Float> scales = ACTIVE_SCALES.get();
                scales.push(layout.scale());
                try {
                    extractor.accept(
                        new CompositionContext(parentContext, compositionGraphics)
                    );
                } finally {
                    scales.pop();
                    if (scales.isEmpty()) {
                        ACTIVE_SCALES.remove();
                    }
                }
            }
        );
    }

    static float relativeScale(float absoluteScale) {
        return absoluteScale / Math.max(activeScale(), Float.MIN_NORMAL);
    }

    static float activeScale() {
        ArrayDeque<Float> scales = ACTIVE_SCALES.get();
        return scales.isEmpty() ? 1.0F : scales.peek();
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
