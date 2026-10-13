package net.jr.client.sound.action;

import net.jr.registry.ModSounds;
import net.jr.client.components.navigation.UiActionFeedback;
import net.jr.client.components.widgets.item.CreativeItemSlotElement;
import net.jr.client.ui.container.actions.ContainerItemSlotFocusController;
import net.jr.client.ui.navigation.UiInputModeController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

public final class InputActionSounds {
    private InputActionSounds() {}

    public static void playBack() { play(ModSounds.UI_BACK.get()); }
    public static void playHover() { play(ModSounds.UI_HOVER.get()); }
    public static void playFocus() { play(ModSounds.UI_HOVER.get()); }
    public static void playClick() { play(ModSounds.UI_CLICK.get()); }
    public static void playChar() { play(ModSounds.UI_CHAR.get()); }
    public static void playBlock() { play(ModSounds.UI_BLOCK.get()); }
    public static void playCraft() { play(ModSounds.UI_CRAFT.get()); }
    public static void playHotbarFocus() { play(ModSounds.UI_HOTBAR_FOCUS.get()); }
    public static void playToastNotify() { play(ModSounds.UI_TOAST_NOTIFY.get()); }
    public static void playGameplayItemPop() { play(ModSounds.GAMEPLAY_ITEM_POP.get()); }

    public static FocusSnapshot captureFocus(Screen screen) {
        GuiEventListener focusedWidget = UiInputModeController.focusedElement(screen);
        Integer focusedSlot = screen instanceof AbstractContainerScreen<?> containerScreen
                ? ContainerItemSlotFocusController.getFocusedItemSlotIndex(containerScreen)
                : null;
        return new FocusSnapshot(focusedWidget, focusedSlot);
    }

    public static void playContainerActionFeedback(
            boolean handled,
            boolean quickMove,
            boolean blockOnFailure
    ) {
        if (handled || quickMove) {
            playClick();
        } else if (blockOnFailure) {
            playBlock();
        }
    }

    public static void play(UiActionFeedback feedback) {
        if (feedback == null) {
            return;
        }

        switch (feedback) {
            case CLICK -> playClick();
            case CHAR -> playChar();
            case NONE -> {
            }
        }
    }

    private static void play(SoundEvent sound) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        SoundManager soundManager = minecraft.getSoundManager();
        soundManager.play(new SimpleSoundInstance(
            sound.location(),
            SoundSource.MASTER,
            1.0F,
            1.0F,
            RandomSource.create(),
            false,
            0,
            SoundInstance.Attenuation.NONE,
            0.0,
            0.0,
            0.0,
            true
        ));
    }

    public record FocusSnapshot(
            @Nullable GuiEventListener focusedWidget,
            @Nullable Integer focusedSlot
    ) {
        public void playIfChanged(Screen screen) {
            FocusSnapshot current = captureFocus(screen);
            if (current.focusedSlot == null
                    && current.focusedWidget != null
                    && current.focusedWidget != this.focusedWidget
                    && !(current.focusedWidget instanceof CreativeItemSlotElement)) {
                playFocus();
            }
        }
    }
}
