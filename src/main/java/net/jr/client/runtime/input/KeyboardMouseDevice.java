package net.jr.client.runtime.input;

import net.jr.api.client.input.IKeyboardMouse;

public final class KeyboardMouseDevice implements IKeyboardMouse {
    @Override
    public String displayName() {
        return "Keyboard and mouse";
    }

    @Override
    public boolean connected() {
        return true;
    }
}
