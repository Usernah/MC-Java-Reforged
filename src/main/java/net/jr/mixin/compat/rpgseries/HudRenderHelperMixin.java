package net.jr.mixin.compat.rpgseries;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.platform.Window;
import net.jr.api.client.ui.Viewport;
import net.jr.client.compat.rpgseries.SpellEngineRuntimeState;
import net.jr.client.runtime.viewport.ViewportHudLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.spell_engine.client.gui.HudRenderHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = HudRenderHelper.class, remap = false)
abstract class HudRenderHelperMixin {
    @WrapMethod(method = "render(Lnet/minecraft/client/gui/GuiGraphicsExtractor;FZ)V")
    private static void javaReforged$renderCurrentSpellEngineState(
        GuiGraphicsExtractor graphics,
        float tickDelta,
        boolean config,
        Operation<Void> original
    ) {
        try (SpellEngineRuntimeState.Scope ignored = SpellEngineRuntimeState.activateCurrent()) {
            original.call(graphics, tickDelta, config);
        }
    }

    @Redirect(
        method = "render(Lnet/minecraft/client/gui/GuiGraphicsExtractor;FZ)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledWidth()I"
        )
    )
    private static int javaReforged$viewportWidth(Window window) {
        return Viewport.width();
    }

    @Redirect(
        method = "render(Lnet/minecraft/client/gui/GuiGraphicsExtractor;FZ)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledHeight()I"
        )
    )
    private static int javaReforged$viewportHeight(Window window) {
        return Viewport.height();
    }

    @ModifyArg(
        method = "render(Lnet/minecraft/client/gui/GuiGraphicsExtractor;FZ)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/spell_engine/client/gui/HudRenderHelper$SpellHotBarWidget;render(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IILnet/spell_engine/client/gui/HudRenderHelper$SpellHotBarWidget$ViewModel;)V"
        ),
        index = 2
    )
    private static int javaReforged$hotbarBottomAnchor(int viewportHeight) {
        return ViewportHudLayout.bottomAnchorY(viewportHeight);
    }
}
