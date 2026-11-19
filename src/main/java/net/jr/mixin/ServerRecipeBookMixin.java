package net.jr.mixin;

import java.util.Collection;
import java.util.Set;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerRecipeBook.class)
public abstract class ServerRecipeBookMixin {
    @Shadow @Final protected Set<ResourceKey<Recipe<?>>> known;
    @Shadow @Final protected Set<ResourceKey<Recipe<?>>> highlight;

    @Inject(method = "sendInitialRecipeBook", at = @At("HEAD"))
    private void javaReforged$provideCompleteRecipeBook(ServerPlayer player, CallbackInfo ci) {


        this.known.clear();
        this.highlight.clear();
        for (RecipeHolder<?> recipe : player.level().getServer().getRecipeManager().getRecipes()) {

            if (!recipe.value().isSpecial()) {
                this.known.add(recipe.id());
            }
        }


    }

    @Inject(method = {"addRecipes", "removeRecipes"}, at = @At("HEAD"), cancellable = true)
    private void javaReforged$keepRecipesAvailable(
        Collection<RecipeHolder<?>> recipes, ServerPlayer player, CallbackInfoReturnable<Integer> cir
    ) {
        cir.setReturnValue(0);
    }

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void javaReforged$preventRecipeRelocking(ResourceKey<Recipe<?>> recipe, CallbackInfo ci) {
        ci.cancel();
    }
}
