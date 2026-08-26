package net.jr.mixin.runtime;

import net.jr.client.runtime.viewport.GuiViewportScope;
import net.jr.client.runtime.viewport.ViewportGuiDimensions;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin {
    @ModifyVariable(method = "containsPointInScissor", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int javaReforged$mapScissorTestXToSharedGui(int x) {
        return GuiViewportScope.mapScissorTestX(x);
    }

    @ModifyVariable(method = "containsPointInScissor", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int javaReforged$mapScissorTestYToSharedGui(int y) {
        return GuiViewportScope.mapScissorTestY(y);
    }

    @Inject(method = "blurBeforeThisStratum", at = @At("HEAD"), cancellable = true)
    private void javaReforged$keepFrameGlobalBlurOutOfLocalViewports(CallbackInfo callback) {
        if (ViewportGuiDimensions.suppressFrameGlobalBlur()) {
            callback.cancel();
        }
    }

    @ModifyArgs(
        method = "entity",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/gui/pip/GuiEntityRenderState;<init>(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lorg/joml/Vector3fc;Lorg/joml/Quaternionfc;Lorg/joml/Quaternionfc;IIIIFLnet/minecraft/client/gui/navigation/ScreenRectangle;)V")
    )
    private void javaReforged$mapEntityPictureInPictureToViewport(Args args) {
        javaReforged$applyPictureInPictureBounds(args, 4, 5, 6, 7, 8);
    }

    @ModifyArgs(
        method = "skin",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/gui/pip/GuiSkinRenderState;<init>(Lnet/minecraft/client/model/Model$Simple;Lnet/minecraft/resources/Identifier;FFFIIIIFLnet/minecraft/client/gui/navigation/ScreenRectangle;)V")
    )
    private void javaReforged$mapSkinPictureInPictureToViewport(Args args) {
        javaReforged$applyPictureInPictureBounds(args, 5, 6, 7, 8, 4);
    }

    @ModifyArgs(
        method = "book",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/gui/pip/GuiBookModelRenderState;<init>(Lnet/minecraft/client/model/object/book/BookModel;Lnet/minecraft/resources/Identifier;FFIIIIFLnet/minecraft/client/gui/navigation/ScreenRectangle;)V")
    )
    private void javaReforged$mapBookPictureInPictureToViewport(Args args) {
        javaReforged$applyPictureInPictureBounds(args, 4, 5, 6, 7, 3);
    }

    @ModifyArgs(
        method = "bannerPattern",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/gui/pip/GuiBannerResultRenderState;<init>(Lnet/minecraft/client/model/object/banner/BannerFlagModel;Lnet/minecraft/world/item/DyeColor;Lnet/minecraft/world/level/block/entity/BannerPatternLayers;IIIILnet/minecraft/client/gui/navigation/ScreenRectangle;)V")
    )
    private void javaReforged$mapBannerPictureInPictureToViewport(Args args) {
        javaReforged$applyPictureInPictureBounds(args, 3, 4, 5, 6, -1);
    }

    @ModifyArgs(
        method = "profilerChart",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/gui/pip/GuiProfilerChartRenderState;<init>(Ljava/util/List;IIIILnet/minecraft/client/gui/navigation/ScreenRectangle;)V")
    )
    private void javaReforged$mapProfilerPictureInPictureToViewport(Args args) {
        javaReforged$applyPictureInPictureBounds(args, 1, 2, 3, 4, -1);
    }

    private static void javaReforged$applyPictureInPictureBounds(
            Args args,
            int x0Index,
            int y0Index,
            int x1Index,
            int y1Index,
            int scaleIndex
    ) {
        float scale = scaleIndex >= 0 ? args.get(scaleIndex) : 1.0F;
        GuiViewportScope.PictureInPictureBounds bounds = GuiViewportScope.mapPictureInPicture(
                args.get(x0Index), args.get(y0Index), args.get(x1Index), args.get(y1Index), scale
        );
        args.set(x0Index, bounds.x0());
        args.set(y0Index, bounds.y0());
        args.set(x1Index, bounds.x1());
        args.set(y1Index, bounds.y1());
        if (scaleIndex >= 0) {
            args.set(scaleIndex, bounds.scale());
        }
    }

    @Inject(method = "guiWidth", at = @At("HEAD"), cancellable = true)
    private void javaReforged$useViewportGuiWidth(CallbackInfoReturnable<Integer> callback) {
        Integer width = ViewportGuiDimensions.activeWidthOrNull();
        if (width != null) {
            callback.setReturnValue(width);
        }
    }

    @Inject(method = "guiHeight", at = @At("HEAD"), cancellable = true)
    private void javaReforged$useViewportGuiHeight(CallbackInfoReturnable<Integer> callback) {
        Integer height = ViewportGuiDimensions.activeHeightOrNull();
        if (height != null) {
            callback.setReturnValue(height);
        }
    }
}
