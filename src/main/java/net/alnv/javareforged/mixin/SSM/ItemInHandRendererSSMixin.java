package net.alnv.javareforged.mixin.SSM;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.alnv.javareforged.ClientRuntime.runtime.Hands;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;

@org.spongepowered.asm.mixin.Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererSSMixin {
    @WrapMethod(method = "tick")
    private void splitTest$tickWithClientHandState(Operation<Void> original) {
        this.splitTest$runWithClientState(() -> original.call());
    }

    @WrapMethod(method = "itemUsed")
    private void splitTest$itemUsedWithClientHandState(InteractionHand hand, Operation<Void> original) {
        this.splitTest$runWithClientState(() -> original.call(hand));
    }

    @WrapMethod(method = "renderHandsWithItems")
    private void splitTest$renderWithClientHandState(
        float partialTick,
        com.mojang.blaze3d.vertex.PoseStack poseStack,
        net.minecraft.client.renderer.MultiBufferSource.BufferSource bufferSource,
        net.minecraft.client.player.LocalPlayer player,
        int packedLight,
        Operation<Void> original
    ) {
        this.splitTest$runWithClientState(
            () -> original.call(partialTick, poseStack, bufferSource, player, packedLight)
        );
    }

    private void splitTest$runWithClientState(Runnable action) {
        ItemInHandRenderer renderer = (ItemInHandRenderer)(Object)this;
        Hands.begin(renderer);
        try {
            action.run();
        } finally {
            Hands.end(renderer);
        }
    }
}
