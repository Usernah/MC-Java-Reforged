package net.jr.api.client.systemui;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

public interface SystemOverlay {
    default void opened(SystemUiHandle handle) {
    }

    default void closing(SystemUiCloseReason reason) {
    }

    default boolean closeAnimationFinished() {
        return true;
    }

    default void tick() {
    }

    void render(SystemUiRenderContext context);

    default void mouseMoved(double x, double y) {
    }

    default void mouseButton(double x, double y, int button, int action) {
    }

    default void mouseScrolled(double x, double y, double horizontal, double vertical) {
    }

    default void keyEvent(int action, KeyEvent event) {
    }

    default void charTyped(CharacterEvent event) {
    }

    default void closed(SystemUiCloseReason reason) {
    }
}
