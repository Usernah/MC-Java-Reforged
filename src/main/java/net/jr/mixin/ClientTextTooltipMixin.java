package net.jr.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.jr.client.ui.presentation.UiPresentation;
import net.jr.client.ui.tooltip.TooltipPresentation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientTextTooltip.class)
public class ClientTextTooltipMixin {
    @Shadow
    @Final
    @Mutable
    private FormattedCharSequence text;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void javaReforged$applyFont(
            FormattedCharSequence original,
            CallbackInfo ci
    ) {
        var font = TooltipPresentation.font()
                .location()
                .asFontDescription();

        this.text = sink -> original.accept(
                (index, style, codePoint) ->
                        sink.accept(
                                index,
                                style.withFont(font),
                                codePoint
                        )
        );
    }

    @ModifyReturnValue(method = "getHeight", at = @At("RETURN"))
    private int getHeightForFont(int original, Font font) {
        return UiPresentation.isSmall() ? font.lineHeight + 4 : font.lineHeight;
    }

}
