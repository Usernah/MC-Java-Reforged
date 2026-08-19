package net.jr.mixin.controls;

import net.jr.screens.controller.ControllerBindingsScreen;
import net.jr.screens.controller.ControllerCalibrationScreen;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.MouseSettingsScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ControlsScreen.class)
public abstract class ControlsScreenMixin extends OptionsSubScreen {
    protected ControlsScreenMixin(Screen lastScreen, Options options, Component title) {
        super(lastScreen, options, title);
    }

    @Inject(method = "addOptions", at = @At("HEAD"), cancellable = true)
    private void javareforged$replaceKeybindScreen(CallbackInfo ci) {
        this.list.addSmall(
            Button.builder(
                Component.translatable("options.mouse_settings"),
                button -> minecraft.gui.setScreen(new MouseSettingsScreen((Screen) (Object) this, options))
            ).build(),
            Button.builder(
                Component.translatable("controls.keybinds"),
                button -> minecraft.gui.setScreen(new ControllerBindingsScreen((Screen) (Object) this, options))
            ).build()
        );
        this.list.addSmall(java.util.List.<AbstractWidget>of(
            Button.builder(
                Component.literal("Calibracion"),
                button -> minecraft.gui.setScreen(new ControllerCalibrationScreen((Screen) (Object) this))
            ).build()
        ));
        this.list.addSmall(new OptionInstance[]{
            options.toggleCrouch(),
            options.toggleSprint(),
            options.autoJump(),
            options.operatorItemsTab()
        });
        ci.cancel();
    }
}
