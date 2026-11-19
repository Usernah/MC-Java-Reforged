package net.jr.client.ui.dsl;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import java.util.function.Function;

/** Generic screen instrumentation seam; production code never imports the experimental editor. */
public final class ScreenEditorHooks {
    private static Listener listener;
    private static Function<Screen, Boolean> toggleHandler;

    private ScreenEditorHooks() { }

    /** Optional tooling callback: production classes do not depend on the editor package. */
    public static void setToggleHandler(Function<Screen, Boolean> handler) {
        toggleHandler = handler;
    }

    /** Called from raw key routing, including while a Screen owns keyboard input. */
    public static boolean toggle(Screen screen) {
        return screen != null && toggleHandler != null && Boolean.TRUE.equals(toggleHandler.apply(screen));
    }

    public static void install(Listener next) { listener = next; }
    public static void clear() { listener = null; }
    public static boolean active() { return listener != null; }

    public static boolean activeFor(Screen screen) { return listener != null && listener.matches(screen); }

    static Screen editedScreenOrNull() {
        return listener == null ? null : listener.editedScreen();
    }

    static void resourcesReloaded() {
        if (listener != null) listener.resourcesReloaded();
    }

    public static boolean fullWindowFor(Screen screen) {
        return activeFor(screen) && listener.fullWindowViewport();
    }

    public static void before(Screen screen, GuiGraphicsExtractor graphics) {
        if (activeFor(screen)) listener.before(screen, graphics);
    }

    public static void after(Screen screen, GuiGraphicsExtractor graphics) {
        if (activeFor(screen)) listener.after(screen, graphics);
    }

    public static boolean mouseButton(Screen screen, double x, double y, int button, int action) {
        return activeFor(screen) && listener.mouseButton(screen, x, y, button, action);
    }

    public static boolean mouseMove(Screen screen, double x, double y) {
        return activeFor(screen) && listener.mouseMove(screen, x, y);
    }

    public static boolean mouseScroll(Screen screen, double x, double y, double horizontal, double vertical) {
        return activeFor(screen) && listener.mouseScroll(screen, x, y, horizontal, vertical);
    }

    public static boolean keyPress(Screen screen, int action, KeyEvent event) {
        return activeFor(screen) && listener.keyPress(screen, action, event);
    }

    public static boolean charTyped(Screen screen, CharacterEvent event) {
        return activeFor(screen) && listener.charTyped(screen, event);
    }

    public interface Listener {
        boolean matches(Screen screen);
        default void before(Screen screen, GuiGraphicsExtractor graphics) { }
        default void after(Screen screen, GuiGraphicsExtractor graphics) { }
        default boolean mouseButton(Screen screen, double x, double y, int button, int action) { return false; }
        default boolean mouseMove(Screen screen, double x, double y) { return false; }
        default boolean mouseScroll(Screen screen, double x, double y, double horizontal, double vertical) { return false; }
        default boolean keyPress(Screen screen, int action, KeyEvent event) { return false; }
        default boolean charTyped(Screen screen, CharacterEvent event) { return false; }
        default boolean fullWindowViewport() { return false; }
        default Screen editedScreen() { return null; }
        default void resourcesReloaded() { }
    }
}
