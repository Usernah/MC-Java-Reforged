package net.jr.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.jr.client.render.nametag.LegacyNameTagRenderPass;
import net.jr.client.runtime.render.experimental.NameTagEventFastPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    @Shadow @Final
    protected EntityRenderDispatcher entityRenderDispatcher;

    @Shadow public abstract Font getFont();

    @Shadow
    protected abstract boolean shouldShowName(T entity, double distanceToCameraSq);

    @Shadow
    protected abstract @Nullable Component getNameTag(T entity);

    @Inject(
        method = "extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void javaReforged$extractNameTagWithoutEmptyEvent(
        T entity,
        S state,
        float partialTicks,
        double nameTagDistance,
        double belowNameDistance,
        CallbackInfo ci
    ) {
        if (!NameTagEventFastPath.bypassCanRenderEvent()) {
            return;
        }
        if (this.entityRenderDispatcher.camera != null) {
            state.distanceToCameraSq = this.entityRenderDispatcher.distanceToSqr(entity);
            Component content = this.getNameTag(entity);
            boolean shouldShow = state.distanceToCameraSq < Mth.square(nameTagDistance)
                && this.shouldShowName(entity, state.distanceToCameraSq);
            if (shouldShow) {
                state.nameTag = content;
                state.nameTagAttachment = entity.getAttachments().getNullable(
                    EntityAttachment.NAME_TAG,
                    0,
                    entity.getYRot(partialTicks)
                );
            } else {
                state.nameTag = null;
            }
            state.scoreText = state.distanceToCameraSq < Mth.square(belowNameDistance)
                ? entity.belowNameDisplay()
                : null;
        }
        ci.cancel();
    }

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
