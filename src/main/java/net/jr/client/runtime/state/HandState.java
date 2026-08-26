package net.jr.client.runtime.state;

import net.jr.mixin.accessors.ItemInHandRendererAccessor;
import net.minecraft.world.item.ItemStack;

public final class HandState {
    private ItemStack mainHandItem = ItemStack.EMPTY;
    private ItemStack offHandItem = ItemStack.EMPTY;
    private float mainHandHeight;
    private float oldMainHandHeight;
    private float offHandHeight;
    private float oldOffHandHeight;

    public void install(ItemInHandRendererAccessor renderer) {
        renderer.javaReforged$setMainHandItem(this.mainHandItem);
        renderer.javaReforged$setOffHandItem(this.offHandItem);
        renderer.javaReforged$setMainHandHeight(this.mainHandHeight);
        renderer.javaReforged$setOldMainHandHeight(this.oldMainHandHeight);
        renderer.javaReforged$setOffHandHeight(this.offHandHeight);
        renderer.javaReforged$setOldOffHandHeight(this.oldOffHandHeight);
    }

    public void capture(ItemInHandRendererAccessor renderer) {
        this.mainHandItem = renderer.javaReforged$getMainHandItem();
        this.offHandItem = renderer.javaReforged$getOffHandItem();
        this.mainHandHeight = renderer.javaReforged$getMainHandHeight();
        this.oldMainHandHeight = renderer.javaReforged$getOldMainHandHeight();
        this.offHandHeight = renderer.javaReforged$getOffHandHeight();
        this.oldOffHandHeight = renderer.javaReforged$getOldOffHandHeight();
    }

    public void clear() {
        this.mainHandItem = ItemStack.EMPTY;
        this.offHandItem = ItemStack.EMPTY;
        this.mainHandHeight = 0.0F;
        this.oldMainHandHeight = 0.0F;
        this.offHandHeight = 0.0F;
        this.oldOffHandHeight = 0.0F;
    }
}
