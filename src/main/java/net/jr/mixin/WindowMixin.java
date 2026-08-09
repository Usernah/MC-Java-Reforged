package net.jr.mixin;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Window.class)
public class WindowMixin {

    @ModifyConstant(
            method = "calculateScale",
            constant = @Constant(intValue = 320)
    )
    private int javaReforged$modifyBaseWidth(int original) {
        return 280;
    }

    @ModifyConstant(
            method = "calculateScale",
            constant = @Constant(intValue = 240)
    )
    private int javaReforged$modifyBaseHeight(int original) {
        return 200;
    }
}