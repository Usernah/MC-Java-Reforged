package net.jr.ClientRuntime.state;

import net.jr.mixin.SSM.ItemInHandRendererSSAccessor;
import net.minecraft.world.item.ItemStack;

public final class HandState {
    private ItemStack mainHandItem = ItemStack.EMPTY;
    private ItemStack offHandItem = ItemStack.EMPTY;
    private float mainHandHeight;
    private float oldMainHandHeight;
    private float offHandHeight;
    private float oldOffHandHeight;

    public void install(ItemInHandRendererSSAccessor renderer) {
        renderer.splitTest$setMainHandItem(this.mainHandItem);
        renderer.splitTest$setOffHandItem(this.offHandItem);
        renderer.splitTest$setMainHandHeight(this.mainHandHeight);
        renderer.splitTest$setOldMainHandHeight(this.oldMainHandHeight);
        renderer.splitTest$setOffHandHeight(this.offHandHeight);
        renderer.splitTest$setOldOffHandHeight(this.oldOffHandHeight);
    }

    public void capture(ItemInHandRendererSSAccessor renderer) {
        this.mainHandItem = renderer.splitTest$getMainHandItem();
        this.offHandItem = renderer.splitTest$getOffHandItem();
        this.mainHandHeight = renderer.splitTest$getMainHandHeight();
        this.oldMainHandHeight = renderer.splitTest$getOldMainHandHeight();
        this.offHandHeight = renderer.splitTest$getOffHandHeight();
        this.oldOffHandHeight = renderer.splitTest$getOldOffHandHeight();
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
