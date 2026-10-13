package net.jr.mixin;

import net.jr.client.ui.presentation.RenderOptimizationOptionsHandler;
import net.jr.client.ui.presentation.VideoOptionsHandler;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VideoSettingsScreen.class)
public abstract class VideoSettingsScreenMixin extends OptionsSubScreen {
    private VideoSettingsScreenMixin() {
        super(null, null, null);
    }

    @Inject(method = "displayOptions", at = @At("RETURN"), cancellable = true)
    private static void javaReforged$extendDisplayOptions(
            net.minecraft.client.Options options,
            CallbackInfoReturnable<OptionInstance<?>[]> cir
    ) {
        cir.setReturnValue(VideoOptionsHandler.extend(options, cir.getReturnValue()));
    }

    @Inject(method = "qualityOptions", at = @At("RETURN"), cancellable = true)
    private static void javaReforged$extendQualityOptions(
            net.minecraft.client.Options options,
            CallbackInfoReturnable<OptionInstance<?>[]> cir
    ) {
        cir.setReturnValue(RenderOptimizationOptionsHandler.extendQuality(options, cir.getReturnValue()));
    }
}
