package net.jr.client.systemui;

import java.util.Map;
import java.util.WeakHashMap;
import net.jr.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class SystemUiSounds {
    private static final Map<Object, Boolean> FOCUS_STATES = new WeakHashMap<>();

    private SystemUiSounds() {
    }

    public static void playOpen() {
        play(ModSounds.SYSTEM_UI_OVERLAY_OPEN.get());
    }

    public static void playClose() {
        play(ModSounds.SYSTEM_UI_OVERLAY_CLOSE.get());
    }

    public static void playSelectButton() {
        play(ModSounds.SYSTEM_UI_SELECT_BUTTON.get());
    }

    public static void playSelectCharacter() {
        play(ModSounds.SYSTEM_UI_SELECT_CHARACTER.get());
    }

    public static void updateFocus(Object widget, boolean focused) {
        boolean previous = Boolean.TRUE.equals(FOCUS_STATES.put(widget, focused));
        if (focused && !previous) {
            play(ModSounds.SYSTEM_UI_FOCUS.get());
        }
    }

    private static void play(SoundEvent sound) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        minecraft.getSoundManager().play(new SimpleSoundInstance(
            sound.location(),
            SoundSource.MASTER,
            1.0F,
            1.0F,
            RandomSource.create(),
            false,
            0,
            SoundInstance.Attenuation.NONE,
            0.0D,
            0.0D,
            0.0D,
            true
        ));
    }
}
