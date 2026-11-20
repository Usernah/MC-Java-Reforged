package net.jr.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.jr.api.client.ui.UiScreenFile;
import net.jr.client.components.widgets.JavaBaseWidget;
import net.jr.client.ui.cursor.ScreenMousePositionResolver;
import net.jr.client.ui.container.DslContainerBridge;
import net.jr.client.ui.dsl.DslHostBridge;
import net.jr.client.ui.dsl.DslUiContext;
import net.jr.client.ui.screen.DslScreenBridge;
import net.jr.client.ui.screen.DslScreenContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin implements DslScreenBridge, DslHostBridge {
    @Unique
    private DslScreenContext javaReforged$dslContext;

    @Shadow
    protected abstract <T extends GuiEventListener & NarratableEntry> T addWidget(T widget);

    @Override
    public DslScreenContext javaReforged$dslContext(UiScreenFile file) {
        if (this.javaReforged$dslContext == null) {
            this.javaReforged$dslContext = new DslScreenContext((Screen)(Object)this, file);
        } else if (!this.javaReforged$dslContext.file().equals(file)) {
            throw new IllegalStateException("A Screen cannot use two different DSL files");
        }
        return this.javaReforged$dslContext;
    }

    @Override
    public DslScreenContext javaReforged$peekDslContext() {
        return this.javaReforged$dslContext;
    }

    @Override
    public void javaReforged$addDslWidget(JavaBaseWidget widget) {
        this.addWidget(widget);
    }

    @Override
    public void javaReforged$closeDslContext() {
        if (this.javaReforged$dslContext != null) {
            this.javaReforged$dslContext.close();
            this.javaReforged$dslContext = null;
        }
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void javaReforged$resetDslContextsForInit(CallbackInfo ci) {
        this.javaReforged$resetDslContexts();
    }

    @Inject(method = "rebuildWidgets", at = @At("HEAD"))
    private void javaReforged$resetDslContextsForRebuild(CallbackInfo ci) {
        this.javaReforged$resetDslContexts();
    }

    @Unique
    private void javaReforged$resetDslContexts() {
        this.javaReforged$closeDslContext();
        if ((Object)this instanceof DslContainerBridge containerBridge) {
            containerBridge.javaReforged$closeDslContainerContext();
        }
    }

    @WrapMethod(method = "extractRenderStateWithTooltipAndSubtitles")
    private void javaReforged$extractDslScreen(
        GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, Operation<Void> original
    ) {
        DslUiContext<?> context = (Object)this instanceof DslContainerBridge containerBridge
            ? containerBridge.javaReforged$peekDslContainerContext()
            : this.javaReforged$dslContext;
        if (context == null) {
            original.call(graphics, mouseX, mouseY, partialTick);
            return;
        }
        context.extractScreen(() -> original.call(graphics, mouseX, mouseY, partialTick));
    }

    @ModifyVariable(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int javaReforged$resolveMouseX(int mouseX) {
        return ScreenMousePositionResolver.x(mouseX);
    }

    @ModifyVariable(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int javaReforged$resolveMouseY(int mouseY) {
        return ScreenMousePositionResolver.y(mouseY);
    }

    @Inject(method = "extractTransparentBackground", at = @At("HEAD"),cancellable = true)
    private void javaReforged$TransparentBackground(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if ((Object) this instanceof AbstractContainerScreen<?>) {
            ci.cancel();
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void javaReforged$closeDslScreen(CallbackInfo ci) {
        this.javaReforged$closeDslContext();
        if ((Object)this instanceof DslContainerBridge containerBridge) {
            containerBridge.javaReforged$closeDslContainerContext();
        }
    }
}
