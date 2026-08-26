package net.jr.client.render.nametag;

import com.mojang.blaze3d.vertex.PoseStack;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.context.SlotScope;
import net.jr.client.runtime.viewport.ViewportArea;
import net.jr.client.ui.presentation.UiPresentation;
import net.jr.mixin.accessors.FontAccessor;
import net.jr.playerdata.PlayerProfileDataManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class LegacyNameTagRenderPass {
    private static final float TAG_SCALE = 0.025F;
    private static final float PADDING = 1.5F;
    private static final int READABLE_FULLSCREEN = 16;
    private static final int READABLE_COMPACT = 8;

    private LegacyNameTagRenderPass() {
    }

    public static void extract(Entity entity, EntityRenderState state) {
        if (!(entity instanceof Player player)) return;
        ((LegacyNameTagRenderState) state).javaReforged$setPlayerColor(PlayerProfileDataManager.getLegacyColor(player));
        Component splitName = PlayerProfileDataManager.getSplitDisplayName(player);
        if (splitName != null && state.nameTag != null) {
            state.nameTag = splitName.copy().withStyle(state.nameTag.getStyle());
        }
    }

    public static boolean submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                                 CameraRenderState camera, int offset, Font font) {
        int playerColor = ((LegacyNameTagRenderState) state).javaReforged$getPlayerColor();
        if (playerColor < 0 || state.nameTag == null || state.nameTagAttachment == null) return false;

        poseStack.pushPose();
        if (state.scoreText != null) {
            collector.submitNameTag(poseStack, state.nameTagAttachment, offset, state.scoreText,
                    !state.isDiscrete, state.lightCoords, camera);
            poseStack.translate(0.0F, 9.0F * 1.15F * TAG_SCALE, 0.0F);
        }
        poseStack.pushPose();
        poseStack.translate(state.nameTagAttachment.x, state.nameTagAttachment.y + 0.5D, state.nameTagAttachment.z);
        poseStack.mulPose(camera.orientation);
        poseStack.scale(TAG_SCALE, -TAG_SCALE, TAG_SCALE);

        float textX = -font.width(state.nameTag) / 2.0F;
        float distance = (float) Math.sqrt(state.distanceToCameraSq);
        float opacity = textOpacity(distance, readableDistance());
        float border = borderThickness(distance, camera);
        int backgroundAlpha = Mth.clamp((int) (Minecraft.getInstance().gameRenderer.gameRenderState()
                .optionsRenderState.getBackgroundOpacity(0.25F) * 255.0F), 0, 255);

        if (opacity < 1.0F) {
            submitSolidTag(collector, poseStack, font, textX, offset, state.nameTag, playerColor,
                    state.isDiscrete ? Font.DisplayMode.NORMAL : Font.DisplayMode.SEE_THROUGH, state.lightCoords);
        }
        if (opacity > 0.0F && !state.isDiscrete && opacity >= 1.0F) {
            submitFrame(collector, poseStack, font, textX, offset, state.nameTag, border, playerColor,
                    backgroundAlpha, Font.DisplayMode.SEE_THROUGH, state.lightCoords, 0x70);
            submitBorder(collector, poseStack, font, textX, offset, state.nameTag, border, playerColor,
                    Font.DisplayMode.NORMAL, LightCoordsUtil.lightCoordsWithEmission(state.lightCoords, 2), 0xD0);
        } else if (opacity > 0.0F && state.isDiscrete && opacity >= 1.0F) {
            submitFrame(collector, poseStack, font, textX, offset, state.nameTag, border, playerColor,
                    backgroundAlpha, Font.DisplayMode.NORMAL, state.lightCoords, 0x58);
        }
        if (opacity > 0.0F) {
            int normalColor = ARGB.color(Mth.clamp((int) (255.0F * opacity), 0, 255), 0xFFFFFF);
            int seeThroughColor = ARGB.color(Mth.clamp((int) (127.0F * opacity), 0, 127), 0xFFFFFF);
            collector.submitText(poseStack, textX, offset, state.nameTag.getVisualOrderText(), false,
                    Font.DisplayMode.NORMAL, LightCoordsUtil.lightCoordsWithEmission(state.lightCoords, 2), normalColor, 0, 0);
            if (!state.isDiscrete) {
                collector.submitText(poseStack, textX, offset, state.nameTag.getVisualOrderText(), false,
                        Font.DisplayMode.SEE_THROUGH, state.lightCoords, seeThroughColor, 0, 0);
            }
        }
        poseStack.popPose();
        poseStack.popPose();
        return true;
    }

    private static int readableDistance() {
        return UiPresentation.isPortable() || ClientRuntime.INSTANCE.clients().count() > 2
                ? READABLE_COMPACT : READABLE_FULLSCREEN;
    }

    private static float textOpacity(float distance, int readableDistance) {
        if (distance < readableDistance) return 1.0F;
        int difference = (int) (distance - readableDistance);
        if (difference > readableDistance) return 0.0F;
        int divisor = difference / 2;
        return divisor <= 1 ? 1.0F : 1.0F / divisor;
    }

    private static float borderThickness(float distance, CameraRenderState camera) {
        Integer slotId = SlotScope.idOrNull();
        ViewportArea viewport = slotId != null ? ClientRuntime.INSTANCE.viewports().viewportOrNull(slotId) : null;
        int height = viewport != null ? viewport.glHeight() : Minecraft.getInstance().getWindow().getHeight();
        float projectionY = camera.projectionMatrix != null ? Math.abs(camera.projectionMatrix.m11()) : 0.0F;
        if (distance <= 0.0F || height <= 0 || projectionY <= 0.0F || !Float.isFinite(projectionY)) return 1.0F;
        return 2.0F * distance / (projectionY * height * TAG_SCALE);
    }

    private static void submitSolidTag(SubmitNodeCollector collector, PoseStack pose, Font font, float x,
                                       float y, Component text, int rgb, Font.DisplayMode mode, int light) {
        float left = x - PADDING, right = x + font.width(text) + PADDING;
        submitRect(collector, pose, font, left, y - 2.0F, right, y + 10.0F, ARGB.color(255, rgb), mode, light);
    }

    private static void submitFrame(SubmitNodeCollector collector, PoseStack pose, Font font, float x, float y,
                                    Component text, float thickness, int rgb, int backgroundAlpha,
                                    Font.DisplayMode mode, int light, int borderAlpha) {
        float left = x - PADDING, right = x + font.width(text) + PADDING, top = y - 2.0F, bottom = y + 10.0F;
        submitRect(collector, pose, font, left, top, right, bottom, ARGB.color(backgroundAlpha, 0), mode, light);
        submitBorderRects(collector, pose, font, left, top, right, bottom, thickness,
                ARGB.color(borderAlpha, rgb), mode, light);
    }

    private static void submitBorder(SubmitNodeCollector collector, PoseStack pose, Font font, float x, float y,
                                     Component text, float thickness, int rgb, Font.DisplayMode mode, int light, int alpha) {
        float left = x - PADDING, right = x + font.width(text) + PADDING;
        submitBorderRects(collector, pose, font, left, y - 2.0F, right, y + 10.0F, thickness,
                ARGB.color(alpha, rgb), mode, light);
    }

    private static void submitBorderRects(SubmitNodeCollector collector, PoseStack pose, Font font, float left,
                                          float top, float right, float bottom, float thickness, int color,
                                          Font.DisplayMode mode, int light) {
        float t = Math.min(thickness, (bottom - top) * 0.5F);
        submitRect(collector, pose, font, left, top, right, top + t, color, mode, light);
        submitRect(collector, pose, font, left, bottom - t, right, bottom, color, mode, light);
        submitRect(collector, pose, font, left, top + t, left + t, bottom - t, color, mode, light);
        submitRect(collector, pose, font, right - t, top + t, right, bottom - t, color, mode, light);
    }

    private static void submitRect(SubmitNodeCollector collector, PoseStack pose, Font font, float left, float top,
                                   float right, float bottom, int color, Font.DisplayMode mode, int light) {
        if (right <= left || bottom <= top) return;
        TextRenderable effect = ((FontAccessor) font).javaReforged$getProvider().effect()
                .createEffect(left, top, right, bottom, -0.01F, color, 0, 0.0F);
        collector.submitCustomGeometry(pose, effect.renderType(mode),
                (entry, buffer) -> effect.render(entry.pose(), buffer, light, false));
    }
}
