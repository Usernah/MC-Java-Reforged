package net.jr.mixin;

import com.mojang.blaze3d.platform.Window;
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
        return SMALL_BASE_WIDTH;
    }

    @ModifyConstant(
            method = "calculateScale",
            constant = @Constant(intValue = VANILLA_BASE_HEIGHT)
    )
    private int javaReforged$modifyBaseHeight(int original) {
        return SMALL_BASE_HEIGHT;
    }
}
