package net.jr.mixin;

import net.jr.client.sound.action.WidgetSoundRouter;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractWidget.class)
public abstract class AbstractWidgetMixin {
    @Inject(method = "playDownSound", at = @At("HEAD"), cancellable = true)
    private void javaReforged$replaceVanillaClickSound(SoundManager soundManager, CallbackInfo ci) {
        WidgetSoundRouter.playDownSound((AbstractWidget) (Object) this);
        ci.cancel();
    }
}
