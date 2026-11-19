package net.jr.api.client.systemui;

import java.util.Objects;
import net.jr.api.client.render.Draw;
import net.jr.registry.ModFonts;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.jr.client.systemui.SystemUiSounds;

public abstract class AbstractSystemPanel implements SystemOverlay {
    private static final long DEFAULT_OPEN_DURATION_MS = 400L;
    private static final float INITIAL_SCALE = 0.02F;
    private static final int DEFAULT_BACKDROP_COLOR = 0xB3000000;
    private static final int TITLE_X = 30;
    private static final int TITLE_Y = 40;
    private static final int TITLE_COLOR = 0xFFE4E4E4;
    private static final float TITLE_SCALE = 1.2F;

    private final Component title;
    private final int width;
    private final int height;
    private final long openDurationMs;
    private long openedAtMs;
    private long closingAtMs;
    private float closingStartProgress;
    private SystemUiHandle handle;

    protected AbstractSystemPanel(Component title, int width, int height) {
        this(title, width, height, DEFAULT_OPEN_DURATION_MS);
    }

    protected AbstractSystemPanel(
        Component title,
        int width,
        int height,
        long openDurationMs
    ) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("System panel size must be positive");
        }
        if (openDurationMs < 0L) {
            throw new IllegalArgumentException("System panel animation duration cannot be negative");
        }
        this.title = Objects.requireNonNull(title, "title");
        this.width = width;
        this.height = height;
        this.openDurationMs = openDurationMs;
    }

    @Override
    public void opened(SystemUiHandle handle) {
        this.handle = handle;
        this.openedAtMs = Util.getMillis();
        this.closingAtMs = 0L;
        this.closingStartProgress = 0.0F;
        SystemUiSounds.playOpen();
    }

    @Override
    public void closing(SystemUiCloseReason reason) {
        if (this.closingAtMs != 0L) {
            return;
        }
        this.closingStartProgress = this.openProgress();
        this.closingAtMs = Util.getMillis();
        SystemUiSounds.playClose();
    }

    @Override
    public boolean closeAnimationFinished() {
        return this.closingAtMs != 0L && this.animationProgress() <= 0.0F;
    }

    @Override
    public final void render(SystemUiRenderContext context) {
        this.renderBackdrop(context, DEFAULT_BACKDROP_COLOR);

        int x = (context.canvasWidth() - this.width) / 2;
        int y = (context.canvasHeight() - this.height) / 2;
        Bounds bounds = new Bounds(x, y, this.width, this.height);
        float progress = this.animationProgress();
        float scale = INITIAL_SCALE + (1.0F - INITIAL_SCALE) * easeOutCubic(progress);
        float centerX = x + this.width / 2.0F;
        float centerY = y + this.height / 2.0F;

        context.graphics().pose().pushMatrix();
        context.graphics().pose().translate(centerX, centerY);
        context.graphics().pose().scale(scale, scale);
        context.graphics().pose().translate(-centerX, -centerY);
        try {
            this.renderPanel(context, bounds);
            if (progress >= 1.0F) {
                this.renderTitle(context, bounds);
                this.renderContent(context, bounds);
            }
        } finally {
            context.graphics().pose().popMatrix();
        }
    }

    protected void renderBackdrop(SystemUiRenderContext context, int color) {
        context.graphics().fill(0, 0, context.canvasWidth(), context.canvasHeight(), color);
    }

    protected abstract void renderPanel(SystemUiRenderContext context, Bounds bounds);

    protected void renderTitle(SystemUiRenderContext context, Bounds bounds) {
        Draw.text(this.title, bounds.x() + TITLE_X, bounds.y() + TITLE_Y)
            .font(ModFonts.Montserrat_bold)
            .scale(TITLE_SCALE)
            .color(TITLE_COLOR)
            .shadow(false)
            .draw(context.graphics());
    }

    protected abstract void renderContent(SystemUiRenderContext context, Bounds bounds);

    @Override
    public final void mouseMoved(double x, double y) {
        if (!this.isOpening()) {
            this.onMouseMoved(x, y);
        }
    }

    @Override
    public final void mouseButton(double x, double y, int button, int action) {
        if (!this.isOpening()) {
            this.onMouseButton(x, y, button, action);
        }
    }

    @Override
    public final void mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (!this.isOpening()) {
            this.onMouseScrolled(x, y, horizontal, vertical);
        }
    }

    @Override
    public final void keyEvent(int action, KeyEvent event) {
        if (!this.isOpening()) {
            this.onKeyEvent(action, event);
        }
    }

    @Override
    public final void charTyped(CharacterEvent event) {
        if (!this.isOpening()) {
            this.onCharTyped(event);
        }
    }

    protected void onMouseMoved(double x, double y) {
    }

    protected void onMouseButton(double x, double y, int button, int action) {
    }

    protected void onMouseScrolled(double x, double y, double horizontal, double vertical) {
    }

    protected void onKeyEvent(int action, KeyEvent event) {
    }

    protected void onCharTyped(CharacterEvent event) {
    }

    protected final boolean isOpening() {
        return this.closingAtMs == 0L && this.openProgress() < 1.0F;
    }

    protected final SystemUiHandle handle() {
        if (this.handle == null) {
            throw new IllegalStateException("System menu has not been opened");
        }
        return this.handle;
    }

    protected final void back() {
        this.handle().back();
    }

    protected final float openProgress() {
        if (this.openDurationMs == 0L || this.openedAtMs == 0L) {
            return 1.0F;
        }
        return Math.clamp(
            (Util.getMillis() - this.openedAtMs) / (float)this.openDurationMs,
            0.0F,
            1.0F
        );
    }

    private float animationProgress() {
        if (this.closingAtMs == 0L) {
            return this.openProgress();
        }
        if (this.openDurationMs == 0L || this.closingStartProgress <= 0.0F) {
            return 0.0F;
        }
        long duration = Math.max(1L, Math.round(this.openDurationMs * this.closingStartProgress));
        float closeProgress = Math.clamp(
            (Util.getMillis() - this.closingAtMs) / (float)duration,
            0.0F,
            1.0F
        );
        return this.closingStartProgress * (1.0F - closeProgress);
    }

    private static float easeOutCubic(float value) {
        float inverse = 1.0F - value;
        return 1.0F - inverse * inverse * inverse;
    }

    public record Bounds(int x, int y, int width, int height) {
        public int right() {
            return this.x + this.width;
        }

        public int bottom() {
            return this.y + this.height;
        }
    }
}
