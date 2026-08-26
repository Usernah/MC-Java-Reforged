package net.jr.mixin.accessors;

import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemInHandRenderer.class)
public interface ItemInHandRendererAccessor {
    @Accessor("mainHandItem") ItemStack javaReforged$getMainHandItem();
    @Accessor("mainHandItem") void javaReforged$setMainHandItem(ItemStack value);
    @Accessor("offHandItem") ItemStack javaReforged$getOffHandItem();
    @Accessor("offHandItem") void javaReforged$setOffHandItem(ItemStack value);
    @Accessor("mainHandHeight") float javaReforged$getMainHandHeight();
    @Accessor("mainHandHeight") void javaReforged$setMainHandHeight(float value);
    @Accessor("oMainHandHeight") float javaReforged$getOldMainHandHeight();
    @Accessor("oMainHandHeight") void javaReforged$setOldMainHandHeight(float value);
    @Accessor("offHandHeight") float javaReforged$getOffHandHeight();
    @Accessor("offHandHeight") void javaReforged$setOffHandHeight(float value);
    @Accessor("oOffHandHeight") float javaReforged$getOldOffHandHeight();
    @Accessor("oOffHandHeight") void javaReforged$setOldOffHandHeight(float value);
}
