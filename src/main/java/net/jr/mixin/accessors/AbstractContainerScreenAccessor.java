package net.jr.mixin.accessors;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;
import java.util.Set;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("hoveredSlot")
    @Nullable
    Slot javaReforged$getHoveredItemSlot();

    @Accessor("hoveredSlot")
    void javaReforged$setHoveredSlot(@Nullable Slot slot);

    @Accessor("isQuickCrafting")
    void javaReforged$setQuickCrafting(boolean quickCrafting);

    @Accessor("quickCraftSlots")
    Set<Slot> javaReforged$getQuickCraftSlots();

    @Accessor("quickCraftingRemainder")
    void javaReforged$setQuickCraftingRemainder(int quickCraftingRemainder);

    @Invoker("slotClicked")
    void javaReforged$invokeSlotClicked(@Nullable Slot slot, int slotId, int mouseButton, ContainerInput clickType);

    @Accessor("leftPos")
    int javaReforged$getLeftPos();

    @Accessor("leftPos")
    void javaReforged$setLeftPos(int leftPos);

    @Accessor("topPos")
    int javaReforged$getTopPos();

    @Accessor("topPos")
    void javaReforged$setTopPos(int topPos);

    @Accessor("imageWidth")
    int javaReforged$getImageWidth();

    @Accessor("imageHeight")
    int javaReforged$getImageHeight();
}

