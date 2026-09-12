package net.jr.mixin;

import net.minecraft.advancements.DisplayInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DisplayInfo.class)
public abstract class AdvancementDisplayMixin {
    @Inject(method = "shouldAnnounceChat", at = @At("HEAD"), cancellable = true)
    private void javaReforged$disableAdvancementChatAnnouncements(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
