package net.jr.mixin.compat.createcatnip;

import net.jr.client.runtime.context.LocalClientExecution;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
        targets = "net.createmod.catnip.api.client.gui.ScreenOpener",
        remap = false
)
abstract class ScreenOpenerMixin {

    @ModifyArg(
            method = "openScreen",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;schedule(Ljava/lang/Runnable;)V"
            ),
            index = 0
    )
    private static Runnable javaReforged$captureScreenOwner(Runnable task) {
        return LocalClientExecution.wrapScheduled(task);
    }

    @Redirect(
            method = "lambda$openScreen$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;setScreenAndShow(Lnet/minecraft/client/gui/screens/Screen;)V"
            )
    )
    private static void javaReforged$openWithoutGlobalForcedFrame(
            Minecraft minecraft,
            Screen screen
    ) {
        minecraft.gui.setScreen(screen);
    }
}
