package net.alnv.javareforged.ClientRuntime.input;

import javax.annotation.Nullable;
import net.alnv.javareforged.ClientRuntime.runtime.Client;
import net.alnv.javareforged.ClientRuntime.runtime.ScreenScale;
import net.alnv.javareforged.ClientRuntime.viewport.ViewportArea;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;

/** Screen input after a high-level keyboard, mouse or gamepad boundary selected the Client. */
public final class ScreenInput {
    private ScreenInput() {
    }

    @Nullable
    public static Screen screen() {
        return Client.screen();
    }

    public static double localWindowX(MouseHandler mouseHandler) {
        return mouseHandler.xpos() - viewport().windowX();
    }

    public static double localWindowY(MouseHandler mouseHandler) {
        return mouseHandler.ypos() - viewport().windowY();
    }

    public static int guiWidth() {
        return ScreenScale.logicalWidth(viewport());
    }

    public static int guiHeight() {
        return ScreenScale.logicalHeight(viewport());
    }

    public static int windowWidth() {
        return viewport().windowWidth();
    }

    public static int windowHeight() {
        return viewport().windowHeight();
    }

    public static void runEvent(Runnable event, String errorTitle, String screenName) {
        Screen.wrapScreenError(event, errorTitle, screenName);
    }

    private static ViewportArea viewport() {
        return Client.viewport();
    }
}
