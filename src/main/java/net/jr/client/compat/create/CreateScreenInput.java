package net.jr.client.compat.create;

import net.jr.api.client.input.IScreenInput;
import net.jr.api.client.input.Inputs;
import net.jr.api.client.local.Client;
import net.jr.client.input.InputApi;
import net.minecraft.client.MouseHandler;

public final class CreateScreenInput {
    private CreateScreenInput() {
    }

    public static double localWindowX(MouseHandler mouseHandler) {
        return screen().localWindowX(mouseHandler);
    }

    public static double localWindowY(MouseHandler mouseHandler) {
        return screen().localWindowY(mouseHandler);
    }

    public static int guiWidth() {
        return screen().guiWidth();
    }

    public static int guiHeight() {
        return screen().guiHeight();
    }

    public static int screenWidth() {
        return screen().windowWidth();
    }

    public static int screenHeight() {
        return screen().windowHeight();
    }

    public static int framebufferWidth() {
        return screen().framebufferWidth();
    }

    public static int framebufferHeight() {
        return screen().framebufferHeight();
    }

    private static IScreenInput screen() {
        return Client.current().input().screen();
    }
}
