package net.jr.mixin.minigame;

import net.jr.experimental_features.minigame.client.BattleClientState;
import net.jr.mixin.accessors.TntBlockAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TntBlock.class)
public class TntBlockMixin {


    @Inject(method = "onPlace", at = @At(value = "HEAD"), cancellable = true)
    private void javaReforged$spawnTntForOnPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston, CallbackInfo ci) {
        if (BattleClientState.isBattle()) {
            level.removeBlock(pos, movedByPiston);
            TntBlockAccessor.javaReforged$invokePrime(level,pos);
            ci.cancel();
        }
    }
}
