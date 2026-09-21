package net.jr.mixin;

import net.minecraft.client.gui.render.GuiItemAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * All local viewports contribute item models to the same vanilla GUI atlas.
 * Four HUDs plus an item-heavy screen exceed the vanilla 512 px minimum.
 */
@Mixin(GuiItemAtlas.class)
public abstract class GuiItemAtlasMixin {
    private static final int SPLIT_SCREEN_MINIMUM_ATLAS_SIZE = 1024;

    @ModifyConstant(
        method = "computeTextureSizeFor",
        constant = @Constant(intValue = 512)
    )
    private static int javaReforged$expandSharedItemAtlas(int vanillaMinimum) {
        return SPLIT_SCREEN_MINIMUM_ATLAS_SIZE;
    }
}
