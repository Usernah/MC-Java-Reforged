package net.jr.client.components.interfaces;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.sounds.SoundManager;

public interface IWidgetSoundHandler {
    enum Event {
        HOVER,
        UNHOVER,
        PRESS,
        RELEASE,
        FOCUS
    }

    void handle(SoundManager handler, AbstractWidget widget, Event event);
}