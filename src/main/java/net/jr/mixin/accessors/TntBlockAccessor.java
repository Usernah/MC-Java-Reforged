package net.jr.mixin.accessors;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TntBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TntBlock.class)
public interface TntBlockAccessor {

    @Invoker("prime")
    static boolean javaReforged$invokePrime(Level level, BlockPos pos) {
        throw new AssertionError();
    }
}
