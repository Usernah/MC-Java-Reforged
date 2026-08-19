package net.jr.mixin.controlhints;

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
    Slot javareforged$getHoveredSlot();

    @Accessor("hoveredSlot")
    void javareforged$setHoveredSlot(@Nullable Slot slot);

    @Accessor("isQuickCrafting")
    void javareforged$setQuickCrafting(boolean quickCrafting);

    @Accessor("quickCraftSlots")
    Set<Slot> javareforged$getQuickCraftSlots();

    @Accessor("quickCraftingRemainder")
    void javareforged$setQuickCraftingRemainder(int quickCraftingRemainder);

    @Invoker("slotClicked")
    void javareforged$invokeSlotClicked(@Nullable Slot slot, int slotId, int mouseButton, ContainerInput clickType);

    @Accessor("leftPos")
    int javareforged$getLeftPos();

    @Accessor("topPos")
    int javareforged$getTopPos();

    @Accessor("imageWidth")
    int javareforged$getImageWidth();

    @Accessor("imageHeight")
    int javareforged$getImageHeight();
}

