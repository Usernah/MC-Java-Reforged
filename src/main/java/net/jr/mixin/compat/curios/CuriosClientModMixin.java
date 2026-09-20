package net.jr.mixin.compat.curios;

import net.jr.compat.curios.screen.CuriosInventoryScreen;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.client.CuriosClientMod;
import top.theillusivec4.curios.impl.CuriosRegistry;

@Mixin(value = CuriosClientMod.class, remap = false)
abstract class CuriosClientModMixin {
    @Inject(method = "registerMenuScreens", at = @At("HEAD"), cancellable = true)
    private void javaReforged$registerScreen(RegisterMenuScreensEvent event, CallbackInfo ci) {
        event.register(CuriosRegistry.CURIO_MENU.get(), CuriosInventoryScreen::new);
        ci.cancel();
    }
}
