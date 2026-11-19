package net.jr.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.jr.client.render.item.GuiItemFrameState;
import net.jr.client.render.item.GuiItemRenderPass;
import net.jr.client.ui.render.composition.GuiCompositionQueue;
import net.jr.client.ui.render.composition.GuiCompositionScope;
import net.minecraft.client.gui.render.GuiItemAtlas;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.function.Consumer;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
    @Unique
    private final GuiItemFrameState javaReforged$itemFrameState = new GuiItemFrameState();

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/profiling/Profiler;get()Lnet/minecraft/util/profiling/ProfilerFiller;",
            shift = At.Shift.BEFORE
        )
    )
    private void javaReforged$renderLiveLayoutTargets(CallbackInfo ci) {
        if (GuiCompositionScope.activeRenderingOrNull() == null) {
            GuiCompositionQueue.renderQueued();
        }
    }

    @ModifyExpressionValue(
        method = "draw",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/state/WindowRenderState;width:I"
        )
    )
    private int javaReforged$compositionFramebufferWidth(int original) {
        GuiCompositionScope.Rendering rendering = GuiCompositionScope.activeRenderingOrNull();
        return rendering == null ? original : rendering.framebufferWidth();
    }

    @ModifyExpressionValue(
        method = "draw",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/state/WindowRenderState;height:I"
        )
    )
    private int javaReforged$compositionFramebufferHeight(int original) {
        GuiCompositionScope.Rendering rendering = GuiCompositionScope.activeRenderingOrNull();
        return rendering == null ? original : rendering.framebufferHeight();
    }

    @ModifyExpressionValue(
        method = {"draw", "preparePictureInPicture", "getGuiScaleInvalidatingItemAtlasIfChanged"},
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/state/WindowRenderState;guiScale:I"
        )
    )
    private int javaReforged$compositionGuiScale(int original) {
        GuiCompositionScope.Rendering rendering = GuiCompositionScope.activeRenderingOrNull();
        return rendering == null ? original : rendering.guiScale();
    }

    @Inject(method = "prepareItemElements", at = @At("HEAD"))
    private void javaReforged$prepareItemResolution(CallbackInfo ci) {
        GuiItemRenderPass.prepareFrame((GuiRenderer) (Object) this, this.javaReforged$itemFrameState);
    }

    @Redirect(
        method = "prepareItemElements",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/render/GuiRenderer;prepareItemAtlas(Ljava/util/Set;I)Lnet/minecraft/client/gui/render/GuiItemAtlas;"
        )
    )
    private GuiItemAtlas javaReforged$selectRepresentativeItemAtlas(
        GuiRenderer renderer,
        Set<Object> identities,
        int vanillaPixelSize
    ) {
        return GuiItemRenderPass.representative(
            this.javaReforged$itemFrameState,
            vanillaPixelSize
        );
    }

    @Redirect(
        method = "prepareItemElements",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/state/gui/GuiRenderState;forEachItem(Ljava/util/function/Consumer;)V",
            ordinal = 0
        )
    )
    private void javaReforged$submitItemsFromResolutionAtlases(
        GuiRenderState renderState,
        Consumer<GuiItemRenderState> vanillaConsumer
    ) {
        renderState.forEachItem(state -> {
            if (state.oversizedItemBounds() != null) {
                vanillaConsumer.accept(state);
                return;
            }
            GuiItemAtlas.SlotView slot = GuiItemRenderPass.getOrUpdate(
                this.javaReforged$itemFrameState,
                state
            );
            if (slot != null) {
                ((net.jr.mixin.accessors.GuiRendererAccessor) this)
                    .javaReforged$submitBlitFromItemAtlas(state, slot);
            }
        });
    }

    @Inject(method = "submitBlitFromItemAtlas", at = @At("HEAD"))
    private void javaReforged$readItemOpacity(
            GuiItemRenderState state,
            GuiItemAtlas.SlotView slot,
            CallbackInfo ci
    ) {
        GuiItemRenderPass.readOpacity(state, this.javaReforged$itemFrameState);
    }

    @ModifyConstant(method = "submitBlitFromItemAtlas", constant = @Constant(intValue = -1))
    private int javaReforged$applyItemOpacity(int original) {
        return this.javaReforged$itemFrameState.itemColor();
    }

    @Inject(method = "endFrame", at = @At("TAIL"))
    private void javaReforged$clearItemResolution(CallbackInfo ci) {
        GuiItemRenderPass.clearFrame(this.javaReforged$itemFrameState);
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void javaReforged$closeItemAtlases(CallbackInfo ci) {
        GuiItemRenderPass.close(this.javaReforged$itemFrameState);
    }
}
