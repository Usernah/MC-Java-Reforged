package net.jr.client.ui.navigation.tabs;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.jr.client.components.navigation.TabNavigator;
import net.jr.client.input.InputApi;
import net.jr.client.input.binding.GamepadInputChord;
import net.jr.client.input.binding.UiInputBinding;
import net.jr.client.ui.hint.model.ControlHintRequest;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public final class TabNavigationController {
    private final List<Entry> entries = new ArrayList<>();
    private final Set<UiInputBinding> consumedInputs = new LinkedHashSet<>();

    public void bind(TabNavigationBinding binding) {
        this.entries.add(new Entry(binding));
    }

    public void bind(
            TabNavigator navigator,
            UiInputBinding previous,
            UiInputBinding next,
            Component hintLabel
    ) {
        this.bind(new TabNavigationBinding(navigator, previous, next, hintLabel));
    }

    public void bind(
            TabNavigator navigator,
            UiInputBinding previous,
            UiInputBinding next,
            Component hintLabel,
            Runnable selectionFeedback
    ) {
        this.bind(new TabNavigationBinding(
                navigator,
                previous,
                next,
                hintLabel,
                selectionFeedback
        ));
    }

    public void clear() {
        this.entries.clear();
        this.consumedInputs.clear();
    }

    public boolean process(boolean gamepadAvailable, boolean enabled) {
        this.consumedInputs.clear();
        boolean handled = false;
        for (Entry entry : this.entries) {
            handled = entry.process(gamepadAvailable, enabled, this.consumedInputs) || handled;
        }
        return handled;
    }

    public void synchronize() {
        this.consumedInputs.clear();
        for (Entry entry : this.entries) {
            entry.synchronize();
        }
    }

    @Nullable
    public KeyMapping handleKey(KeyEvent event) {
        for (Entry entry : this.entries) {
            KeyMapping handled = entry.handleKey(event);
            if (handled != null) {
                return handled;
            }
        }
        return null;
    }

    public boolean consumes(KeyMapping mapping) {
        for (UiInputBinding consumed : this.consumedInputs) {
            if (consumed instanceof UiInputBinding.KeyMappingInput keyInput) {
                if (keyInput.mapping() == mapping) {
                    return true;
                }
                continue;
            }
            if (consumed instanceof UiInputBinding.GamepadInput gamepadInput) {
                GamepadInputChord chord = InputApi.binding(mapping);
                if (chord != null && chord.inputs().contains(gamepadInput.input())) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<ControlHintRequest> hintRequests() {
        return this.entries.stream()
                .map(entry -> entry.binding.hintRequest())
                .toList();
    }

    private static final class Entry {
        private final TabNavigationBinding binding;
        private boolean initialized;
        private boolean previousWasDown;
        private boolean nextWasDown;

        private Entry(TabNavigationBinding binding) {
            this.binding = binding;
        }

        private boolean process(
                boolean gamepadAvailable,
                boolean enabled,
                Set<UiInputBinding> consumedInputs
        ) {
            boolean previousDown = this.binding.previous().isDown();
            boolean nextDown = this.binding.next().isDown();
            if (!this.initialized || !enabled) {
                this.binding.previous().discardPresses();
                this.binding.next().discardPresses();
                this.previousWasDown = previousDown;
                this.nextWasDown = nextDown;
                this.initialized = true;
                return false;
            }

            boolean previousPressed = isEnabled(this.binding.previous(), gamepadAvailable)
                    && this.binding.previous().consumePress(this.previousWasDown, previousDown);
            boolean nextPressed = isEnabled(this.binding.next(), gamepadAvailable)
                    && this.binding.next().consumePress(this.nextWasDown, nextDown);
            this.previousWasDown = previousDown;
            this.nextWasDown = nextDown;

            if (previousPressed && !consumedInputs.add(this.binding.previous())) {
                previousPressed = false;
            }
            if (nextPressed && !consumedInputs.add(this.binding.next())) {
                nextPressed = false;
            }

            if (previousPressed == nextPressed) {
                return false;
            }

            return this.select(previousPressed);
        }

        private void synchronize() {
            this.binding.previous().discardPresses();
            this.binding.next().discardPresses();
            this.previousWasDown = this.binding.previous().isDown();
            this.nextWasDown = this.binding.next().isDown();
            this.initialized = true;
        }

        @Nullable
        private KeyMapping handleKey(KeyEvent event) {
            KeyMapping previous = matchingKey(this.binding.previous(), event);
            if (previous != null && this.select(true)) {
                return previous;
            }
            KeyMapping next = matchingKey(this.binding.next(), event);
            if (next != null && this.select(false)) {
                return next;
            }
            return null;
        }

        private boolean select(boolean previous) {
            boolean handled = previous
                    ? this.binding.navigator().selectPrevious()
                    : this.binding.navigator().selectNext();
            if (handled) {
                this.binding.selectionFeedback().run();
            }
            return handled;
        }

        private static boolean isEnabled(UiInputBinding input, boolean gamepadAvailable) {
            return !input.requiresGamepad() || gamepadAvailable;
        }

        @Nullable
        private static KeyMapping matchingKey(UiInputBinding input, KeyEvent event) {
            if (!(input instanceof UiInputBinding.KeyMappingInput keyInput)) {
                return null;
            }
            KeyMapping mapping = keyInput.mapping();
            return !mapping.isUnbound() && mapping.matches(event) ? mapping : null;
        }
    }
}
