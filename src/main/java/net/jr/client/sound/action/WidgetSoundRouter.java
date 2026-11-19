package net.jr.client.sound.action;

import net.jr.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;

public final class WidgetSoundRouter {
    private static final ThreadLocal<Boolean> PENDING_PRESS = new ThreadLocal<>();

    private WidgetSoundRouter() {
    }

    public static void playDownSound(AbstractWidget widget, SoundManager soundManager) {
        if (widget instanceof AbstractSliderButton) {
            return;
        }
        if (PENDING_PRESS.get() == null) {
            playClick(soundManager);
        } else {
            PENDING_PRESS.set(true);
        }
    }

    public static void beginPress() {
        PENDING_PRESS.set(false);
    }

    public static void finishPress(Screen sourceScreen, boolean handled) {
        boolean play = handled
                && Boolean.TRUE.equals(PENDING_PRESS.get())
                && Minecraft.getInstance().gui.screen() == sourceScreen;
        PENDING_PRESS.remove();
        if (play) {
            playClick(Minecraft.getInstance().getSoundManager());
        }
    }

    private static void playClick(SoundManager soundManager) {
        soundManager.play(SimpleSoundInstance.forUI(ModSounds.UI_CLICK.get(), 1.0F, 1.0F));
    }
}
