package net.jr.client.ui.layout;

import net.jr.client.ui.layout.render.UILayoutRenderTarget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

public final class UILayoutRuntime implements AutoCloseable {
    private static final int OVERLAY_DIM_COLOR = 0x99000000;

    private final Screen screen;
    private final IntSupplier widthSupplier;
    private final IntSupplier heightSupplier;
    private final Consumer<UILayout> interactiveLayoutListener;
    private final Map<UILayout, UILayoutRenderTarget> renderTargets = new IdentityHashMap<>();

    private UILayout currentLayout;
    private UIOverlay activeOverlay;
    private Transition transition;

    public UILayoutRuntime(
        Screen screen,
        IntSupplier widthSupplier,
        IntSupplier heightSupplier,
        Consumer<UILayout> interactiveLayoutListener
    ) {
        this.screen = screen;
        this.widthSupplier = widthSupplier;
        this.heightSupplier = heightSupplier;
        this.interactiveLayoutListener = interactiveLayoutListener;
    }

    public void setInitialLayout(UILayout layout) {
        if (this.currentLayout != null) {
            throw new IllegalStateException("The initial layout has already been assigned");
        }
        this.prepare(layout);
        this.currentLayout = layout;
        layout.onOpen();
        this.begin(Phase.ENTER, null, layout, layout.transitionType(), layout.transitionDuration());
    }

    @Nullable
    public UILayout currentLayout() {
        return this.currentLayout;
    }

    @Nullable
    public UIOverlay activeOverlay() {
        return this.activeOverlay;
    }

    @Nullable
    public UILayout interactiveLayout() {
        if (this.transition != null) {
            return null;
        }
        return this.activeOverlay != null ? this.activeOverlay : this.currentLayout;
    }

    public boolean isTransitioning() {
        return this.transition != null;
    }

    public void resize() {
        int width = this.widthSupplier.getAsInt();
        int height = this.heightSupplier.getAsInt();
        for (UILayout layout : this.liveLayouts()) {
            layout.resizeIfNeeded(width, height);
        }
    }

    public void tick() {
        this.completeTransitionIfNeeded();
        for (UILayout layout : this.liveLayouts()) {
            layout.tick();
        }
    }

    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.resize();
        float progress = this.transitionProgress();
        this.completeTransitionIfNeeded();

        Transition active = this.transition;
        if (active == null) {
            this.submit(graphics, this.currentLayout, mouseX, mouseY, partialTick, Composite.full());
            if (this.activeOverlay != null) {
                graphics.fill(0, 0, this.widthSupplier.getAsInt(), this.heightSupplier.getAsInt(), OVERLAY_DIM_COLOR);
                this.submit(graphics, this.activeOverlay, mouseX, mouseY, partialTick, Composite.full());
            }
            return;
        }

