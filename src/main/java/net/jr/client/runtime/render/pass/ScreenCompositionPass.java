package net.jr.client.runtime.render.pass;

import com.mojang.blaze3d.textures.FilterMode;
import net.jr.api.client.render.gui.GuiRenderContext;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.slot.ClientSlots;
import net.jr.client.ui.render.composition.GuiCompositionTarget;
import net.jr.client.ui.dsl.ScreenEditorHooks;
import net.jr.client.ui.render.composition.ScreenFramebufferSpace;
import net.jr.client.ui.render.composition.UiFramebufferLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;


public final class ScreenCompositionPass {
    private static final float PIXEL_BIAS = 2.35F;
    private static final GuiCompositionTarget[] TARGETS =
        new GuiCompositionTarget[ClientSlots.MAX_SLOTS];

    private ScreenCompositionPass() {
    }

    public static Screen extract(
        Minecraft minecraft,
        Screen screen,
        GuiRenderContext parentContext,
        float partialTick
    ) {
        int slotId = SlotScope.requireId();
        UiFramebufferLayout layout =
            ScreenFramebufferSpace.layout(parentContext.viewport());
        int mouseX = ScreenPass.mouseX(
            minecraft, slotId, parentContext.viewport(), screen
        );
        int mouseY = ScreenPass.mouseY(
            minecraft, slotId, parentContext.viewport(), screen
        );

        target(slotId).extractCompositionAndSubmit(
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
            mouseX,
            mouseY,
            compositionGraphics -> {
                GuiRenderContext compositionContext = new CompositionContext(
                    parentContext, compositionGraphics
                );
                ScreenEditorHooks.before(screen, compositionGraphics);
                ScreenPass.extractKnown(
                    minecraft,
                    screen,
                    compositionGraphics,
                    mouseX,
                    mouseY,
                    partialTick
                );
                net.jr.api.client.render.gui.GuiRenderObjects.render(
                    net.jr.api.client.render.gui.GuiRenderPhase.SCREEN,
                    compositionContext
                );
                ScreenOverlayRenderPass.extract(minecraft, screen, compositionContext);
                ScreenEditorHooks.after(screen, compositionGraphics);
            }
        );
        return screen;
    }

    public static void closeAll() {
        for (int slotId = 0; slotId < TARGETS.length; slotId++) {
            GuiCompositionTarget target = TARGETS[slotId];
            if (target != null) {
                target.close();
                TARGETS[slotId] = null;
            }
        }
    }

    private static GuiCompositionTarget target(int slotId) {
        GuiCompositionTarget target = TARGETS[slotId];
        if (target == null) {
            target = new GuiCompositionTarget();
            TARGETS[slotId] = target;
        }
        return target;
    }

    private record CompositionContext(
        GuiRenderContext parent,
        GuiGraphicsExtractor graphics
    ) implements GuiRenderContext {
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
