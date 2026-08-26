package net.jr.mixin;

import net.jr.client.input.MouseInputHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onMove", at = @At("HEAD"), cancellable = true)
    private void javaReforged$trackMouseInputMode(
            long windowPointer,
            double x,
            double y,
            CallbackInfo ci
    ) {
        if (MouseInputHandler.handleMove(windowPointer, x, y)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "onButton",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/client/ClientHooks;onMouseButtonPre(Lnet/minecraft/client/input/MouseButtonInfo;I)Z"
            ),
            cancellable = true
    )
    private void javaReforged$consumeMappedScreenMouseButton(
            long windowPointer,
            MouseButtonInfo buttonInfo,
            int action,
            CallbackInfo ci
    ) {
        if (MouseInputHandler.handleButton(Minecraft.getInstance(), windowPointer, buttonInfo, action)) {
            ci.cancel();
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void javaReforged$consumeMappedScreenMouseScroll(
            long windowPointer,
            double xOffset,
            double yOffset,
            CallbackInfo ci
    ) {
        if (MouseInputHandler.handleScroll(Minecraft.getInstance(), windowPointer, xOffset, yOffset)) {
            ci.cancel();
        }
    }
}
