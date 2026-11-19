package net.jr.mixin;

import net.jr.api.client.ui.DslContainer;
import net.jr.api.client.ui.UiContainerFile;
import net.jr.client.ui.container.DslContainerBridge;
import net.jr.client.ui.container.DslContainerContext;
import net.jr.client.ui.container.slots.FloatingItemRenderPass;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin implements DslContainerBridge {
    @Unique
    private DslContainerContext javaReforged$dslContainerContext;

    @Override
    public DslContainerContext javaReforged$dslContainerContext(UiContainerFile file) {
        if (this.javaReforged$dslContainerContext == null) {
            this.javaReforged$dslContainerContext = new DslContainerContext(
                (AbstractContainerScreen<?>)(Object)this,
                file
            );
        } else if (!this.javaReforged$dslContainerContext.file().equals(file)) {
            throw new IllegalStateException("A container cannot use two different DSL files");
        }
        return this.javaReforged$dslContainerContext;
    }

    @Override
    public DslContainerContext javaReforged$peekDslContainerContext() {
        return this.javaReforged$dslContainerContext;
    }

    @Override
    public void javaReforged$closeDslContainerContext() {
        if (this.javaReforged$dslContainerContext != null) {
            this.javaReforged$dslContainerContext.close();
            this.javaReforged$dslContainerContext = null;
        }
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void javaReforged$resetDslContainerContextForInit(CallbackInfo ci) {
        this.javaReforged$closeDslContainerContext();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void javaReforged$applyDslContainerBoundsOffset(CallbackInfo ci) {
        if ((Object)this instanceof DslContainer container) {
            container.dslContext().applyBoundsOffset();
        }
    }

    @Inject(method = "extractFloatingItem", at = @At("HEAD"), cancellable = true)
    private void javaReforged$extractLargeFloatingItem(
            GuiGraphicsExtractor graphics,
            ItemStack stack,
            int x,
            int y,
            @Nullable String text,
            CallbackInfo ci
    ) {
        FloatingItemRenderPass.extract(graphics, stack, x, y, text);
        ci.cancel();
    }
}
