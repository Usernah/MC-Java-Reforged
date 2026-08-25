package net.jr.mixin;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import net.jr.client.render.GuiGraphicsExtractorBridge;
import net.jr.client.render.ItemRenderResolution;
import net.jr.client.runtime.viewport.PreciseGuiScissor;
import net.jr.client.runtime.viewport.PreciseScissorAccess;
import net.jr.client.runtime.viewport.ViewportGuiScale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2f;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorScissorMixin
        implements GuiGraphicsExtractorBridge {

    @Shadow
    @Final
    private Matrix3x2fStack pose;

    @Unique
    private final Deque<ScreenRectangle> javaReforged$scissors =
            new ArrayDeque<>();

    @Unique
    private final Deque<PreciseGuiScissor> javaReforged$preciseScissors =
            new ArrayDeque<>();

    @Inject(method = "enableScissor", at = @At("TAIL"))
    private void javaReforged$pushScissor(
            int left,
            int top,
            int right,
            int bottom,
            CallbackInfo callbackInfo
    ) {

        ScreenRectangle transformed =
                new ScreenRectangle(
                        left,
                        top,
                        right - left,
                        bottom - top
                ).transformAxisAligned(this.pose);

        ScreenRectangle previous =
                this.javaReforged$scissors.peekLast();

        ScreenRectangle resolved =
                previous == null
                        ? transformed
                        : Objects.requireNonNullElse(
                        transformed.intersection(previous),
                        ScreenRectangle.empty()
                );

        this.javaReforged$scissors.addLast(resolved);

        if (!ViewportGuiScale.isActive()) {
            return;
        }

        this.javaReforged$pushPreciseScissor(left, top, right, bottom);
    }

    @Inject(method = "disableScissor", at = @At("TAIL"))
    private void javaReforged$popScissor(
            CallbackInfo callbackInfo
    ) {
        this.javaReforged$scissors.removeLast();

        if (ViewportGuiScale.isActive()
                && !this.javaReforged$preciseScissors.isEmpty()) {

            this.javaReforged$preciseScissors.removeLast();
        }
    }

    @Override
    @Nullable
    public ScreenRectangle javaReforged$currentScissor() {

        return ((GuiGraphicsExtractor)(Object)this).peekScissorStack();
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
        GuiGraphicsExtractor graphics = (GuiGraphicsExtractor)(Object)this;
        ItemRenderResolution.drawAtSize(size, () -> {
            if (fakeItem) {
                graphics.fakeItem(stack, x, y, seed);
            } else {
                graphics.item(stack, x, y, seed);
            }
        });
    }

    @Override
    public void javaReforged$renderItemStackAtSize(
            ItemStack stack,
            int x,
            int y,
            int seed,
            int size,
            boolean fakeItem,
            @Nullable net.minecraft.client.gui.Font decorationFont,
            @Nullable String countText
    ) {
        GuiGraphicsExtractor graphics = (GuiGraphicsExtractor)(Object)this;
        this.javaReforged$renderItemAtSize(stack, x, y, seed, size, fakeItem);
        if (decorationFont != null) {
            graphics.itemDecorations(decorationFont, stack, x, y, countText);
        }
    }

    @Override
    public void javaReforged$enablePreciseScissor(
            double left,
            double top,
            double right,
            double bottom
    ) {
        int fallbackLeft = (int)Math.floor(left);
        int fallbackTop = (int)Math.floor(top);
        int fallbackRight = (int)Math.ceil(right);
        int fallbackBottom = (int)Math.ceil(bottom);

        GuiGraphicsExtractor graphics = (GuiGraphicsExtractor)(Object)this;
        graphics.enableScissor(
                fallbackLeft,
                fallbackTop,
                fallbackRight,
                fallbackBottom
        );

        if (!ViewportGuiScale.isActive()) {
            return;
        }

        this.javaReforged$preciseScissors.removeLast();
        this.javaReforged$pushPreciseScissor(left, top, right, bottom);
    }

    @Unique
    private void javaReforged$pushPreciseScissor(
            double left,
            double top,
            double right,
            double bottom
    ) {
        PreciseGuiScissor previousPrecise = this.javaReforged$preciseScissors.peekLast();
        ViewportGuiScale.Context viewportContext = ViewportGuiScale.activeOrNull();

        if (previousPrecise == null
                && viewportContext != null
                && left == 0.0D
                && top == 0.0D
                && right == viewportContext.logicalWidth()
                && bottom == viewportContext.logicalHeight()) {

            var viewport = viewportContext.viewport();
            this.javaReforged$preciseScissors.addLast(new PreciseGuiScissor(
                    viewport.x(),
                    viewport.y(),
                    viewport.x() + viewport.width(),
                    viewport.y() + viewport.height()
            ));

            ScreenRectangle vanillaScissor = ((GuiGraphicsExtractor)(Object)this).peekScissorStack();
            if (vanillaScissor != null) {
                ((PreciseScissorAccess)(Object)vanillaScissor)
                        .javaReforged$setPreciseScissor(
                                this.javaReforged$preciseScissors.peekLast()
                        );
            }
            return;
        }

        Vector2f p0 = this.pose.transformPosition(
                (float)left,
                (float)top,
                new Vector2f()
        );
        Vector2f p1 = this.pose.transformPosition(
                (float)right,
                (float)bottom,
                new Vector2f()
        );
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        PreciseGuiScissor precise = new PreciseGuiScissor(
                Math.min(p0.x, p1.x) * guiScale,
                Math.min(p0.y, p1.y) * guiScale,
                Math.max(p0.x, p1.x) * guiScale,
                Math.max(p0.y, p1.y) * guiScale
        );

        if (previousPrecise != null) {
            precise = precise.intersection(previousPrecise);
        }
        this.javaReforged$preciseScissors.addLast(precise);

        ScreenRectangle vanillaScissor = ((GuiGraphicsExtractor)(Object)this).peekScissorStack();
        if (vanillaScissor != null) {
            ((PreciseScissorAccess)(Object)vanillaScissor)
                    .javaReforged$setPreciseScissor(precise);
        }
    }
}
