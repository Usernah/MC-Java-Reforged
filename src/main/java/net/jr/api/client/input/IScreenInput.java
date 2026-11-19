package net.jr.api.client.input;

import net.minecraft.client.MouseHandler;


public interface IScreenInput {
    double localWindowX(MouseHandler mouse);

    double localWindowY(MouseHandler mouse);

    int guiWidth();

    int guiHeight();

    int windowWidth();

    int windowHeight();

    int framebufferWidth();

    int framebufferHeight();
}
