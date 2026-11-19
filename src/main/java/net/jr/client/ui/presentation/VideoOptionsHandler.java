package net.jr.client.ui.presentation;

import net.jr.api.client.local.Client;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;

import java.util.ArrayList;
import java.util.List;

public final class VideoOptionsHandler {
    private VideoOptionsHandler() {
    }

    public static OptionInstance<?>[] extend(Options options, OptionInstance<?>[] original) {
        var currentClient = Client.currentOrNull();
        if (currentClient == null) {
            currentClient = Client.get(0);
        }
        var state = currentClient.options();
        boolean primary = currentClient.isPrimary();
        OptionInstance<?> brightness = options.gamma();
        OptionInstance<?> vanillaGuiScale = options.guiScale();
        List<OptionInstance<?>> extended = new ArrayList<>(original.length);
        boolean legacyGammaInserted = false;
        for (OptionInstance<?> option : original) {
            if (option == vanillaGuiScale) {
                continue;
            }
            extended.add(option);
            if (option == brightness) {
                if (primary) {
                    extended.add(state.legacyGamma());
                }
                legacyGammaInserted = true;
            }
        }
        if (!legacyGammaInserted && primary) {
            extended.add(state.legacyGamma());
        }
        return extended.toArray(OptionInstance<?>[]::new);
    }

    public static OptionInstance<?>[] filterPreferences(Options options, OptionInstance<?>[] original) {
        List<OptionInstance<?>> filtered = new ArrayList<>(original.length);
        OptionInstance<?> autosaveIndicator = options.showAutosaveIndicator();
        OptionInstance<?> attackIndicator = options.attackIndicator();
        for (OptionInstance<?> option : original) {
            if (option != autosaveIndicator && option != attackIndicator) {
                filtered.add(option);
            }
        }
        return filtered.toArray(OptionInstance<?>[]::new);
    }
}
