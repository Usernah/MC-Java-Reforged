package net.jr.screens.controller;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.MouseSettingsScreen;
import net.minecraft.network.chat.Component;

public final class ControlsScreenHandler {
    private ControlsScreenHandler() {
    }

    public static void populate(
            Screen screen,
            Minecraft minecraft,
            Options options,
            OptionsList list
    ) {
        list.addSmall(
                Button.builder(
                        Component.translatable("options.mouse_settings"),
                        button -> minecraft.gui.setScreen(new MouseSettingsScreen(screen, options))
                ).build(),
                Button.builder(
                        Component.translatable("controls.keybinds"),
                        button -> minecraft.gui.setScreen(new ControllerBindingsScreen(screen, options))
                ).build()
        );
        list.addSmall(List.<AbstractWidget>of(
                Button.builder(
                        Component.translatable("controls.java_reforged.controller_options"),
                        button -> minecraft.gui.setScreen(new ControllerSettingsScreen(screen, options))
                ).build(),
                Button.builder(
                        Component.translatable("controls.java_reforged.calibration"),
                        button -> minecraft.gui.setScreen(new ControllerCalibrationScreen(screen))
                ).build()
        ));
        list.addSmall(new OptionInstance[]{
                options.toggleCrouch(),
                options.toggleSprint()
        });
    }
}
