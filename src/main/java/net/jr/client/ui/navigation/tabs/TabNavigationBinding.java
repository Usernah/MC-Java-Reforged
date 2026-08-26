package net.jr.client.ui.navigation.tabs;

import java.util.List;
import java.util.Objects;
import net.jr.client.components.navigation.TabNavigator;
import net.jr.client.ui.navigation.UiInputBinding;
import net.jr.client.sound.action.InputActionSounds;
import net.jr.client.ui.hint.model.ControlHintRequest;
import net.jr.client.ui.hint.model.HintPlacement;
import net.minecraft.network.chat.Component;

public record TabNavigationBinding(
        TabNavigator navigator,
        UiInputBinding previous,
        UiInputBinding next,
        Component hintLabel,
        HintPlacement hintPlacement,
        Runnable selectionFeedback
) {
    public TabNavigationBinding {
        Objects.requireNonNull(navigator, "navigator");
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(next, "next");
        Objects.requireNonNull(hintLabel, "hintLabel");
        Objects.requireNonNull(hintPlacement, "hintPlacement");
        Objects.requireNonNull(selectionFeedback, "selectionFeedback");
        if (previous.equals(next)) {
            throw new IllegalArgumentException("previous and next inputs must be different");
        }
    }

    public TabNavigationBinding(
            TabNavigator navigator,
            UiInputBinding previous,
            UiInputBinding next,
            Component hintLabel
    ) {
        this(
                navigator,
                previous,
                next,
                hintLabel,
                HintPlacement.RIGHT,
                InputActionSounds::playClick
        );
    }

    public TabNavigationBinding(
            TabNavigator navigator,
            UiInputBinding previous,
            UiInputBinding next,
            Component hintLabel,
            HintPlacement hintPlacement
    ) {
        this(
                navigator,
                previous,
                next,
                hintLabel,
                hintPlacement,
                InputActionSounds::playClick
        );
    }

    public TabNavigationBinding(
            TabNavigator navigator,
            UiInputBinding previous,
            UiInputBinding next,
            Component hintLabel,
            Runnable selectionFeedback
    ) {
        this(navigator, previous, next, hintLabel, HintPlacement.RIGHT, selectionFeedback);
    }

    public ControlHintRequest hintRequest() {
        return new ControlHintRequest(
                List.of(this.previous, this.next),
                this.hintLabel,
                this.hintPlacement
        );
    }
}
