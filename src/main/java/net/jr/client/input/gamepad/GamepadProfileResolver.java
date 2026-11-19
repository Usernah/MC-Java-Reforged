package net.jr.client.input.gamepad;

import java.util.Locale;
import javax.annotation.Nullable;
import net.jr.api.client.input.ControllerGlyphSet;
import net.jr.api.client.input.GamepadIdentity;


public final class GamepadProfileResolver {
    private GamepadProfileResolver() {
    }

    public static ControllerGlyphSet glyphs(@Nullable GamepadIdentity identity) {
        if (identity == null) {
            return ControllerGlyphSet.GENERIC;
        }

        String name = normalize(identity.displayName());
        String key = normalize(identity.key());
        String guid = normalize(identity.guid());
        String text = name + " " + key + " " + guid;
        int vendor = identity.vendor();
        int product = identity.product();

        if (vendor == 0x28de || containsAny(text, "steam", "valve")) {
            return ControllerGlyphSet.XBOX;
        }
        if (vendor == 0x057e || containsAny(text, "nintendo", "switch", "joy-con", "joycon", "pro controller")) {
            return ControllerGlyphSet.XBOX;
        }
        if (vendor == 0x054c || containsAny(text, "sony", "playstation", "dualshock", "dualsense", "ps3", "ps4", "ps5", "wireless controller")) {
            return isPlayStation5(text, product)
                ? ControllerGlyphSet.PLAYSTATION : ControllerGlyphSet.PLAYSTATION_LEGACY;
        }
        if (containsAny(text, "xbox 360", "360 controller") || (vendor == 0x045e && isXbox360(product))) {
            return ControllerGlyphSet.XBOX;
        }
        if (vendor == 0x045e || containsAny(text, "xbox series", "xbox one", "xbox wireless controller", "microsoft")) {
            return ControllerGlyphSet.XBOX;
        }
        if (containsAny(text, "xinput", "x-input", "xbox")) {
            return ControllerGlyphSet.XBOX;
        }
        return ControllerGlyphSet.GENERIC;
    }

    private static boolean isPlayStation5(String text, int product) {
        return product == 0x0ce6 || product == 0x0df2
            || containsAny(text, "dualsense", "ps5", "playstation 5");
    }

    private static boolean isXbox360(int product) {
        return product == 0x028e || product == 0x028f || product == 0x0719;
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
