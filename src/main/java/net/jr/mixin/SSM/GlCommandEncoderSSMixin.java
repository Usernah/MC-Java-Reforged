package net.jr.mixin.SSM;

import net.jr.ClientRuntime.runtime.SlotRenderTargets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Corrects OpenGL's extent-to-corner conversion for split target copies. */
@Mixin(targets = "com.mojang.blaze3d.opengl.GlCommandEncoder")
public abstract class GlCommandEncoderSSMixin {
    @ModifyArgs(
        method = "copyTextureToTexture",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/opengl/DirectStateAccess;blitFrameBuffers(IIIIIIIIIIII)V"
        )
    )
    private void splitTest$convertCopyExtentsToBlitCorners(Args args) {
        if (!SlotRenderTargets.isPresenting()) {
            return;
        }

        int sourceX = args.get(2);
        int sourceY = args.get(3);
        int destinationX = args.get(6);
        int destinationY = args.get(7);
        int width = args.get(4);
        int height = args.get(5);
        args.set(4, sourceX + width);
        args.set(5, sourceY + height);
        args.set(8, destinationX + width);
        args.set(9, destinationY + height);
    }
}
