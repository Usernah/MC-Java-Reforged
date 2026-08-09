package net.jr.client.ui.hint;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;

public final class WorldHintRules {
    private WorldHintRules() {
    }

    public static boolean shouldShowAttackHint(ControlHintContext context) {
        HitResult hitResult = context.hitResult();
        return hitResult instanceof EntityHitResult entityHitResult
            && entityHitResult.getEntity() instanceof LivingEntity;
    }

    public static boolean shouldShowBreakHint(ControlHintContext context) {
        BlockStateContext stateContext = blockStateContext(context);
        if (stateContext == null) {
            return false;
        }

        return stateContext.hasBlockEntity() || stateContext.state().blocksMotion();
    }

    @Nullable
    public static Component resolveUseLabel(ControlHintContext context) {
        BlockStateContext stateContext = blockStateContext(context);
        if (stateContext == null) {
            return null;
        }

        BlockState state = stateContext.state();
        if (isOpenable(state)) {
            boolean isOpen = state.hasProperty(BlockStateProperties.OPEN) && Boolean.TRUE.equals(state.getValue(BlockStateProperties.OPEN));
            return Component.literal(isOpen ? "Cerrar" : "Abrir");
        }

        if (isChestLike(state)) {
            return Component.literal("Abrir");
        }

        if (isActionLike(state)) {
            return Component.literal("Accionar");
        }

        if (stateContext.menuProvider() != null) {
            return Component.literal("Usar");
        }

        ItemStack heldItem = mainHeldItem(context.player());
        if (!heldItem.isEmpty()) {
            if (heldItem.getItem() instanceof BlockItem) {
                return Component.literal("Colocar");
            }
            return Component.literal("Usar");
        }

        return null;
    }

    @Nullable
    private static BlockStateContext blockStateContext(ControlHintContext context) {
        if (!(context.hitResult() instanceof BlockHitResult blockHitResult)) {
            return null;
        }

        LocalPlayer player = context.player();
        if (player == null) {
            return null;
        }

        Level level = player.level();
        BlockPos blockPos = blockHitResult.getBlockPos();
        BlockState state = level.getBlockState(blockPos);
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        MenuProvider menuProvider = blockEntity instanceof MenuProvider menu ? menu : null;
        return new BlockStateContext(level, blockPos, state, blockEntity, menuProvider);
    }

    private static ItemStack mainHeldItem(@Nullable LocalPlayer player) {
        return player == null ? ItemStack.EMPTY : player.getMainHandItem();
    }

    private static boolean isOpenable(BlockState state) {
        return state.getBlock() instanceof DoorBlock
            || state.getBlock() instanceof TrapDoorBlock
            || state.getBlock() instanceof FenceGateBlock;
    }

    private static boolean isChestLike(BlockState state) {
        return state.getBlock() instanceof ChestBlock
            || state.getBlock() instanceof EnderChestBlock
            || state.getBlock() instanceof BarrelBlock
            || state.getBlock() instanceof ShulkerBoxBlock;
    }

    private static boolean isActionLike(BlockState state) {
        return state.getBlock() instanceof ButtonBlock
            || state.getBlock() instanceof LeverBlock;
    }

    private record BlockStateContext(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, @Nullable MenuProvider menuProvider) {
        private boolean hasBlockEntity() {
            return this.blockEntity != null;
        }
    }
}
