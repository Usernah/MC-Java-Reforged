package net.jr.mixin.uifocus;

import net.jr.client.input.InputApi;
import net.jr.client.input.runtime.MappedActionProcessor;
import net.jr.client.sound.action.InputActionSounds;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetSoundMixin {
    @Inject(method = "playDownSound", at = @At("HEAD"), cancellable = true)
    private void javareforged$replaceVanillaClickSound(
            SoundManager soundManager,
            CallbackInfo ci
    ) {
        if ((Object) this instanceof AbstractSliderButton) {
            ci.cancel();
            return;
        }

        if (!InputApi.isGamepadMode()) {
            MappedActionProcessor.suppressNextScreenTransitionSound();
            InputActionSounds.playClick();
        }
        ci.cancel();
    }
}
