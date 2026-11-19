package net.jr.client.ui.hud;

import net.jr.api.client.local.Client;
import net.jr.api.client.render.gui.HudRenderContext;
import net.jr.api.client.render.gui.HudRenderObject;
import net.jr.api.client.ui.UiDataValues;
import net.jr.registry.ModUi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class PlayerModelHudOverlay implements HudRenderObject {
    private final UiDataValues data = ModUi.PLAYER_MODEL_HUD_DAT.readData();

    @Override
    public void render(HudRenderContext context) {
        var client = Client.current();
        LocalPlayer player = client.player();
        if (player == null || !this.data.isLoaded()) {
            return;
        }

        boolean visible = client.playerModelHud().updateAndIsVisible(player);
        if (!client.options().showPlayerModelHud().get() || !visible) {
            return;
        }

        int left = this.data.integer("player_left");
        int top = this.data.integer("player_top");
        int right = this.data.integer("player_right");
        int bottom = this.data.integer("player_bottom");
        int size = this.data.integer("player_size");
        float offsetY = this.data.decimal("player_offset_y");
        float modelYaw = this.data.decimal("player_model_yaw");
        float modelPitch = this.data.decimal("player_model_pitch");

        if (right <= left
                || bottom <= top
                || size <= 0
                || !Float.isFinite(offsetY)
                || !Float.isFinite(modelYaw)
                || !Float.isFinite(modelPitch)) {
            return;
        }

        EntityRenderState renderState = extractRenderState(player);
        if (renderState instanceof LivingEntityRenderState livingRenderState) {
            livingRenderState.bodyRot = 180.0F + modelYaw;
            livingRenderState.xRot = 0.0F;
            livingRenderState.boundingBoxWidth /= livingRenderState.scale;
            livingRenderState.boundingBoxHeight /= livingRenderState.scale;
            livingRenderState.scale = 1.0F;
        }

        Quaternionf rotation = new Quaternionf().rotateZ((float)Math.PI);
        Quaternionf xRotation = new Quaternionf().rotateX((float)Math.toRadians(modelPitch));
        rotation.mul(xRotation);
        Vector3f translation = new Vector3f(
                0.0F,
                renderState.boundingBoxHeight / 2.0F + offsetY,
                0.0F
        );
        context.graphics().entity(
                renderState,
                size,
                translation,
                rotation,
                xRotation,
                left,
                top,
                right,
                bottom
        );
    }

    private static EntityRenderState extractRenderState(LivingEntity entity) {
        EntityRenderDispatcher dispatcher =
                Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderer<? super LivingEntity, ?> renderer = dispatcher.getRenderer(entity);
        EntityRenderState renderState = renderer.createRenderState(entity, 1.0F);
        renderState.shadowPieces.clear();
        renderState.outlineColor = 0;
        return renderState;
    }
}
