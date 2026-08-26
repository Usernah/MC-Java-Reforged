package net.jr.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.jr.client.render.nametag.LegacyNameTagRenderPass;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    @Shadow public abstract Font getFont();

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void javaReforged$extractPlayerNameTag(T entity, S state, float partialTicks, CallbackInfo ci) {
        LegacyNameTagRenderPass.extract(entity, state);
    }

    @Inject(method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;I)V",
            at = @At("HEAD"), cancellable = true)
    private void javaReforged$submitLegacyNameTag(S state, PoseStack poseStack, SubmitNodeCollector collector,
                                                  CameraRenderState camera, int offset, CallbackInfo ci) {
        if (LegacyNameTagRenderPass.submit(state, poseStack, collector, camera, offset, this.getFont())) ci.cancel();
    }
}
