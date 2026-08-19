package net.jr.mixin.runtime;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.jr.client.runtime.render.state.HandStateScope;
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

    @WrapMethod(method = "submitHandsWithItems")
    private void splitTest$renderWithClientHandState(
        float partialTick,
        com.mojang.blaze3d.vertex.PoseStack poseStack,
        net.minecraft.client.renderer.SubmitNodeCollector submitNodeCollector,
        net.minecraft.client.player.LocalPlayer player,
        int packedLight,
        Operation<Void> original
    ) {
        this.splitTest$runWithClientState(
            () -> original.call(partialTick, poseStack, submitNodeCollector, player, packedLight)
        );
    }

    private void splitTest$runWithClientState(Runnable action) {
        ItemInHandRenderer renderer = (ItemInHandRenderer)(Object)this;
        try (HandStateScope ignored = HandStateScope.enter(renderer)) {
            action.run();
        }
    }
}
