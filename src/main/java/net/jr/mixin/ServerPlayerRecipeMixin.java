package net.jr.mixin;

import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerRecipeMixin {
    @Inject(method = "awardRecipesByKey", at = @At("HEAD"), cancellable = true)
    private void javaReforged$skipRedundantRecipeAwards(List<ResourceKey<Recipe<?>>> recipes, CallbackInfo ci) {

        ci.cancel();
    }
}
