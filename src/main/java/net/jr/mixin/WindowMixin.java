package net.jr.mixin;

import com.mojang.blaze3d.platform.Window;
import net.jr.ClientRuntime.runtime.LocalPlayers;
import net.jr.ClientRuntime.viewport.ViewportLayout;
import net.jr.client.ui.presentation.UiPresentation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Window.class)
public class WindowMixin {
    private static final int VANILLA_BASE_WIDTH = 320;
    private static final int VANILLA_BASE_HEIGHT = 240;
    private static final int SMALL_BASE_WIDTH = 280;
    private static final int SMALL_BASE_HEIGHT = 200;

    @ModifyConstant(
            method = "calculateScale",
            constant = @Constant(intValue = VANILLA_BASE_WIDTH)
    )
    private int javaReforged$modifyBaseWidth(int original) {
        return javaReforged$useSmallMetrics() ? SMALL_BASE_WIDTH : VANILLA_BASE_WIDTH;
    }

    @ModifyConstant(
            method = "calculateScale",
            constant = @Constant(intValue = VANILLA_BASE_HEIGHT)
    )
    private int javaReforged$modifyBaseHeight(int original) {
        return javaReforged$useSmallMetrics() ? SMALL_BASE_HEIGHT : VANILLA_BASE_HEIGHT;
    }

    private static boolean javaReforged$useSmallMetrics() {
        if (UiPresentation.isPortable()) {
            return true;
        }
        if (!UiPresentation.isSplitScreen()) {
            return false;
        }
        ViewportLayout layout = LocalPlayers.INSTANCE.slots().layout();
        return layout == ViewportLayout.TWO_HORIZONTAL || layout == ViewportLayout.FOUR_GRID;
    }
}
