package net.jr.mixin.accessors;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CreativeModeInventoryScreen.class)
public interface CreativeModeInventoryScreenAccessor {
    @Invoker("isCreativeSlot")
    boolean javaReforged$isCreativeSlot(Slot slot);

    @Accessor("destroyItemSlot")
    Slot javaReforged$getDestroyItemSlot();
}
