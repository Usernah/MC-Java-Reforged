package net.jr.client.ui.presentation;

import net.jr.ClientConfig;
import net.jr.api.client.split.SplitScreen;

public final class UiPresentation {
    public static final String PORTABLE_SUFFIX = "-portable";
    public static final String SPLIT_SCREEN_SUFFIX = "-split_screen";

    private UiPresentation() {
    }

    public static InterfaceProfile selectedProfile() {
        return ClientConfig.interfaceProfile();
    }

    public static InterfaceProfile effectiveProfile() {
        return isSplitScreen() ? InterfaceProfile.STANDARD : selectedProfile();
    }

    public static boolean isPortable() {
        return effectiveProfile() == InterfaceProfile.PORTABLE;
    }

    public static boolean isSplitScreen() {
        return SplitScreen.isActive();
    }

    public static String documentVariantSuffix() {
        if (isSplitScreen()) {
            return SPLIT_SCREEN_SUFFIX;
        }
        return isPortable() ? PORTABLE_SUFFIX : "";
    }

    public static String resourceVariantFolder() {
        if (isSplitScreen()) {
            return "split_screen";
        }
        return isPortable() ? "portable" : "";
    }
}
