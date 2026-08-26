package net.jr.mixin.runtime;

import net.jr.client.runtime.render.target.TextureCopyBounds;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlCommandEncoder")
public abstract class GlCommandEncoderMixin {
    @ModifyArgs(
        method = "copyTextureToTexture",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/opengl/DirectStateAccess;blitFrameBuffers(IIIIIIIIIIII)V"
        )
    )
    private void javaReforged$convertCopyExtentsToBlitCorners(Args args) {
        int sourceX = args.get(2);
        int sourceY = args.get(3);
        int destinationX = args.get(6);
        int destinationY = args.get(7);
        int width = args.get(4);
        int height = args.get(5);
        TextureCopyBounds.Corners corners = TextureCopyBounds.presentingCorners(
                sourceX, sourceY, destinationX, destinationY, width, height
        );
        if (corners != null) {
            args.set(4, corners.sourceRight());
            args.set(5, corners.sourceBottom());
            args.set(8, corners.destinationRight());
            args.set(9, corners.destinationBottom());
        }
    }
}
