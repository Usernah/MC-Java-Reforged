package net.jr.mixin.compat.curios;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "top.theillusivec4.curios.client.screen.CuriosScreenEvents", remap = false)
abstract class CuriosScreenEventsMixin {
    @Definition(
        id = "InventoryScreen",
        type = net.minecraft.client.gui.screens.inventory.InventoryScreen.class
    )
    @Expression("? instanceof InventoryScreen")
    @ModifyExpressionValue(
        method = "postScreenInit",
        at = @At("MIXINEXTRAS:EXPRESSION")
    )
    private boolean javaReforged$acceptInventoryScreen(
        boolean original,
        ScreenEvent.Init.Post event
    ) {
        return original || event.getScreen() instanceof net.jr.screens.InventoryScreen;
    }

    @ModifyExpressionValue(
        method = "postScreenInit",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/Integer;intValue()I",
            ordinal = 0
        )
    )
    private int javaReforged$moveInventoryButton(
        int original,
        ScreenEvent.Init.Post event
    ) {
        return event.getScreen() instanceof net.jr.screens.InventoryScreen
            ? original + 39
            : original;
    }
}
