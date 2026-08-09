package net.jr.client.ui.hint.glyph;

import java.util.Locale;
import java.util.Objects;
import javax.annotation.Nullable;
import net.jr.api.client.resource.Asset;
import net.jr.client.input.InputApi;
import net.jr.client.input.gamepad.GamepadIdentity;
import net.jr.client.input.sdl.SdlGamepad;

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
        return currentTheme().font();
    }

    public static Asset font(ControllerGlyphTheme theme) {
        return Objects.requireNonNull(theme, "theme").font();
    }

    public static ControllerGlyphTheme themeForCurrentClient() {
        ControllerGlyphTheme theme = themeForIdentity(InputApi.currentGamepadIdentity());
        return theme == null ? currentTheme : theme;
    }

    @Nullable
    public static ControllerGlyphTheme themeForIdentity(@Nullable GamepadIdentity identity) {
        if (identity == null) {
            return ControllerGlyphTheme.GENERIC_A;
        }

        String name = normalize(identity.displayName());
        String key = normalize(identity.key());
        String guid = normalize(identity.guid());
        String text = name + " " + key + " " + guid;
        int vendor = identity.vendor();
        int product = identity.product();

        if (vendor == 0x28de || containsAny(text, "steam", "valve")) {
            return ControllerGlyphTheme.STEAM;
        }

        if (vendor == 0x057e || containsAny(text, "nintendo", "switch", "joy-con", "joycon", "pro controller")) {
            return ControllerGlyphTheme.SWITCH;
        }

        if (vendor == 0x054c || containsAny(text, "sony", "playstation", "dualshock", "dualsense", "ps3", "ps4", "ps5", "wireless controller")) {
            return isPlayStation5(text, product) ? ControllerGlyphTheme.PS : ControllerGlyphTheme.PS_OLD;
        }

        if (containsAny(text, "xbox 360", "360 controller") || (vendor == 0x045e && isXbox360(product))) {
            return ControllerGlyphTheme.XBOX_OLD;
        }

        if (vendor == 0x045e || containsAny(text, "xbox series", "xbox one", "xbox wireless controller", "microsoft")) {
            return ControllerGlyphTheme.XBOX;
        }

        if (containsAny(text, "xinput", "x-input", "xbox")) {
            return ControllerGlyphTheme.XBOX_OLD;
        }

        return ControllerGlyphTheme.STEAM;
    }

    private static boolean isPlayStation5(String text, int product) {
        return product == 0x0ce6
            || product == 0x0df2
            || containsAny(text, "dualsense", "ps5", "playstation 5");
    }

    private static boolean isXbox360(int product) {
        return product == 0x028e
            || product == 0x028f
            || product == 0x0719;
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

}
