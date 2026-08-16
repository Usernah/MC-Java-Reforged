package net.jr.mixin.runtime;

import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemInHandRenderer.class)
public interface ItemInHandRendererSSAccessor {
    @Accessor("mainHandItem") ItemStack splitTest$getMainHandItem();
    @Accessor("mainHandItem") void splitTest$setMainHandItem(ItemStack value);
    @Accessor("offHandItem") ItemStack splitTest$getOffHandItem();
    @Accessor("offHandItem") void splitTest$setOffHandItem(ItemStack value);
    @Accessor("mainHandHeight") float splitTest$getMainHandHeight();
    @Accessor("mainHandHeight") void splitTest$setMainHandHeight(float value);
    @Accessor("oMainHandHeight") float splitTest$getOldMainHandHeight();
    @Accessor("oMainHandHeight") void splitTest$setOldMainHandHeight(float value);
    @Accessor("offHandHeight") float splitTest$getOffHandHeight();
    @Accessor("offHandHeight") void splitTest$setOffHandHeight(float value);
    @Accessor("oOffHandHeight") float splitTest$getOldOffHandHeight();
    @Accessor("oOffHandHeight") void splitTest$setOldOffHandHeight(float value);
}
