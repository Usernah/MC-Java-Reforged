package net.jr.mixin.runtime;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.jr.api.client.split.SplitScreen;
import net.jr.client.runtime.render.state.HandStateScope;
import net.jr.client.runtime.viewport.ViewportLayout;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@org.spongepowered.asm.mixin.Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    private static final float SPLIT_HAND_CENTERING = 0.60F;
    private static final float SPLIT_BOW_CENTERING = 0.32F;

    @Unique
    private boolean javaReforged$centerBaseHandTransform;

    @WrapMethod(method = "tick")
    private void javaReforged$tickWithClientHandState(Operation<Void> original) {
        this.javaReforged$runWithClientState(() -> original.call());
    }

    @WrapMethod(method = "itemUsed")
    private void javaReforged$itemUsedWithClientHandState(InteractionHand hand, Operation<Void> original) {
        this.javaReforged$runWithClientState(() -> original.call(hand));
    }

    @WrapMethod(method = "submitHandsWithItems")
    private void javaReforged$renderWithClientHandState(
        float partialTick,
        com.mojang.blaze3d.vertex.PoseStack poseStack,
        net.minecraft.client.renderer.SubmitNodeCollector submitNodeCollector,
        net.minecraft.client.player.LocalPlayer player,
        int packedLight,
        Operation<Void> original
    ) {
        this.javaReforged$runWithClientState(
            () -> original.call(partialTick, poseStack, submitNodeCollector, player, packedLight)
        );
    }

    @WrapOperation(
        method = "submitHandsWithItems",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;submitArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"
        )
    )
    private void javaReforged$centerIndividualSplitHand(
        ItemInHandRenderer renderer,
        AbstractClientPlayer player,
        float frameInterp,
        float xRot,
        InteractionHand hand,
        float attack,
        ItemStack itemStack,
        float inverseArmHeight,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int lightCoords,
        Operation<Void> original
    ) {
        if (!javaReforged$shouldCenterHand(player, hand, itemStack)) {
            original.call(
                renderer,
                player,
                frameInterp,
                xRot,
                hand,
                attack,
                itemStack,
                inverseArmHeight,
                poseStack,
                submitNodeCollector,
                lightCoords
            );
            return;
        }

        boolean usingThisHand = player.isUsingItem()
            && player.getUseItemRemainingTicks() > 0
            && player.getUsedItemHand() == hand;
        ItemUseAnimation animation = itemStack.getUseAnimation();
        boolean centerAtBase = usingThisHand
            && javaReforged$usesVanillaBaseTransform(animation);

        if (centerAtBase) {
            boolean previous = this.javaReforged$centerBaseHandTransform;
            this.javaReforged$centerBaseHandTransform = true;
            try {
                original.call(
                    renderer,
                    player,
                    frameInterp,
                    xRot,
                    hand,
                    attack,
                    itemStack,
                    inverseArmHeight,
                    poseStack,
                    submitNodeCollector,
                    lightCoords
                );
            } finally {
                this.javaReforged$centerBaseHandTransform = previous;
            }
            return;
        }

        HumanoidArm arm = hand == InteractionHand.MAIN_HAND
            ? player.getMainArm()
            : player.getMainArm().getOpposite();
        float direction = arm == HumanoidArm.RIGHT ? -1.0F : 1.0F;
        float centering = usingThisHand && animation == ItemUseAnimation.BOW
            ? SPLIT_BOW_CENTERING
            : SPLIT_HAND_CENTERING;
        float offset = 0.56F * centering;

        poseStack.pushPose();
        poseStack.translate(direction * offset, 0.0F, 0.0F);
        try {
            original.call(
                renderer,
                player,
                frameInterp,
                xRot,
                hand,
                attack,
                itemStack,
                inverseArmHeight,
                poseStack,
                submitNodeCollector,
                lightCoords
            );
        } finally {
            poseStack.popPose();
        }
    }

    @ModifyArg(
        method = "applyItemArmTransform",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"
        ),
        index = 0
    )
    private float javaReforged$centerBaseHandX(float x) {
        return this.javaReforged$centerBaseHandTransform
            ? x * (1.0F - SPLIT_HAND_CENTERING)
            : x;
    }

    @ModifyConstant(
        method = "submitArmWithItem",
        constant = @Constant(floatValue = 0.56F)
    )
    private float javaReforged$centerDirectBaseHandX(float value) {
        return this.javaReforged$centerBaseHandTransform
            ? value * (1.0F - SPLIT_HAND_CENTERING)
            : value;
    }

    @Unique
    private static boolean javaReforged$usesVanillaBaseTransform(ItemUseAnimation animation) {
        return !animation.hasCustomArmTransform()
            ? animation != ItemUseAnimation.BOW
            : animation == ItemUseAnimation.SPEAR;
    }

    private static boolean javaReforged$shouldCenterHand(
        AbstractClientPlayer player,
        InteractionHand hand,
        ItemStack itemStack
    ) {
        if (!SplitScreen.isActive()) {
            return false;
        }
        ViewportLayout layout = SplitScreen.layout();
        if (layout != ViewportLayout.TWO_VERTICAL) {
            return false;
        }
        return !(hand == InteractionHand.MAIN_HAND
            && itemStack.getItem() instanceof MapItem
            && player.getOffhandItem().isEmpty());
    }

    private void javaReforged$runWithClientState(Runnable action) {
        HandStateScope.run((ItemInHandRenderer) (Object) this, action);
    }
}
