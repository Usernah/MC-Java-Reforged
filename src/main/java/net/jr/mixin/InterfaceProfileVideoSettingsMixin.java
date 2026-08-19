package net.jr.mixin;

import java.util.Arrays;
import net.jr.client.ui.presentation.InterfaceProfile;
import net.jr.client.ui.presentation.InterfaceProfileOption;
import net.jr.client.ui.presentation.SplitOrientationOption;
import net.jr.client.ui.presentation.UiPresentation;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotScope;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VideoSettingsScreen.class)
public abstract class InterfaceProfileVideoSettingsMixin extends OptionsSubScreen {
    private InterfaceProfileVideoSettingsMixin() {
        super(null, null, null);
    }

    @Inject(method = "displayOptions", at = @At("RETURN"), cancellable = true)
    private static void javaReforged$addInterfaceProfile(
        net.minecraft.client.Options options,
        CallbackInfoReturnable<OptionInstance<?>[]> cir
    ) {
        OptionInstance<?>[] original = cir.getReturnValue();
        OptionInstance<?> playerGuiScale = ClientRuntime.INSTANCE.slots()
            .slot(SlotScope.idOrNull() == null ? 0 : SlotScope.requireId())
            .optionsState()
            .guiScale();
        OptionInstance<?>[] extended = Arrays.copyOf(original, original.length + 2);
        for (int index = 0; index < original.length; index++) {
            if (original[index] == options.guiScale()) {
                extended[index] = playerGuiScale;
                break;
            }
        }
        extended[original.length] = InterfaceProfileOption.INSTANCE;
        extended[original.length + 1] = SplitOrientationOption.INSTANCE;
        cir.setReturnValue(extended);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void javaReforged$lockProfileDuringSplit(CallbackInfo ci) {
        if (this.list == null) {
            return;
        }
        AbstractWidget widget = this.list.findOption(InterfaceProfileOption.INSTANCE);
        if (widget == null) {
            return;
        }
        boolean split = UiPresentation.isSplitScreen();
        widget.active = !split;
        if (widget instanceof CycleButton<?> rawButton) {
            @SuppressWarnings("unchecked")
            CycleButton<InterfaceProfile> button = (CycleButton<InterfaceProfile>)rawButton;
            button.setValue(split ? InterfaceProfile.STANDARD : InterfaceProfileOption.INSTANCE.get());
        }
    }
}
