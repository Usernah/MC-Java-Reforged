package net.jr.mixin.runtime;

import net.jr.client.runtime.render.pass.HudRenderPass;
import net.jr.client.runtime.viewport.GuiViewportScope;
import net.jr.client.runtime.viewport.ViewportGuiScale;
import net.jr.client.runtime.ClientRuntime;
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
public abstract class GuiGraphicsExtractorSSMixin {
    @ModifyVariable(method = "containsPointInScissor", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int splitTest$mapScissorTestXToSharedGui(int x) {
        return GuiViewportScope.mapScissorTestX(x);
    }

    @ModifyVariable(method = "containsPointInScissor", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int splitTest$mapScissorTestYToSharedGui(int y) {
        return GuiViewportScope.mapScissorTestY(y);
    }

    @Inject(method = "blurBeforeThisStratum", at = @At("HEAD"), cancellable = true)
    private void splitTest$keepFrameGlobalBlurOutOfLocalViewports(CallbackInfo callback) {
        if (ClientRuntime.INSTANCE.viewports().presentedCount() > 1) {
            callback.cancel();
        }
    }

    @ModifyArgs(
        method = "entity",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/gui/pip/GuiEntityRenderState;<init>(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lorg/joml/Vector3fc;Lorg/joml/Quaternionfc;Lorg/joml/Quaternionfc;IIIIFLnet/minecraft/client/gui/navigation/ScreenRectangle;)V")
    )
    private void splitTest$mapEntityPictureInPictureToViewport(Args args) {
        splitTest$mapPictureInPictureBounds(args, 4, 5, 6, 7, 8);
    }

    @ModifyArgs(
        method = "skin",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/gui/pip/GuiSkinRenderState;<init>(Lnet/minecraft/client/model/Model$Simple;Lnet/minecraft/resources/Identifier;FFFIIIIFLnet/minecraft/client/gui/navigation/ScreenRectangle;)V")
    )
    private void splitTest$mapSkinPictureInPictureToViewport(Args args) {
        splitTest$mapPictureInPictureBounds(args, 5, 6, 7, 8, 4);
    }

    @ModifyArgs(
        method = "book",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/gui/pip/GuiBookModelRenderState;<init>(Lnet/minecraft/client/model/object/book/BookModel;Lnet/minecraft/resources/Identifier;FFIIIIFLnet/minecraft/client/gui/navigation/ScreenRectangle;)V")
    )
    private void splitTest$mapBookPictureInPictureToViewport(Args args) {
        splitTest$mapPictureInPictureBounds(args, 4, 5, 6, 7, 3);
    }

    @ModifyArgs(
        method = "bannerPattern",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/gui/pip/GuiBannerResultRenderState;<init>(Lnet/minecraft/client/model/object/banner/BannerFlagModel;Lnet/minecraft/world/item/DyeColor;Lnet/minecraft/world/level/block/entity/BannerPatternLayers;IIIILnet/minecraft/client/gui/navigation/ScreenRectangle;)V")
    )
    private void splitTest$mapBannerPictureInPictureToViewport(Args args) {
        splitTest$mapPictureInPictureBounds(args, 3, 4, 5, 6);
    }

    @ModifyArgs(
        method = "profilerChart",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/state/gui/pip/GuiProfilerChartRenderState;<init>(Ljava/util/List;IIIILnet/minecraft/client/gui/navigation/ScreenRectangle;)V")
    )
    private void splitTest$mapProfilerPictureInPictureToViewport(Args args) {
        splitTest$mapPictureInPictureBounds(args, 1, 2, 3, 4);
    }

    private static void splitTest$mapPictureInPictureBounds(Args args, int x0Index, int y0Index, int x1Index, int y1Index, int scaleIndex) {
        args.set(x0Index, GuiViewportScope.mapPictureInPictureX(args.get(x0Index)));
        args.set(y0Index, GuiViewportScope.mapPictureInPictureY(args.get(y0Index)));
        args.set(x1Index, GuiViewportScope.mapPictureInPictureX(args.get(x1Index)));
        args.set(y1Index, GuiViewportScope.mapPictureInPictureY(args.get(y1Index)));
        args.set(scaleIndex, GuiViewportScope.mapPictureInPictureScale(args.get(scaleIndex)));
    }

    private static void splitTest$mapPictureInPictureBounds(Args args, int x0Index, int y0Index, int x1Index, int y1Index) {
        args.set(x0Index, GuiViewportScope.mapPictureInPictureX(args.get(x0Index)));
        args.set(y0Index, GuiViewportScope.mapPictureInPictureY(args.get(y0Index)));
        args.set(x1Index, GuiViewportScope.mapPictureInPictureX(args.get(x1Index)));
        args.set(y1Index, GuiViewportScope.mapPictureInPictureY(args.get(y1Index)));
    }

    @Inject(method = "guiWidth", at = @At("HEAD"), cancellable = true)
    private void splitTest$useViewportGuiWidth(CallbackInfoReturnable<Integer> callback) {
        Integer screenWidth = ViewportGuiScale.activeGuiWidthOrNull();
        if (screenWidth != null) {
            callback.setReturnValue(screenWidth);
            return;
        }
        Integer width = HudRenderPass.guiWidthOrNull();
        if (width != null) {
            callback.setReturnValue(width);
        }
    }

    @Inject(method = "guiHeight", at = @At("HEAD"), cancellable = true)
    private void splitTest$useViewportGuiHeight(CallbackInfoReturnable<Integer> callback) {
        Integer screenHeight = ViewportGuiScale.activeGuiHeightOrNull();
        if (screenHeight != null) {
            callback.setReturnValue(screenHeight);
            return;
        }
        Integer height = HudRenderPass.guiHeightOrNull();
        if (height != null) {
            callback.setReturnValue(height);
        }
    }
}
