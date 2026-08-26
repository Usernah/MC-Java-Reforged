package net.jr.client.ui.hint.provider;

import net.jr.client.components.navigation.UiAction;

public interface RecipeControlHintHost {
    Activation controlHintActivation(UiAction action);

    enum Activation {
        NONE,
        VIEW_VARIANTS,
        CRAFT
    }
}
