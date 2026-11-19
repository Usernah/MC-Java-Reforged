package net.jr.client.ui.hint.glyph;

import java.util.Objects;
import javax.annotation.Nullable;
import net.jr.client.input.InputApi;
import net.jr.api.client.input.GamepadIdentity;
import net.jr.api.client.input.IGamepad;
import net.jr.api.client.input.ControllerGlyphSet;
import net.jr.client.input.gamepad.GamepadProfileResolver;

public final class GamepadGlyphThemeState {
    private static volatile ControllerGlyphTheme currentTheme = ControllerGlyphTheme.GENERIC_A;
    private static final ThreadLocal<ControllerGlyphTheme> renderTheme = new ThreadLocal<>();

    private GamepadGlyphThemeState() {
    }

    public static ControllerGlyphTheme currentTheme() {
        return themeForCurrentClient();
    }

    public static void setCurrentTheme(ControllerGlyphTheme theme) {
        currentTheme = Objects.requireNonNull(theme, "theme");
    }

    public static ControllerGlyphTheme themeForCurrentClient() {
        ControllerGlyphTheme scopedTheme = renderTheme.get();
        if (scopedTheme != null) {
            return scopedTheme;
        }
        ControllerGlyphTheme theme = themeForGamepad(InputApi.currentGamepad());
        return theme == null ? currentTheme : theme;
    }

    public static void withRenderTheme(ControllerGlyphTheme theme, Runnable render) {
        ControllerGlyphTheme previous = renderTheme.get();
        renderTheme.set(Objects.requireNonNull(theme, "theme"));
        try {
            Objects.requireNonNull(render, "render").run();
        } finally {
            if (previous == null) {
                renderTheme.remove();
            } else {
                renderTheme.set(previous);
            }
        }
    }

    @Nullable
    public static ControllerGlyphTheme themeForGamepad(@Nullable IGamepad gamepad) {
        return themeForGlyphs(gamepad == null
            ? ControllerGlyphSet.GENERIC : gamepad.profile().glyphs());
    }

    @Nullable
    public static ControllerGlyphTheme themeForIdentity(@Nullable GamepadIdentity identity) {
        return themeForGlyphs(GamepadProfileResolver.glyphs(identity));
    }

    private static ControllerGlyphTheme themeForGlyphs(ControllerGlyphSet glyphs) {
        return switch (glyphs) {
            case GENERIC -> ControllerGlyphTheme.GENERIC_A;
            case XBOX -> ControllerGlyphTheme.XBOX;
            case XBOX_LEGACY -> ControllerGlyphTheme.XBOX_OLD;
            case STEAM -> ControllerGlyphTheme.STEAM;
            case PLAYSTATION -> ControllerGlyphTheme.PS;
            case PLAYSTATION_LEGACY -> ControllerGlyphTheme.PS_OLD;
            case SWITCH -> ControllerGlyphTheme.SWITCH;
        };
    }
}
