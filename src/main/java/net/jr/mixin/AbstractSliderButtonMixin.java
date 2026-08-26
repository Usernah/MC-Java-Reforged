package net.jr.mixin;

import net.jr.client.components.navigation.SliderInteractionHandler;
import net.jr.client.components.navigation.SliderInteractionState;
import net.jr.client.components.navigation.UiAction;
import net.jr.client.components.navigation.UiActionFeedback;
import net.jr.client.components.navigation.UiActionHandler;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSliderButton.class)
public abstract class AbstractSliderButtonMixin implements UiActionHandler {
    @Unique
    private final SliderInteractionState javaReforged$interactionState = new SliderInteractionState();

    @Inject(method = "onDrag", at = @At("HEAD"))
    private void javaReforged$captureDragValue(
            MouseButtonEvent event,
            double dragX,
            double dragY,
            CallbackInfo ci
    ) {
        SliderInteractionHandler.capturePointerValue(this.javaReforged$slider(), this.javaReforged$interactionState);
    }

    @Inject(method = "onClick", at = @At("HEAD"))
    private void javaReforged$captureClickValue(
            MouseButtonEvent event,
            boolean doubleClick,
            CallbackInfo ci
    ) {
        SliderInteractionHandler.capturePointerValue(this.javaReforged$slider(), this.javaReforged$interactionState);
    }

    @Inject(method = "onClick", at = @At("TAIL"))
    private void javaReforged$reportPointerClick(
            MouseButtonEvent event,
            boolean doubleClick,
            CallbackInfo ci
    ) {
        SliderInteractionHandler.reportPointerClick(this.javaReforged$slider(), this.javaReforged$interactionState);
    }

    @Inject(method = "onDrag", at = @At("TAIL"))
    private void javaReforged$reportPointerDrag(
            MouseButtonEvent event,
            double dragX,
            double dragY,
            CallbackInfo ci
    ) {
        SliderInteractionHandler.reportPointerDrag(this.javaReforged$slider(), this.javaReforged$interactionState);
    }

    @Override
    public boolean handleUiAction(UiAction action) {
        return SliderInteractionHandler.handleUiAction(
                this.javaReforged$slider(),
                this.javaReforged$interactionState,
                action
        );
    }

    @Override
    public UiActionFeedback uiActionFeedback(UiAction action) {
        return this.javaReforged$interactionState.lastFeedback();
    }

    @Override
    public boolean prefersDirectFocusAction() {
        return true;
    }

    @Unique
    private AbstractSliderButton javaReforged$slider() {
        return (AbstractSliderButton) (Object) this;
    }
}
