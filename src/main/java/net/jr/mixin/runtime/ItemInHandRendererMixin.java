package net.jr.mixin.runtime;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.jr.client.runtime.render.state.HandStateScope;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;

@org.spongepowered.asm.mixin.Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
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

    private void javaReforged$runWithClientState(Runnable action) {
        HandStateScope.run((ItemInHandRenderer) (Object) this, action);
    }
}
