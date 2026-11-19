package net.jr.api.client.systemui;

import net.jr.client.systemui.SystemUiManager;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

public final class SystemUI {
    public static final float FRAMEBUFFER_SCALE = 0.25F;
    public static final float SPLIT_FRAMEBUFFER_SCALE = 0.35F;
    public static final int FRAMEBUFFER_RASTER_SCALE = 3;

    private SystemUI() {
    }

    public static float framebufferScale(boolean splitScreen) {
        return splitScreen ? SPLIT_FRAMEBUFFER_SCALE : FRAMEBUFFER_SCALE;
    }

    public static SystemUiHandle show(SystemOverlay overlay) {
        return show(SystemUiTarget.fullWindow(), overlay);
    }

    public static SystemUiHandle show(SystemUiTarget target, SystemOverlay overlay) {
        return SystemUiManager.get().show(target, overlay);
    }

    public static ShowMenu showMenu(SystemOverlay menu) {
        return ShowMenu.of(menu);
    }

    public static ShowMenu showMenu(SystemUiTarget target, SystemOverlay menu) {
        return ShowMenu.of(target, menu);
    }

    public static boolean blocksSlot(int slotId) {
        return SystemUiManager.get().blocksSlot(slotId);
    }

    public static boolean blocksAllSlots() {
        return SystemUiManager.get().hasFullWindowOverlay();
    }

    public static boolean blocksKeyboardMouse() {
        return blocksSlot(net.jr.client.input.InputApi.keyboardMouseSlotId());
    }

    public static boolean keyEvent(int action, KeyEvent event) {
        return SystemUiManager.get().keyEvent(action, event);
    }

    public static boolean charTyped(CharacterEvent event) {
        return SystemUiManager.get().charTyped(event);
    }

    public static boolean mouseMoved(double rawX, double rawY) {
        return SystemUiManager.get().mouseMoved(rawX, rawY);
    }

    public static boolean mouseButton(int button, int action) {
        return SystemUiManager.get().mouseButton(button, action);
    }

    public static boolean mouseScrolled(double horizontal, double vertical) {
        return SystemUiManager.get().mouseScrolled(horizontal, vertical);
    }

    public static void tickInput(int slotId) {
        SystemUiManager.get().tickInput(slotId);
    }
}
