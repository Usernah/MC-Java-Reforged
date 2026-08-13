package net.jr.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.jr.client.render.LegacyNameTagRenderState;
import net.jr.mixin.accessors.FontProviderAccessor;
import net.jr.playerdata.PlayerProfileDataManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    @Unique
    private static final float JAVA_REFORGED$TAG_SCALE = 0.025F;
    @Unique
    private static final float JAVA_REFORGED$PADDING = 1.5F;
    @Unique
    private static final float JAVA_REFORGED$MIN_BORDER = 0.03F;
    @Unique
    private static final float JAVA_REFORGED$MAX_BORDER = 6.0F;
    @Unique
    private static final float JAVA_REFORGED$BORDER_PER_BLOCK = 0.1F;

    @Shadow
    @Final
    protected EntityRenderDispatcher entityRenderDispatcher;

    @Shadow
    public abstract Font getFont();

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void javaReforged$extractPlayerColor(T entity, S state, float partialTicks, CallbackInfo ci) {
        if (entity instanceof Player player) {
            ((LegacyNameTagRenderState) state).javaReforged$setPlayerColor(PlayerProfileDataManager.getLegacyColor(player));
            net.minecraft.network.chat.Component splitName = PlayerProfileDataManager.getSplitDisplayName(player);
            if (splitName != null && state.nameTag != null) {
                state.nameTag = splitName.copy().withStyle(state.nameTag.getStyle());
            }
        }
    }

    @Inject(
            method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void javaReforged$submitLegacyNameTag(
            S state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera,
            int offset,
            CallbackInfo ci
    ) {
        int playerColor = ((LegacyNameTagRenderState) state).javaReforged$getPlayerColor();
        if (playerColor < 0 || state.nameTag == null || state.nameTagAttachment == null) {
            return;
        }

        poseStack.pushPose();
        if (state.scoreText != null) {
            collector.submitNameTag(poseStack, state.nameTagAttachment, offset, state.scoreText, !state.isDiscrete, state.lightCoords, camera);
            poseStack.translate(0.0F, 9.0F * 1.15F * JAVA_REFORGED$TAG_SCALE, 0.0F);
        }

        poseStack.pushPose();
        poseStack.translate(state.nameTagAttachment.x, state.nameTagAttachment.y + 0.5D, state.nameTagAttachment.z);
        poseStack.mulPose(camera.orientation);
        poseStack.scale(JAVA_REFORGED$TAG_SCALE, -JAVA_REFORGED$TAG_SCALE, JAVA_REFORGED$TAG_SCALE);

        Font font = this.getFont();
        float textX = -font.width(state.nameTag) / 2.0F;
        float border = javaReforged$resolveBorderThickness(state.distanceToCameraSq);
        int backgroundAlpha = Mth.clamp(
                (int) (Minecraft.getInstance().gameRenderer.gameRenderState().optionsRenderState.getBackgroundOpacity(0.25F) * 255.0F),
                0,
                255
        );

        if (!state.isDiscrete) {
            javaReforged$submitFrame(collector, poseStack, font, textX, offset, state.nameTag, border, playerColor, backgroundAlpha,
                    Font.DisplayMode.SEE_THROUGH, state.lightCoords, 0x70);
            javaReforged$submitBorder(collector, poseStack, font, textX, offset, state.nameTag, border, playerColor,
                    Font.DisplayMode.NORMAL, LightCoordsUtil.lightCoordsWithEmission(state.lightCoords, 2), 0xD0);
            collector.submitText(poseStack, textX, offset, state.nameTag.getVisualOrderText(), false, Font.DisplayMode.NORMAL,
                    LightCoordsUtil.lightCoordsWithEmission(state.lightCoords, 2), -1, 0, 0);
            collector.submitText(poseStack, textX, offset, state.nameTag.getVisualOrderText(), false, Font.DisplayMode.SEE_THROUGH,
                    state.lightCoords, 0x7FFFFFFF, 0, 0);
        } else {
            javaReforged$submitFrame(collector, poseStack, font, textX, offset, state.nameTag, border, playerColor, backgroundAlpha,
                    Font.DisplayMode.NORMAL, state.lightCoords, 0x58);
            collector.submitText(poseStack, textX, offset, state.nameTag.getVisualOrderText(), false, Font.DisplayMode.NORMAL,
                    state.lightCoords, 0x7FFFFFFF, 0, 0);
        }

        poseStack.popPose();
        poseStack.popPose();
        ci.cancel();
    }

    @Unique
    private static float javaReforged$resolveBorderThickness(double distanceSq) {
        float distance = (float) Math.sqrt(distanceSq);
        return Mth.clamp(distance * JAVA_REFORGED$BORDER_PER_BLOCK, JAVA_REFORGED$MIN_BORDER, JAVA_REFORGED$MAX_BORDER);
    }

    @Unique
    private static void javaReforged$submitFrame(
            SubmitNodeCollector collector, PoseStack poseStack, Font font, float textX, float lineY,
            net.minecraft.network.chat.Component text, float thickness, int rgb, int backgroundAlpha,
            Font.DisplayMode mode, int light, int borderAlpha
    ) {
        float left = textX - JAVA_REFORGED$PADDING;
        float right = textX + font.width(text) + JAVA_REFORGED$PADDING;
        float top = lineY - 2.0F;
        float bottom = lineY + 10.0F;
        javaReforged$submitRect(collector, poseStack, font, left, top, right, bottom, ARGB.color(backgroundAlpha, 0), mode, light);
        javaReforged$submitBorderRects(collector, poseStack, font, left, top, right, bottom, thickness,
                ARGB.color(borderAlpha, rgb), mode, light);
    }

    @Unique
    private static void javaReforged$submitBorder(
            SubmitNodeCollector collector, PoseStack poseStack, Font font, float textX, float lineY,
            net.minecraft.network.chat.Component text, float thickness, int rgb, Font.DisplayMode mode, int light, int alpha
    ) {
        float left = textX - JAVA_REFORGED$PADDING;
        float right = textX + font.width(text) + JAVA_REFORGED$PADDING;
        float top = lineY - 2.0F;
        float bottom = lineY + 10.0F;
        javaReforged$submitBorderRects(collector, poseStack, font, left, top, right, bottom, thickness,
                ARGB.color(alpha, rgb), mode, light);
    }

    @Unique
    private static void javaReforged$submitBorderRects(
            SubmitNodeCollector collector, PoseStack poseStack, Font font, float left, float top, float right, float bottom,
            float thickness, int color, Font.DisplayMode mode, int light
    ) {
        float clamped = Math.min(thickness, (bottom - top) * 0.5F);
        javaReforged$submitRect(collector, poseStack, font, left, top, right, top + clamped, color, mode, light);
        javaReforged$submitRect(collector, poseStack, font, left, bottom - clamped, right, bottom, color, mode, light);
        javaReforged$submitRect(collector, poseStack, font, left, top + clamped, left + clamped, bottom - clamped, color, mode, light);
        javaReforged$submitRect(collector, poseStack, font, right - clamped, top + clamped, right, bottom - clamped, color, mode, light);
    }

    @Unique
    private static void javaReforged$submitRect(
            SubmitNodeCollector collector, PoseStack poseStack, Font font,
            float left, float top, float right, float bottom, int color, Font.DisplayMode mode, int light
    ) {
        if (right <= left || bottom <= top) {
            return;
        }
        TextRenderable effect = ((FontProviderAccessor) font).javaReforged$getProvider().effect()
                .createEffect(left, top, right, bottom, -0.01F, color, 0, 0.0F);
        collector.submitCustomGeometry(poseStack, effect.renderType(mode),
                (pose, buffer) -> effect.render(pose.pose(), buffer, light, false));
    }
}
