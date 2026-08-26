package net.jr.client.ui.container;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;

public final class ContainerHighlightPolicy {
    private ContainerHighlightPolicy() {
    }

    public static boolean suppressFrontHighlight() {
        return Minecraft.getInstance().gui.screen() instanceof ContainerScreen;
    }
}
