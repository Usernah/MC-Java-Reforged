package net.jr.mixin.runtime;

import net.jr.client.runtime.render.pass.GuiRenderPass;
import net.jr.client.runtime.render.pass.ToastRenderPass;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Gui.class)
public abstract class GuiSSExtractionMixin {
    @Shadow
    @Final
    private GuiRenderState guiRenderState;

    @Redirect(
            method = "update",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/toasts/ToastManager;update()V"
            )
    )
    private void javaReforged$updateToastStates(ToastManager manager) {
        ToastRenderPass.update(manager);
    }

    /**
     * @author Usernah
     * @reason GUI extraction is fully owned by the Java Reforged runtime.
     */
    @Overwrite
    public void extractRenderState(
            DeltaTracker deltaTracker,
            boolean shouldRenderLevel,
            boolean gameLoadFinished
    ) {
        GuiRenderPass.extract(
                (Gui)(Object)this,
                this.guiRenderState,
                deltaTracker,
                shouldRenderLevel,
                gameLoadFinished
        );
    }
}