        switch (active.phase()) {
            case ENTER -> this.submit(
                graphics,
                active.to(),
                mouseX,
                mouseY,
                partialTick,
                composite(active.to(), active.type(), progress, true)
            );
            case SWITCH -> {
                this.submit(
                    graphics,
                    active.from(),
                    mouseX,
                    mouseY,
                    partialTick,
                    composite(active.from(), active.type(), 1.0F - progress, false)
                );
                this.submit(
                    graphics,
                    active.to(),
                    mouseX,
                    mouseY,
                    partialTick,
                    composite(active.to(), active.type(), progress, true)
                );
            }
            case OVERLAY_OPEN -> {
                this.submit(graphics, this.currentLayout, mouseX, mouseY, partialTick, Composite.full());
                this.extractOverlayDim(graphics, progress);
                this.submit(
                    graphics,
                    active.to(),
                    mouseX,
                    mouseY,
                    partialTick,
                    composite(active.to(), active.type(), progress, true)
                );
            }
            case OVERLAY_CLOSE -> {
                this.submit(graphics, this.currentLayout, mouseX, mouseY, partialTick, Composite.full());
                this.extractOverlayDim(graphics, 1.0F - progress);
                this.submit(
                    graphics,
                    active.from(),
                    mouseX,
                    mouseY,
                    partialTick,
                    composite(active.from(), active.type(), 1.0F - progress, false)
                );
            }
        }
    }

    public void switchLayout(UILayout target, TransitionType type, int durationMs) {
        if (target == null || target == this.currentLayout || this.transition != null) {
            return;
        }
        if (this.activeOverlay != null) {
            this.closeOverlayImmediately();
        }
        this.prepare(target);
        target.onOpen();
        this.begin(Phase.SWITCH, this.currentLayout, target, type, durationMs);
    }

    public void showOverlay(UIOverlay overlay) {
        if (overlay == null || this.transition != null) {
            return;
        }
        if (this.activeOverlay != null) {
            this.closeOverlayImmediately();
        }
        this.prepare(overlay);
        this.activeOverlay = overlay;
        overlay.onOpen();
        this.begin(Phase.OVERLAY_OPEN, null, overlay, overlay.transitionType(), overlay.transitionDuration());
    }

    public void closeOverlay() {
        if (this.activeOverlay == null || this.transition != null) {
            return;
        }
        UIOverlay closing = this.activeOverlay;
        this.begin(Phase.OVERLAY_CLOSE, closing, null, closing.transitionType(), closing.transitionDuration());
    }

    @Override
    public void close() {
        Set<UILayout> closingLayouts = this.liveLayouts();
        this.transition = null;
        if (this.activeOverlay != null) {
            this.activeOverlay.onClose();
        }
        if (this.currentLayout != null) {
            this.currentLayout.onClose();
        }
        for (UILayoutRenderTarget target : this.renderTargets.values()) {
            target.close();
        }
        for (UILayout layout : closingLayouts) {
            layout.disposeUiContent();
        }
        this.renderTargets.clear();
        this.activeOverlay = null;
        this.currentLayout = null;
        this.notifyInteractiveLayoutChanged();
    }

    private void submit(
        GuiGraphicsExtractor graphics,
        @Nullable UILayout layout,
        int mouseX,
        int mouseY,
        float partialTick,
        Composite composite
    ) {
        if (layout == null || composite.alpha() <= 0.0F) {
            return;
        }
        this.renderTargets
            .computeIfAbsent(layout, ignored -> new UILayoutRenderTarget())
            .extractAndSubmit(graphics, layout, mouseX, mouseY, partialTick, composite.alpha(), composite.offsetX(), composite.offsetY());
    }

    private void prepare(UILayout layout) {
        layout.initialize(
            this.screen,
            this,
            this.widthSupplier.getAsInt(),
            this.heightSupplier.getAsInt()
        );
    }

    private void begin(
        Phase phase,
        @Nullable UILayout from,
        @Nullable UILayout to,
        TransitionType type,
        int durationMs
    ) {
        TransitionType resolvedType = type == null ? TransitionType.NONE : type;
        int resolvedDuration = Math.max(0, durationMs);
        if (resolvedType == TransitionType.NONE || resolvedDuration == 0) {
            this.transition = new Transition(phase, from, to, resolvedType, 0, Util.getMillis());
            this.finishTransition();
            return;
        }
        this.transition = new Transition(phase, from, to, resolvedType, resolvedDuration, Util.getMillis());
        this.notifyInteractiveLayoutChanged();
    }

    private float transitionProgress() {
        if (this.transition == null || this.transition.durationMs() <= 0) {
            return 1.0F;
        }
        return Mth.clamp(
            (float)(Util.getMillis() - this.transition.startedAtMs()) / this.transition.durationMs(),
            0.0F,
            1.0F
        );
    }

    private void completeTransitionIfNeeded() {
        if (this.transition != null && this.transitionProgress() >= 1.0F) {
            this.finishTransition();
        }
    }

    private void finishTransition() {
        Transition completed = this.transition;
        if (completed == null) {
            return;
        }
        this.transition = null;

        switch (completed.phase()) {
            case ENTER, OVERLAY_OPEN -> {
            }
            case SWITCH -> {
                UILayout previous = completed.from();
                if (previous != null) {
                    previous.onClose();
                    this.release(previous);
                }
                this.currentLayout = completed.to();
            }
            case OVERLAY_CLOSE -> this.closeOverlayImmediately();
        }
        this.notifyInteractiveLayoutChanged();
    }

    private void closeOverlayImmediately() {
        UIOverlay previous = this.activeOverlay;
        this.activeOverlay = null;
        if (previous != null) {
            previous.onClose();
            this.release(previous);
        }
    }

    private void release(UILayout layout) {
        layout.disposeUiContent();
        UILayoutRenderTarget target = this.renderTargets.remove(layout);
        if (target != null) {
            target.close();
        }
    }

    private Set<UILayout> liveLayouts() {
        Set<UILayout> layouts = new LinkedHashSet<>();
        if (this.currentLayout != null) {
            layouts.add(this.currentLayout);
        }
        if (this.activeOverlay != null) {
            layouts.add(this.activeOverlay);
        }
        if (this.transition != null) {
            if (this.transition.from() != null) {
                layouts.add(this.transition.from());
            }
            if (this.transition.to() != null) {
                layouts.add(this.transition.to());
            }
        }
        return layouts;
    }

    private void notifyInteractiveLayoutChanged() {
        this.interactiveLayoutListener.accept(this.interactiveLayout());
    }

    private void extractOverlayDim(GuiGraphicsExtractor graphics, float progress) {
        int alpha = Math.round(0x99 * smooth(progress));
        if (alpha > 0) {
            graphics.fill(
                0,
                0,
                this.widthSupplier.getAsInt(),
                this.heightSupplier.getAsInt(),
                alpha << 24
            );
        }
    }

    private static Composite composite(UILayout layout, TransitionType type, float progress, boolean entering) {
        float eased = smooth(progress);
        if (type == TransitionType.NONE) {
            return Composite.full();
        }
        if (type == TransitionType.FADE) {
            return new Composite(eased, 0.0F, 0.0F);
        }

        float movement = layout == null ? 0.0F : layout.slideDistance() * (1.0F - eased);
        float direction = entering ? 1.0F : -1.0F;
        return switch (type) {
            case SLIDE_LEFT -> new Composite(1.0F, movement * direction, 0.0F);
            case SLIDE_RIGHT -> new Composite(1.0F, -movement * direction, 0.0F);
            case SLIDE_UP -> new Composite(1.0F, 0.0F, movement * direction);
            case SLIDE_DOWN -> new Composite(1.0F, 0.0F, -movement * direction);
            case NONE, FADE -> Composite.full();
        };
    }

    private static float smooth(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return 1.0F - (float)Math.pow(1.0F - t, 3.0D);
    }

    private enum Phase {
        ENTER,
        SWITCH,
        OVERLAY_OPEN,
        OVERLAY_CLOSE
    }

    private record Transition(
        Phase phase,
        @Nullable UILayout from,
        @Nullable UILayout to,
        TransitionType type,
        int durationMs,
        long startedAtMs
    ) {
    }

    private record Composite(float alpha, float offsetX, float offsetY) {
        private static Composite full() {
            return new Composite(1.0F, 0.0F, 0.0F);
        }
    }
}
