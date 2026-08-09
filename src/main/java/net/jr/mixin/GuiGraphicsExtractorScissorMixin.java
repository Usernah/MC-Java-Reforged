package net.jr.mixin;

import net.jr.client.render.GuiGraphicsExtractorBridge;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix3x2fStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorScissorMixin implements GuiGraphicsExtractorBridge {
    @Shadow
    @Final
    private Matrix3x2fStack pose;

    @Unique
    private final Deque<ScreenRectangle> javaReforged$scissors = new ArrayDeque<>();

    @Inject(method = "enableScissor", at = @At("TAIL"))
    private void javaReforged$pushScissor(int left, int top, int right, int bottom, CallbackInfo callbackInfo) {
        ScreenRectangle transformed = new ScreenRectangle(left, top, right - left, bottom - top)
            .transformAxisAligned(this.pose);
        ScreenRectangle previous = this.javaReforged$scissors.peekLast();
        ScreenRectangle resolved = previous == null
            ? transformed
            : Objects.requireNonNullElse(transformed.intersection(previous), ScreenRectangle.empty());
        this.javaReforged$scissors.addLast(resolved);
    }

    @Inject(method = "disableScissor", at = @At("TAIL"))
    private void javaReforged$popScissor(CallbackInfo callbackInfo) {
        this.javaReforged$scissors.removeLast();
    }

    @Override
    @Nullable
    public ScreenRectangle javaReforged$currentScissor() {
        return this.javaReforged$scissors.peekLast();
    }
}
