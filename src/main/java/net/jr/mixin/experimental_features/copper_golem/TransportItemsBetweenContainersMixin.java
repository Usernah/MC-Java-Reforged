package net.jr.mixin.experimental_features.copper_golem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.jr.experimental_features.copper_golem.CopperGolemContainerMemory;
import net.jr.experimental_features.copper_golem.CopperGolemMemoryAccess;
import net.jr.experimental_features.copper_golem.CopperGolemSortingLogic;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(TransportItemsBetweenContainers.class)
public abstract class TransportItemsBetweenContainersMixin {

    @Shadow
    private TransportItemsBetweenContainers.@Nullable TransportItemTarget target;

    @Shadow
    protected abstract void clearMemoriesAfterMatchingTargetFound(
            PathfinderMob body
    );

    @Inject(
            method = "getTransportTarget",
            at = @At("HEAD"),
            cancellable = true
    )
    private void javaReforged$waitAfterRejectedItem(
            ServerLevel level,
            PathfinderMob body,
            CallbackInfoReturnable<
                    Optional<
                            TransportItemsBetweenContainers.TransportItemTarget
                    >
            > cir
    ) {
        if (body instanceof CopperGolemMemoryAccess access
                && body.getMainHandItem().isEmpty()
                && level.getGameTime() < access.javaReforged$getRetryAfterGameTime()) {
            cir.setReturnValue(Optional.empty());
        }
    }

