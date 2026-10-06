package net.jr.client.ui.hud;

import net.jr.api.client.local.Client;
import net.jr.api.client.render.Draw;
import net.jr.client.components.elements.TextElement;
import net.jr.client.runtime.ClientRuntime;
import net.jr.client.runtime.client.ClientInstance;
import net.jr.client.runtime.minimap.MinimapEngine;
import net.jr.client.runtime.state.MinimapState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class MinimapHudOverlay {
    private static final Identifier BASE_SPRITE =
            Identifier.withDefaultNamespace("hud/minmap_base");

    private static final int BASE_WIDTH = 77;
    private static final int BASE_HEIGHT = 77;

    private static final int MARGIN_X = 10;
    private static final int MARGIN_Y = 10;

    private static final int CONTENT_OFFSET_X = 6;
    private static final int CONTENT_OFFSET_Y = 6;
    private static final int CONTENT_WIDTH = 65;
    private static final int CONTENT_HEIGHT = 65;

    private static final int MAP_SIZE =
            MinimapEngine.TEXTURE_SIZE;

    private static final float NORTH_UP_TO_MINECRAFT_YAW = 180.0F;

    private MinimapHudOverlay() {
    }

    public static void extract(GuiGraphicsExtractor graphics) {
        ClientInstance client = Client.currentOrNull();
        if (client == null) {
            return;
        }

        MinimapEngine engine = ClientRuntime.INSTANCE.minimap();
        engine.updateView(client);

        MinimapState state = client.minimap();
        if (!state.visible() || state.dimension() == null) {
            return;
        }

        int clipLeft =
                MARGIN_X + CONTENT_OFFSET_X;

        int clipTop =
                MARGIN_Y + CONTENT_OFFSET_Y;

        int clipRight =
                clipLeft + CONTENT_WIDTH;

        int clipBottom =
                clipTop + CONTENT_HEIGHT;

        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                BASE_SPRITE,
                MARGIN_X,
                MARGIN_Y,
                BASE_WIDTH,
                BASE_HEIGHT
        );

        int displayX = (int) Math.floor(state.x());
        int displayY = (int) Math.floor(state.y());
        int displayZ = (int) Math.floor(state.z());

        TextElement xyz = new TextElement(
                Component.literal("X:" + displayX + " Y:" + displayY + " Z:" + displayZ),
                0,
                0,
                graphics
        );

        xyz.setColor(0xFFF4F4F4);
        xyz.setScale(0.8F);
        xyz.setShadow(true);
        xyz.setAlign(Draw.TextAlign.CENTER);

        float textX = (MARGIN_X + (BASE_WIDTH / 2.0F));
        float textY = MARGIN_Y + BASE_HEIGHT + 5.0F;

        xyz.visualState().setX(textX);
        xyz.visualState().setY(textY);

        graphics.enableScissor(
                clipLeft,
                clipTop,
                clipRight,
                clipBottom
        );

        graphics.pose().pushMatrix();

        try {
            graphics.pose().translate(
                    MARGIN_X,
                    MARGIN_Y
            );

            float centerX =
                    BASE_WIDTH / 2.0F;

            float centerY =
                    BASE_HEIGHT / 2.0F;

            graphics.pose().pushMatrix();

            try {
                graphics.pose().translate(
                        centerX,
                        centerY
                );

                float mapRotationDegrees =
                        NORTH_UP_TO_MINECRAFT_YAW
                                - state.rotation();

                graphics.pose().rotate(
                        (float) Math.toRadians(mapRotationDegrees)
                );

                graphics.pose().translate(
                        (float) -engine.offsetX(client),
                        (float) -engine.offsetZ(client)
                );

                int half = MAP_SIZE / 2;

                graphics.blit(
                        engine.texture(client.slotId()),
                        -half,
                        -half,
                        half,
                        half,
                        0.0F,
                        1.0F,
                        0.0F,
                        1.0F
                );
            } finally {
                graphics.pose().popMatrix();
            }
        } finally {
            graphics.pose().popMatrix();
            graphics.disableScissor();
        }

        xyz.draw(graphics);
    }
}
