package net.jr.client.runtime.render.pass;

import com.mojang.blaze3d.textures.FilterMode;
import java.util.HashMap;
import java.util.Map;
import net.jr.api.client.local.Client;
import net.jr.api.client.render.gui.BottomHudRenderContext;
import net.jr.api.client.render.gui.GuiRenderObjects;
import net.jr.api.client.render.gui.HudRenderContext;
import net.jr.client.ui.render.composition.BottomHudFramebufferLayout;
import net.jr.client.ui.render.composition.GuiCompositionTarget;
import net.jr.client.ui.presentation.UiPresentation;
import net.jr.mixin.accessors.HudAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;


public final class BottomHudPass {
    private static final int BOTTOM_GUTTER = 8;
    private static final Map<Integer, GuiCompositionTarget> TARGETS = new HashMap<>();
    private static final ThreadLocal<Integer> SUPPRESSION_DEPTH =
            ThreadLocal.withInitial(() -> 0);

    private BottomHudPass() {
    }

    public static SuppressionScope suppressVanillaExtraction() {
        SUPPRESSION_DEPTH.set(SUPPRESSION_DEPTH.get() + 1);
        return new SuppressionScope();
    }

    public static boolean isVanillaExtractionSuppressed() {
        return SUPPRESSION_DEPTH.get() > 0;
    }

    public static void extract(Hud hud, HudRenderContext parentContext, float bottomInset) {
        int slotId = Client.slotId();
        int parentWidth = Math.max(1, parentContext.graphics().guiWidth());
        int parentHeight = Math.max(1, parentContext.graphics().guiHeight());
        BottomHudFramebufferLayout layout = BottomHudFramebufferLayout.resolve(
                parentContext.viewport(),
                parentWidth,
                parentHeight,
                HudCompositionPass.activeScale(),
                Client.current().options().bottomHudScaleMultiplier(UiPresentation.isSmall())
        );
        float safeInset = Float.isFinite(bottomInset)
                ? Math.clamp(bottomInset, 0.0F, parentHeight - 1.0F)
                : 0.0F;

        int logicalWidth = layout.canvasWidth();
        int logicalHeight = layout.canvasHeight();
        float destinationScale = layout.scale();
        int logicalBottomInset = Math.max(
                0,
                (int)Math.ceil(safeInset / Math.max(destinationScale, Float.MIN_NORMAL))
        );
        int contentBottom = Math.max(
                1,
                logicalHeight - logicalBottomInset - BOTTOM_GUTTER
        );

        target(slotId).extractCompositionAndSubmitBlit(
                parentContext.graphics(),
                logicalWidth,
                logicalHeight,
                layout.rasterScale(),
                0.0F,
                0.0F,
                destinationScale,
                destinationScale,
                FilterMode.NEAREST,
                Integer.MIN_VALUE,
                Integer.MIN_VALUE,
                0xFFFFFFFF,
                graphics -> extractContents(
                        hud,
                        parentContext,
                        graphics,
                        logicalHeight,
                        contentBottom
                )
        );

        // The compositor must finish before using the parent's GUI dimensions.
        // Move only the label's Y anchor; never scale its glyphs or backdrop.
        var gameMode = Client.current().gameMode();
        float vanillaBottomDistance = gameMode != null && !gameMode.canHurtPlayer()
                ? 45.0F  // 59 - 14 (creative)
                : 59.0F;
        float desiredY = (contentBottom - vanillaBottomDistance) * destinationScale;
        float untransformedY = parentHeight - vanillaBottomDistance;

        GuiGraphicsExtractor parentGraphics = parentContext.graphics();

        float textScale = UiPresentation.isSmall() ? 1.0F : 3.0F;
        float centerX = parentWidth / 2.0F;

        parentGraphics.pose().pushMatrix();
        try {
            parentGraphics.pose().translate(centerX, desiredY);
            parentGraphics.pose().scale(textScale, textScale);
            parentGraphics.pose().translate(-centerX, -untransformedY);

            ((HudAccessor) hud).javaReforged$maybeExtractSelectedItemName(
                    parentGraphics,
                    parentContext.deltaTracker()
            );
        } finally {
            parentGraphics.pose().popMatrix();
        }

    }

    private static void extractContents(
            Hud hud,
            HudRenderContext parentContext,
            GuiGraphicsExtractor graphics,
            int logicalHeight,
            int contentBottom
    ) {
        HudRenderContext hudContext = new CompositionContext(parentContext, graphics);

        graphics.pose().pushMatrix();
        graphics.pose().translate(0.0F, contentBottom - logicalHeight);
        try {
            HudAccessor accessor = (HudAccessor)hud;
            var deltaTracker = parentContext.deltaTracker();
            accessor.javaReforged$extractHotbar(graphics, deltaTracker);

            var gameMode = Client.current().gameMode();
            boolean survivalVisible = gameMode != null && gameMode.canHurtPlayer();
            if (survivalVisible) {
                accessor.javaReforged$extractHealthLevel(graphics);
                accessor.javaReforged$extractArmorLevel(graphics);
                accessor.javaReforged$extractFoodLevel(graphics);
            }
            accessor.javaReforged$extractVehicleHealth(graphics);
            if (survivalVisible) {
                accessor.javaReforged$extractAirLevel(graphics);
            }
            accessor.javaReforged$updateContextualBarRenderer();
            accessor.javaReforged$extractContextualInfoBarBackground(graphics, deltaTracker);
            accessor.javaReforged$extractExperienceLevel(graphics, deltaTracker);
            accessor.javaReforged$extractContextualInfoBar(graphics, deltaTracker);
            accessor.javaReforged$maybeExtractSpectatorTooltip(graphics, deltaTracker);

        } finally {
            graphics.pose().popMatrix();
        }

        BottomHudRenderContext bottomContext = new BottomHudRenderContext(
                parentContext.slot(),
                parentContext.viewport(),
                graphics,
                parentContext.deltaTracker(),
                contentBottom
        );
        GuiRenderObjects.renderBottomHud(bottomContext);
    }

    public static void closeAll() {
        TARGETS.values().forEach(GuiCompositionTarget::close);
        TARGETS.clear();
    }

    private static GuiCompositionTarget target(int slotId) {
        return TARGETS.computeIfAbsent(slotId, ignored -> new GuiCompositionTarget());
    }

    public static final class SuppressionScope implements AutoCloseable {
        private boolean closed;

        private SuppressionScope() {
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            int depth = SUPPRESSION_DEPTH.get() - 1;
            if (depth <= 0) {
                SUPPRESSION_DEPTH.remove();
            } else {
                SUPPRESSION_DEPTH.set(depth);
            }
            this.closed = true;
        }
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
