package net.jr.screens.test;

import net.jr.client.ui.layout.UILayoutScreen;
import net.minecraft.network.chat.Component;

public final class TestScreen extends UILayoutScreen {
    public TestScreen() {
        super(Component.literal("Java Reforged Test Screen"), new TestLayout());
    }
}
