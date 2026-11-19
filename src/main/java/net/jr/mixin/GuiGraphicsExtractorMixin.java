package net.jr.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.jr.client.render.item.GuiItemExtractionHandler;
import net.jr.client.render.item.GuiItemRenderPass;
import net.jr.client.render.GuiGraphicsExtractorBridge;
import net.jr.client.runtime.viewport.GuiScissorHandler;
import net.jr.client.runtime.viewport.GuiScissorState;
import net.jr.client.ui.tooltip.TooltipPresentation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin implements GuiGraphicsExtractorBridge {
    @Shadow
    private GuiRenderState guiRenderState;

    @Unique
    private final GuiScissorState javaReforged$scissorState = new GuiScissorState();

    @Inject(method = "enableScissor", at = @At("TAIL"))
    private void javaReforged$trackScissor(
            int left,
            int top,
            int right,
            int bottom,
            CallbackInfo ci
    ) {
        GuiScissorHandler.enabled(this.javaReforged$graphics(), this.javaReforged$scissorState, left, top, right, bottom);
    }

    @Inject(method = "disableScissor", at = @At("TAIL"))
    private void javaReforged$releaseScissor(CallbackInfo ci) {
        GuiScissorHandler.disabled(this.javaReforged$scissorState);
    }

    @ModifyArg(
            method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/state/gui/GuiRenderState;addItem(Lnet/minecraft/client/renderer/state/gui/GuiItemRenderState;)V"
            ),
            index = 0
    )
    private GuiItemRenderState javaReforged$captureItemState(GuiItemRenderState state) {
        return GuiItemRenderPass.capture(state);
    }

    @Override
    public GuiRenderState javaReforged$getGuiRenderState() {
        return this.guiRenderState;
    }

    @Override
    @Nullable
    public ScreenRectangle javaReforged$currentScissor() {
        return this.javaReforged$graphics().peekScissorStack();
    }

    @Override
    public void javaReforged$enablePreciseScissor(double left, double top, double right, double bottom) {
        GuiScissorHandler.enablePrecise(
                this.javaReforged$graphics(),
                this.javaReforged$scissorState,
                left,
                top,
                right,
                bottom
        );
    }

    @Override
    public void javaReforged$renderItemAtSize(
            ItemStack stack,
            int x,
            int y,
            int seed,
            int size,
            boolean fakeItem
    ) {
        GuiItemExtractionHandler.renderAtSize(this.javaReforged$graphics(), stack, x, y, seed, size, fakeItem);
    }

    @Override
    public void javaReforged$renderItemStackAtSize(
            ItemStack stack,
            int x,
            int y,
            int seed,
            int size,
            boolean fakeItem,
            @Nullable Font decorationFont,
            @Nullable String countText
    ) {
        GuiItemExtractionHandler.renderStackAtSize(
                this.javaReforged$graphics(),
                stack,
                x,
                y,
                seed,
                size,
                fakeItem,
                decorationFont,
                countText
        );
    }

    @Override
    public void javaReforged$renderCarriedItemStackAtSize(
            ItemStack stack,
            int x,
            int y,
            int seed,
            int size,
            boolean fakeItem,
            @Nullable Font decorationFont,
            @Nullable String countText
    ) {
        GuiItemExtractionHandler.renderCarriedStackAtSize(
                this.javaReforged$graphics(),
                stack,
                x,
                y,
                seed,
                size,
                fakeItem,
                decorationFont,
                countText
        );
    }

    @WrapMethod(method = "tooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;Lnet/minecraft/world/item/ItemStack;)V")
    private void javaReforged$scaleTooltip(
            Font font,
            List<ClientTooltipComponent> lines,
            int mouseX,
            int mouseY,
            ClientTooltipPositioner positioner,
            @Nullable Identifier style,
            ItemStack tooltipStack,
            Operation<Void> original
    ) {
        float scale = TooltipPresentation.scale();
        if (scale == 1.0F) {
            original.call(font, lines, mouseX, mouseY, positioner, style, tooltipStack);
            return;
        }

        GuiGraphicsExtractor graphics = this.javaReforged$graphics();
        ClientTooltipPositioner scaledPositioner = (
                screenWidth, screenHeight, x, y, tooltipWidth, tooltipHeight
        ) -> positioner.positionTooltip(
                Math.max(1, (int) Math.floor(screenWidth / scale)),
                Math.max(1, (int) Math.floor(screenHeight / scale)),
                x, y, tooltipWidth, tooltipHeight
        );

        graphics.pose().pushMatrix();
        try {
            graphics.pose().scale(scale, scale);
            original.call(
                    font,
                    lines,
                    Math.round(mouseX / scale),
                    Math.round(mouseY / scale),
                    scaledPositioner,
                    style,
                    tooltipStack
            );
        } finally {
            graphics.pose().popMatrix();
        }
    }

    @Unique
    private GuiGraphicsExtractor javaReforged$graphics() {
        return (GuiGraphicsExtractor) (Object) this;
    }
}
