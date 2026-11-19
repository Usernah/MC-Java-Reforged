package net.jr.client.systemui;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import net.jr.api.client.systemui.SystemOverlay;
import net.jr.api.client.systemui.SystemUiCloseReason;
import net.jr.api.client.systemui.SystemUiHandle;
import net.jr.api.client.systemui.SystemUiScope;
import net.jr.api.client.systemui.SystemUiTarget;
import net.jr.client.input.InputApi;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.slot.ClientSlots;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.runtime.viewport.ViewportGuiScale;
import net.jr.client.ui.cursor.SystemCursorController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.jetbrains.annotations.Nullable;

public final class SystemUiManager {
    private static final SystemUiManager INSTANCE = new SystemUiManager();

    private final AtomicLong nextId = new AtomicLong(1L);
    private final Map<Integer, ActiveOverlay> viewportOverlays = new HashMap<>();
    private @Nullable ActiveOverlay fullWindowOverlay;
    private boolean restoreMouseGrab;
    private boolean mouseSessionActive;

    private SystemUiManager() {
    }

    public static SystemUiManager get() {
        return INSTANCE;
    }

    public SystemUiHandle show(SystemUiTarget target, SystemOverlay overlay) {
        return this.showInternal(target, overlay, null);
    }

    public SystemUiHandle showMenu(
        SystemUiTarget target,
        SystemOverlay overlay,
        @Nullable MenuHistory lastMenu
    ) {
        return this.showInternal(target, overlay, lastMenu);
    }

    private SystemUiHandle showInternal(
        SystemUiTarget target,
        SystemOverlay overlay,
        @Nullable MenuHistory lastMenu
    ) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(overlay, "overlay");
        if (target.ownerSlot() >= ClientSlots.MAX_SLOTS) {
            throw new IllegalArgumentException("System UI owner slot is out of range: " + target.ownerSlot());
        }
        if (target.scope() == SystemUiScope.VIEWPORT
                && !InputApi.hasGamepadForSlot(target.ownerSlot())) {
            throw new IllegalStateException(
                "Viewport System UI requires a gamepad assigned to slot " + target.ownerSlot()
            );
        }
        this.beginMouseSession();
        SystemUiHandle handle = new SystemUiHandle(this.nextId.getAndIncrement(), target);
        ActiveOverlay active = new ActiveOverlay(handle, overlay, lastMenu);

