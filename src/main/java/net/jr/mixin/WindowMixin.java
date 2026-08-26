package net.jr.mixin;

import com.mojang.blaze3d.platform.Window;
import net.jr.client.ui.presentation.WindowScalePolicy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Window.class)
public abstract class WindowMixin {
    private static final int VANILLA_BASE_WIDTH = 320;
    private static final int VANILLA_BASE_HEIGHT = 240;
    private static final int SMALL_BASE_WIDTH = 280;
    private static final int SMALL_BASE_HEIGHT = 200;

    @ModifyConstant(
            method = "calculateScale",
            constant = @Constant(intValue = VANILLA_BASE_WIDTH)
    )
    private int javaReforged$modifyBaseWidth(int original) {
        return WindowScalePolicy.useCompactMetrics() ? SMALL_BASE_WIDTH : VANILLA_BASE_WIDTH;
    }

    @ModifyConstant(
            method = "calculateScale",
            constant = @Constant(intValue = VANILLA_BASE_HEIGHT)
    )
    private int javaReforged$modifyBaseHeight(int original) {
        return WindowScalePolicy.useCompactMetrics() ? SMALL_BASE_HEIGHT : VANILLA_BASE_HEIGHT;
    }
}
