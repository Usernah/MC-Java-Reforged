package net.jr.mixin.accessors;

import javax.annotation.Nullable;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BlockItem.class)
public interface BlockItemPlacementAccessor {
    @Invoker("getPlacementState")
    @Nullable
    BlockState javareforged$getPlacementState(
            BlockPlaceContext context
    );
}