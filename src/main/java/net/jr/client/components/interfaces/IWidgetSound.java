package net.jr.client.components.interfaces;

import net.jr.client.components.widgets.Button;
import net.minecraft.client.sounds.SoundManager;

@FunctionalInterface
public interface IWidgetSound {
    void play(SoundManager handler, Button button);
}