        if (target.scope() == SystemUiScope.FULL_WINDOW) {
            if (!this.isPreservedHistory(this.fullWindowOverlay, lastMenu)) {
                this.finishImmediately(this.fullWindowOverlay, SystemUiCloseReason.REPLACED);
            }
            this.fullWindowOverlay = active;
        } else {
            ActiveOverlay previous = this.viewportOverlays.put(target.ownerSlot(), active);
            if (!this.isPreservedHistory(previous, lastMenu)) {
                this.finishImmediately(previous, SystemUiCloseReason.REPLACED);
            }
        }
        overlay.opened(handle);
        InputApi.releaseBindings();
        InputApi.suppressHeldBindings();
        return handle;
    }

    public @Nullable MenuHistory captureMenu(SystemUiTarget target) {
        ActiveOverlay active = target.scope() == SystemUiScope.FULL_WINDOW
            ? this.fullWindowOverlay
            : this.viewportOverlays.get(target.ownerSlot());
        return active == null
            ? null
            : new MenuHistory(active.handle.target(), active.overlay, active.lastMenu);
    }

    public boolean isOpen(SystemUiHandle handle) {
        ActiveOverlay active = this.find(handle);
        return active != null && active.handle.id() == handle.id();
    }

    public void close(SystemUiHandle handle, SystemUiCloseReason reason) {
        ActiveOverlay active = this.find(handle);
        if (active == null || active.handle.id() != handle.id()) {
            return;
        }
        active.beginClosing(reason == null ? SystemUiCloseReason.CANCELLED : reason);
        this.finishIfReady(active);
        InputApi.releaseBindings();
        InputApi.suppressHeldBindings();
    }

    public void back(SystemUiHandle handle) {
        ActiveOverlay active = this.find(handle);
        if (active == null || active.handle.id() != handle.id()) {
            return;
        }
        if (active.lastMenu == null) {
            this.close(handle, SystemUiCloseReason.CANCELLED);
            return;
        }
        active.returnToLastMenu = true;
        active.beginClosing(SystemUiCloseReason.BACK);
        this.finishIfReady(active);
        InputApi.releaseBindings();
        InputApi.suppressHeldBindings();
    }

    public boolean hasFullWindowOverlay() {
        return this.fullWindowOverlay != null;
    }

    public boolean blocksSlot(int slotId) {
        return this.fullWindowOverlay != null || this.viewportOverlays.containsKey(slotId);
    }

    public @Nullable ActiveOverlay fullWindowOverlay() {
        return this.fullWindowOverlay;
    }

    public Map<Integer, ActiveOverlay> viewportOverlays() {
        return Map.copyOf(this.viewportOverlays);
    }

    public void tickInput(int slotId) {
        ActiveOverlay active = this.inputOverlay(slotId);
        if (active != null && active.handle.target().ownerSlot() == slotId) {
            if (active.isClosing()) {
                this.finishIfReady(active);
            } else {
                active.overlay.tick();
            }
        }
    }

    public void updateClosures() {
        this.finishIfReady(this.fullWindowOverlay);
        for (ActiveOverlay active : java.util.List.copyOf(this.viewportOverlays.values())) {
            this.finishIfReady(active);
        }
    }

    public boolean keyEvent(int action, KeyEvent event) {
        ActiveOverlay active = this.keyboardMouseOverlay();
        if (active == null) {
            return false;
        }
        if (!active.isClosing()) {
            active.overlay.keyEvent(action, event);
        }
        return true;
    }

    public boolean charTyped(CharacterEvent event) {
        ActiveOverlay active = this.keyboardMouseOverlay();
        if (active == null) {
            return false;
        }
        if (!active.isClosing()) {
            active.overlay.charTyped(event);
        }
        return true;
    }

    public boolean mouseMoved(double rawX, double rawY) {
        ActiveOverlay active = this.keyboardMouseOverlay();
        if (active == null) {
            return false;
        }
        if (!active.isClosing()) {
            Point point = this.toCanvas(active.handle.target(), rawX, rawY);
            active.overlay.mouseMoved(point.x, point.y);
        }
        return true;
    }

    public boolean mouseButton(int button, int action) {
        ActiveOverlay active = this.keyboardMouseOverlay();
        if (active == null) {
            return false;
        }
        if (!active.isClosing()) {
            Minecraft minecraft = Minecraft.getInstance();
            Point point = this.toCanvas(
                active.handle.target(),
                minecraft.mouseHandler.xpos(),
                minecraft.mouseHandler.ypos()
            );
            active.overlay.mouseButton(point.x, point.y, button, action);
        }
        return true;
    }

    public boolean mouseScrolled(double horizontal, double vertical) {
        ActiveOverlay active = this.keyboardMouseOverlay();
        if (active == null) {
            return false;
        }
        if (!active.isClosing()) {
            Minecraft minecraft = Minecraft.getInstance();
            Point point = this.toCanvas(
                active.handle.target(),
                minecraft.mouseHandler.xpos(),
                minecraft.mouseHandler.ypos()
            );
            active.overlay.mouseScrolled(point.x, point.y, horizontal, vertical);
        }
        return true;
    }

    public void clear() {
        this.finishForced(this.fullWindowOverlay, SystemUiCloseReason.FORCED);
        this.fullWindowOverlay = null;
        for (ActiveOverlay active : this.viewportOverlays.values()) {
            this.finishForced(active, SystemUiCloseReason.FORCED);
        }
        this.viewportOverlays.clear();
        this.endMouseSession();
    }

    private @Nullable ActiveOverlay inputOverlay(int slotId) {
        return this.fullWindowOverlay != null
            ? this.fullWindowOverlay
            : this.viewportOverlays.get(slotId);
    }

    private @Nullable ActiveOverlay keyboardMouseOverlay() {
        return this.inputOverlay(InputApi.keyboardMouseSlotId());
    }

    private @Nullable ActiveOverlay find(SystemUiHandle handle) {
        return handle.target().scope() == SystemUiScope.FULL_WINDOW
            ? this.fullWindowOverlay
            : this.viewportOverlays.get(handle.target().ownerSlot());
    }

    private void finishImmediately(@Nullable ActiveOverlay active, SystemUiCloseReason reason) {
        if (active != null) {
            active.beginClosing(reason);
            active.overlay.closed(reason);
        }
    }

    private void finishForced(@Nullable ActiveOverlay active, SystemUiCloseReason reason) {
        if (active != null) {
            active.overlay.closed(reason);
        }
    }

    private void finishIfReady(@Nullable ActiveOverlay active) {
        if (active == null || !active.isClosing() || !active.overlay.closeAnimationFinished()) {
            return;
        }
        if (active.handle.target().scope() == SystemUiScope.FULL_WINDOW) {
            if (this.fullWindowOverlay != active) {
                return;
            }
            this.fullWindowOverlay = null;
        } else if (!this.viewportOverlays.remove(active.handle.target().ownerSlot(), active)) {
            return;
        }
        active.overlay.closed(active.closeReason);
        if (active.returnToLastMenu && active.lastMenu != null) {
            MenuHistory previous = active.lastMenu;
            this.showMenu(previous.target, previous.menu, previous.lastMenu);
        } else {
            this.endMouseSessionIfIdle();
        }
        InputApi.releaseBindings();
        InputApi.suppressHeldBindings();
    }

    private boolean isPreservedHistory(
        @Nullable ActiveOverlay active,
        @Nullable MenuHistory history
    ) {
        return active != null && history != null && active.overlay == history.menu;
    }

    private void beginMouseSession() {
        if (this.mouseSessionActive) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.mouseHandler == null) {
            return;
        }
        this.mouseSessionActive = true;
        this.restoreMouseGrab = minecraft.mouseHandler.isMouseGrabbed();
        if (this.restoreMouseGrab) {
            minecraft.mouseHandler.releaseMouse();
        }
        SystemCursorController.setHidden(false);
    }

    private void endMouseSessionIfIdle() {
        if (this.fullWindowOverlay == null && this.viewportOverlays.isEmpty()) {
            this.endMouseSession();
        }
    }

    private void endMouseSession() {
        if (!this.mouseSessionActive) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.mouseHandler != null && this.restoreMouseGrab) {
            minecraft.mouseHandler.grabMouse();
        }
        this.mouseSessionActive = false;
        this.restoreMouseGrab = false;
    }

    private Point toCanvas(SystemUiTarget target, double rawX, double rawY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (target.scope() == SystemUiScope.FULL_WINDOW) {
            double windowWidth = Math.max(1, minecraft.getWindow().getWidth());
            double windowHeight = Math.max(1, minecraft.getWindow().getHeight());
            return new Point(
                rawX * minecraft.getWindow().getGuiScaledWidth() / windowWidth
                    / net.jr.api.client.systemui.SystemUI.FRAMEBUFFER_SCALE,
                rawY * minecraft.getWindow().getGuiScaledHeight() / windowHeight
                    / net.jr.api.client.systemui.SystemUI.FRAMEBUFFER_SCALE
            );
        }

        ViewportArea viewport = ClientRuntime.INSTANCE.viewports().viewport(target.ownerSlot());
        float screenScale = ViewportGuiScale.screenScale(viewport);
        float framebufferScale = net.jr.api.client.systemui.SystemUI.framebufferScale(
            viewport.layout().viewportCount() > 1
        );
        return new Point(
            viewport.windowMouseToLocalGuiX(rawX)
                / screenScale
                / framebufferScale,
            viewport.windowMouseToLocalGuiY(rawY)
                / screenScale
                / framebufferScale
        );
    }

    public static final class ActiveOverlay {
        private final SystemUiHandle handle;
        private final SystemOverlay overlay;
        private final @Nullable MenuHistory lastMenu;
        private @Nullable SystemUiCloseReason closeReason;
        private boolean returnToLastMenu;

        private ActiveOverlay(
            SystemUiHandle handle,
            SystemOverlay overlay,
            @Nullable MenuHistory lastMenu
        ) {
            this.handle = handle;
            this.overlay = overlay;
            this.lastMenu = lastMenu;
        }

        public SystemUiHandle handle() {
            return this.handle;
        }

        public SystemOverlay overlay() {
            return this.overlay;
        }

        public boolean isClosing() {
            return this.closeReason != null;
        }

        private void beginClosing(SystemUiCloseReason reason) {
            if (this.closeReason != null) {
                return;
            }
            this.closeReason = reason;
            this.overlay.closing(reason);
        }
    }

    public record MenuHistory(
        SystemUiTarget target,
        SystemOverlay menu,
        @Nullable MenuHistory lastMenu
    ) {
        public MenuHistory {
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(menu, "menu");
        }
    }

    private record Point(double x, double y) {
    }
}
