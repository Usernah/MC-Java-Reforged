package net.jr.client.runtime.player;

import javax.annotation.Nullable;
import net.jr.mixin.accessors.BlockItemPlacementAccessor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class BedrockBridgePlacement {
    private BedrockBridgePlacement() {
    }

    @Nullable
    public static Candidate resolve(
            LocalPlayer player,
            InteractionHand hand
    ) {
        if (player.isCrouching() || player.isSpectator()) {
            return null;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return null;
        }

        var level = player.level();

        if (!blockItem.getBlock().isEnabled(level.enabledFeatures())) {
            return null;
        }

        Direction direction = player.getDirection();

        BlockPos supportPos = player.blockPosition().below();
        BlockState supportState = level.getBlockState(supportPos);

        if (!supportState.isFaceSturdy(
                level,
                supportPos,
                direction
        )) {
            return null;
        }

        BlockPos targetPos = supportPos.relative(direction);

        if (!level.getWorldBorder().isWithinBounds(targetPos)) {
            return null;
        }

        // Conservamos el comportamiento del bridge viejo:
        // sólo extender hacia espacio vacío.
        if (!level.isEmptyBlock(targetPos)) {
            return null;
        }

        Vec3 hitLocation = Vec3.atCenterOf(supportPos).add(
                direction.getStepX() * 0.5,
                direction.getStepY() * 0.5,
                direction.getStepZ() * 0.5
        );

        BlockHitResult hitResult = new BlockHitResult(
                hitLocation,
                direction,
                supportPos,
                false
        );

        BlockPlaceContext context = new BlockPlaceContext(
                player,
                hand,
                stack,
                hitResult
        );

        if (!context.canPlace()) {
            return null;
        }

        if (!context.getClickedPos().equals(targetPos)) {
            return null;
        }

        BlockPlaceContext updatedContext =
                blockItem.updatePlacementContext(context);

        if (updatedContext == null
                || !updatedContext.getClickedPos().equals(targetPos)) {
            return null;
        }

        BlockState placementState =
                ((BlockItemPlacementAccessor) blockItem)
                        .javareforged$getPlacementState(updatedContext);

        if (placementState == null) {
            return null;
        }

        return new Candidate(
                hand,
                targetPos,
                hitResult,
                placementState
        );
    }

    @Nullable
    public static Candidate resolveFirst(LocalPlayer player) {
        Candidate main = resolve(player, InteractionHand.MAIN_HAND);
        return main != null
                ? main
                : resolve(player, InteractionHand.OFF_HAND);
    }

    public record Candidate(
            InteractionHand hand,
            BlockPos targetPos,
            BlockHitResult hitResult,
            BlockState placementState
    ) {
    }
}