    @Redirect(
            method = "getTransportTarget",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;distToCenterSqr(Lnet/minecraft/core/Position;)D"
            )
    )
    private double javaReforged$prioritizeRememberedContainers(
            BlockPos pos,
            Position from,
            ServerLevel level,
            PathfinderMob body
    ) {
        double vanillaDistance =
                pos.distToCenterSqr(from);

        if (!(body instanceof CopperGolem)) {
            return vanillaDistance;
        }

        if (body.getMainHandItem().isEmpty()) {
            return vanillaDistance;
        }

        BlockEntity blockEntity =
                level.getBlockEntity(pos);

        if (blockEntity == null) {
            return vanillaDistance;
        }

        TransportItemsBetweenContainers.TransportItemTarget candidate =
                TransportItemsBetweenContainers.TransportItemTarget
                        .tryCreatePossibleTarget(
                                blockEntity,
                                level
                        );

        if (candidate == null) {
            return vanillaDistance;
        }

        return CopperGolemSortingLogic.scoreDistance(
                body,
                level,
                candidate,
                vanillaDistance
        );
    }

    @Inject(
            method = "matchesLeavingItemsRequirement",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void javaReforged$allowFamilyDestination(
            PathfinderMob body,
            Container container,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(body instanceof CopperGolemMemoryAccess access)) {
            return;
        }

        if (access.javaReforged$isReturningToSource()) {
            if (CopperGolemSortingLogic.canAccept(
                    container,
                    body.getMainHandItem()
            )) {
                cir.setReturnValue(true);
            }

            return;
        }

        if (CopperGolemSortingLogic.canPlaceByFamily(
                body,
                container
        )) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = "doReachedTargetInteraction",
            at = @At("HEAD")
    )
    private void javaReforged$observeDestinationBeforeInteraction(
            PathfinderMob body,
            Container container,
            java.util.function.BiConsumer<PathfinderMob, Container> onPickupSuccess,
            java.util.function.BiConsumer<PathfinderMob, Container> onPickupFailure,
            java.util.function.BiConsumer<PathfinderMob, Container> onPlaceSuccess,
            java.util.function.BiConsumer<PathfinderMob, Container> onPlaceFailure,
            CallbackInfo ci
    ) {
        if (!(body instanceof CopperGolemMemoryAccess access)) {
            return;
        }

        if (body.getMainHandItem().isEmpty()) {
            return;
        }

        if (access.javaReforged$isReturningToSource()) {
            return;
        }

        if (this.target == null) {
            return;
        }

        access.javaReforged$getContainerMemory()
                .observe(
                        body.level(),
                        this.target.pos(),
                        container
                );
    }

    @Inject(
            method = "pickUpItems",
            at = @At("HEAD"),
            cancellable = true
    )
    private void javaReforged$rememberSourceAndPickDifferentItem(
            PathfinderMob body,
            Container container,
            CallbackInfo ci
    ) {
        if (!(body instanceof CopperGolemMemoryAccess access)) {
            return;
        }

        if (this.target != null) {
            BlockPos normalized =
                    CopperGolemContainerMemory
                            .normalizeChestPos(
                                    body.level(),
                                    this.target.pos()
                            );

            access.javaReforged$setTransportSource(
                    GlobalPos.of(
                            body.level().dimension(),
                            normalized
                    )
            );
        }

        access.javaReforged$setReturningToSource(false);

        ItemStack picked =
                CopperGolemSortingLogic.takeItem(
                        container,
                        access.javaReforged$getLastRejectedItem()
                );

        body.setItemSlot(
                EquipmentSlot.MAINHAND,
                picked
        );

        body.setGuaranteedDrop(
                EquipmentSlot.MAINHAND
        );

        container.setChanged();

        if (!picked.isEmpty()) {
            access.javaReforged$setLastRejectedItem(null);
        }

        this.clearMemoriesAfterMatchingTargetFound(body);

        ci.cancel();
    }

    @Inject(
            method = "getTransportTarget",
            at = @At("RETURN"),
            cancellable = true
    )
    private void javaReforged$returnRejectedItemToSource(
            ServerLevel level,
            PathfinderMob body,
            CallbackInfoReturnable<
                    Optional<
                            TransportItemsBetweenContainers.TransportItemTarget
                    >
            > cir
    ) {
        if (cir.getReturnValue().isPresent()) {
            return;
        }

        if (!(body instanceof CopperGolemMemoryAccess access)) {
            return;
        }

        if (body.getMainHandItem().isEmpty()) {
            return;
        }

        GlobalPos source =
                access.javaReforged$getTransportSource();

        if (source == null) {
            return;
        }

        if (!source.dimension().equals(level.dimension())) {
            return;
        }

        if (!level.hasChunkAt(source.pos())) {
            return;
        }

        TransportItemsBetweenContainers.TransportItemTarget sourceTarget =
                TransportItemsBetweenContainers.TransportItemTarget
                        .tryCreatePossibleTarget(
                                source.pos(),
                                level
                        );

        if (sourceTarget == null) {
            return;
        }

        if (!sourceTarget.state().is(BlockTags.COPPER_CHESTS)) {
            return;
        }

        if (!CopperGolemSortingLogic.canAccept(
                sourceTarget.container(),
                body.getMainHandItem()
        )) {
            return;
        }

        access.javaReforged$setReturningToSource(true);

        cir.setReturnValue(
                Optional.of(sourceTarget)
        );
    }

    @Inject(
            method = "isWantedBlock",
            at = @At("RETURN"),
            cancellable = true
    )
    private void javaReforged$allowSourceWhileReturning(
            PathfinderMob body,
            net.minecraft.world.level.block.state.BlockState state,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(body instanceof CopperGolemMemoryAccess access)) {
            return;
        }

        if (!access.javaReforged$isReturningToSource()) {
            return;
        }

        if (!body.getMainHandItem().isEmpty()
                && state.is(BlockTags.COPPER_CHESTS)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
            method = "putDownItem",
            at = @At("HEAD")
    )
    private void javaReforged$rememberRejectedItem(
            PathfinderMob body,
            Container container,
            CallbackInfo ci
    ) {
        if (!(body instanceof CopperGolemMemoryAccess access)) {
            return;
        }

        if (!access.javaReforged$isReturningToSource()) {
            return;
        }

        if (!body.getMainHandItem().isEmpty()) {
            access.javaReforged$setLastRejectedItem(
                    body.getMainHandItem().getItem()
            );
        }
    }

    @Inject(
            method = "putDownItem",
            at = @At("TAIL")
    )
    private void javaReforged$finishReturnToSource(
            PathfinderMob body,
            Container container,
            CallbackInfo ci
    ) {
        if (!(body instanceof CopperGolemMemoryAccess access)) {
            return;
        }

        if (!access.javaReforged$isReturningToSource()) {
            return;
        }

        if (body.getMainHandItem().isEmpty()) {
            access.javaReforged$setReturningToSource(false);
            access.javaReforged$setTransportSource(null);
            access.javaReforged$setRetryAfterGameTime(
                    body.level().getGameTime()
                            + CopperGolemSortingLogic.REJECTED_ITEM_RETRY_DELAY_TICKS
            );
        }
    }
}
