package net.jr.client.ui.overlay;

import org.jetbrains.annotations.Nullable;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

/** Owns the single modal/non-modal screen overlay for one local client slot. */
public final class ScreenOverlayManager {
    private @Nullable ActiveOverlay active;

    public boolean open(Screen ownerScreen, VanillaScreenOverlay overlay) {
        if (ownerScreen == null || overlay == null) {
            return false;
        }
        this.close(ScreenOverlayCloseReason.REPLACED);
        ScreenOverlayContext context = new ScreenOverlayContext(ownerScreen, this);
        this.active = new ActiveOverlay(ownerScreen, overlay, context);
        overlay.opened(context);
        return this.active != null && this.active.overlay == overlay;
    }

    public boolean isShowing(VanillaScreenOverlay overlay) {
        return this.active != null && this.active.overlay == overlay;
    }

    public boolean hasOverlay() {
        return this.active != null;
    }

    public boolean ownsInput(Screen screen) {
        return this.ensureOwner(screen) && this.active.overlay.blocksScreenInput();
    }

    public boolean tick(Screen screen) {
        if (!this.ensureOwner(screen)) {
            return false;
        }
        ActiveOverlay current = this.active;
        boolean blocksInput = current.overlay.blocksScreenInput();
        current.overlay.tick(current.context);
        return blocksInput;
    }

    public boolean render(
        Screen screen,
        GuiGraphicsExtractor graphics,
        int width,
        int height,
        float partialTick
    ) {
        if (!this.ensureOwner(screen)) {
            return false;
        }
        ActiveOverlay current = this.active;
        boolean hidesCursor = current.overlay.hidesCursor();
        current.overlay.render(graphics, width, height, partialTick);
        return hidesCursor;
    }

    public void close(ScreenOverlayCloseReason reason) {
        ActiveOverlay closing = this.active;
        if (closing == null) {
            return;
        }
        this.active = null;
        closing.overlay.closed(reason == null ? ScreenOverlayCloseReason.CANCELLED : reason);
    }

    public void clear() {
        this.close(ScreenOverlayCloseReason.SLOT_CLEARED);
    }

    private boolean ensureOwner(Screen screen) {
        if (this.active == null) {
            return false;
        }
        if (this.active.ownerScreen != screen) {
            this.close(ScreenOverlayCloseReason.SCREEN_CHANGED);
            return false;
        }
        return true;
    }

    private record ActiveOverlay(
        Screen ownerScreen,
        VanillaScreenOverlay overlay,
        ScreenOverlayContext context
    ) {
    }
}
