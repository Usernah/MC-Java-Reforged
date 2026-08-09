package net.jr.client.sound.action;

import net.jr.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class InputActionSounds {
    private InputActionSounds() {}

    public static void playBack() { play(ModSounds.UI_BACK.get()); }
    public static void playHover() { play(ModSounds.UI_HOVER.get()); }
    public static void playClick() { play(ModSounds.UI_CLICK.get()); }
    public static void playChar() { play(ModSounds.UI_CHAR.get()); }
    public static void playBlock() { play(ModSounds.UI_BLOCK.get()); }
    public static void playHotbarFocus() { play(ModSounds.UI_HOTBAR_FOCUS.get()); }
    public static void playToastNotify() { play(ModSounds.UI_TOAST_NOTIFY.get()); }
    public static void playGameplayItemPop() { play(ModSounds.GAMEPLAY_ITEM_POP.get()); }

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
}
