package net.jr.mixin.uifocus;

import net.jr.client.components.navigation.UiActionFeedback;
import net.jr.client.sound.action.WidgetInputSounds;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSliderButton.class)
public abstract class AbstractSliderButtonSoundMixin {
    @Shadow
    protected double value;

    @Unique
    private double javareforged$valueBeforePointerUpdate;

    @Inject(method = "onClick", at = @At("HEAD"))
    private void javareforged$captureClickValue(
            MouseButtonEvent event,
            boolean doubleClick,
            CallbackInfo ci
    ) {
        this.javareforged$valueBeforePointerUpdate = this.value;
    }

    @Inject(method = "onClick", at = @At("TAIL"))
    private void javareforged$playClickValueFeedback(
            MouseButtonEvent event,
            boolean doubleClick,
            CallbackInfo ci
    ) {
        WidgetInputSounds.reportPointerClickFeedback(
                this.valueChangedSincePointerUpdate()
                        ? UiActionFeedback.CHAR
                        : UiActionFeedback.NONE
        );
    }

    @Inject(method = "onDrag", at = @At("HEAD"))
    private void javareforged$captureDragValue(
            MouseButtonEvent event,
            double dragX,
            double dragY,
            CallbackInfo ci
    ) {
        this.javareforged$valueBeforePointerUpdate = this.value;
    }

    @Inject(method = "onDrag", at = @At("TAIL"))
    private void javareforged$playDragValueFeedback(
            MouseButtonEvent event,
            double dragX,
            double dragY,
            CallbackInfo ci
    ) {
        if (this.valueChangedSincePointerUpdate()) {
            WidgetInputSounds.playPointerDragFeedback(UiActionFeedback.CHAR);
        }
    }

    @Unique
    private boolean valueChangedSincePointerUpdate() {
        return Double.compare(this.javareforged$valueBeforePointerUpdate, this.value) != 0;
    }
}
