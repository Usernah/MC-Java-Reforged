package net.jr.mixin;

import net.jr.client.ui.cursor.ScreenMousePositionResolver;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @ModifyVariable(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int javaReforged$resolveMouseX(int mouseX) {
        return ScreenMousePositionResolver.x(mouseX);
    }

    @ModifyVariable(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int javaReforged$resolveMouseY(int mouseY) {
        return ScreenMousePositionResolver.y(mouseY);
    }
}
