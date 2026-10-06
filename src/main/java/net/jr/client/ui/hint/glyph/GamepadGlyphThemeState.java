package net.jr.client.ui.hint.glyph;

import java.util.Objects;
import javax.annotation.Nullable;
import net.jr.api.client.resource.Asset;
import net.jr.client.input.InputApi;
import net.jr.api.client.input.GamepadIdentity;
import net.jr.api.client.input.IGamepad;
import net.jr.api.client.input.ControllerGlyphSet;
import net.jr.client.input.gamepad.GamepadProfileResolver;

public final class GamepadGlyphThemeState {
    private static volatile ControllerGlyphTheme currentTheme = ControllerGlyphTheme.GENERIC_A;

    private GamepadGlyphThemeState() {
    }

    public static ControllerGlyphTheme currentTheme() {
        return themeForCurrentClient();
    }

    public static void setCurrentTheme(ControllerGlyphTheme theme) {
        currentTheme = Objects.requireNonNull(theme, "theme");
    }

    public static Asset currentFont() {
        return font(currentTheme());
    }

    public static Asset font(ControllerGlyphTheme theme) {
        ControllerGlyphTheme resolved = Objects.requireNonNull(theme, "theme");
        return switch (resolved) {
            case PS, PS_OLD -> ControllerGlyphTheme.PS.font();
            default -> ControllerGlyphTheme.XBOX.font();
        };
    }

    public static ControllerGlyphTheme themeForCurrentClient() {
        ControllerGlyphTheme theme = themeForGamepad(InputApi.currentGamepad());
        return theme == null ? currentTheme : theme;
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
            case XBOX -> ControllerGlyphTheme.XBOX_OLD;
            case XBOX_LEGACY -> ControllerGlyphTheme.XBOX_OLD;
            case STEAM -> ControllerGlyphTheme.STEAM;
            case PLAYSTATION -> ControllerGlyphTheme.PS;
            case PLAYSTATION_LEGACY -> ControllerGlyphTheme.PS_OLD;
            case SWITCH -> ControllerGlyphTheme.SWITCH;
        };
    }
}
