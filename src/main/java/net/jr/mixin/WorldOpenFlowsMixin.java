package net.jr.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.jr.screens.MessageScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WorldOpenFlows.class)
public abstract class WorldOpenFlowsMixin {
    @WrapOperation(
            method = {
                    "createFreshLevel",
                    "openWorld",
                    "openWorldLoadLevelData",
                    "openWorldLoadLevelStem"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;setScreenAndShow(Lnet/minecraft/client/gui/screens/Screen;)V"
            )
    )
    private void javaReforged$useMessageScreen(
            Minecraft minecraft,
            Screen requestedScreen,
            Operation<Void> original
    ) {
        if (!(requestedScreen instanceof GenericMessageScreen)) {
            original.call(minecraft, requestedScreen);
            return;
        }

        if (minecraft.gui.screen() instanceof MessageScreen messageScreen) {
            messageScreen.setMessage(requestedScreen.getTitle());
            minecraft.renderFrame(false);
            return;
        }

        original.call(minecraft, new MessageScreen(requestedScreen.getTitle()));
    }
}
