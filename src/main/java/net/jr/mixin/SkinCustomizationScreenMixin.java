package net.jr.mixin;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkinCustomizationScreen.class)
public abstract class SkinCustomizationScreenMixin extends OptionsSubScreen {
    protected SkinCustomizationScreenMixin(Screen lastScreen, Options options, Component title) {
        super(lastScreen, options, title);
    }

    @Inject(method = "addOptions", at = @At("HEAD"), cancellable = true)
    private void javaReforged$moveMainHandToGameplay(CallbackInfo callback) {
        List<AbstractWidget> widgets = new ArrayList<>();
        for (PlayerModelPart part : PlayerModelPart.values()) {
            widgets.add(CycleButton.onOffBuilder(this.options.isModelPartEnabled(part))
                    .create(part.getName(), (button, visible) -> this.options.setModelPart(part, visible)));
        }
        this.list.addSmall(widgets);
        callback.cancel();
    }
}
