package net.jr.screens.controller;

import net.jr.client.runtime.options.OptionsStateResolver;
import net.jr.client.runtime.state.OptionsState;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

public final class ControllerSettingsScreen extends OptionsSubScreen {
    private static final Component TITLE = Component.translatable(
            "controls.java_reforged.controller_options.title"
    );

    public ControllerSettingsScreen(Screen lastScreen, Options options) {
        super(lastScreen, options, TITLE);
    }

    @Override
    protected void addOptions() {
        OptionsState state = OptionsStateResolver.current();
        this.list.addSmall(
                state.controllerCursorSensitivity(),
                state.controllerCameraSensitivity(),
                state.controllerSpyglassSensitivity(),
                state.invertControllerMovementY(),
                state.invertControllerCameraX(),
                state.invertControllerCameraY()
        );
    }
}
