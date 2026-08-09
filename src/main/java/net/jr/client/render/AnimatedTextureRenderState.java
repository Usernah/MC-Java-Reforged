package net.jr.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;
import org.jetbrains.annotations.Nullable;

public record AnimatedTextureRenderState(
    RenderPipeline pipeline,
    TextureSetup textureSetup,
    Matrix3x2fc pose,
    int x0,
    int y0,
    int x1,
    int y1,
    float u0,
    float u1,
    float v0,
    float v1,
    int nextU0,
    int nextU1,
    int nextV0,
    int nextV1,
    float progress,
    int color,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
    public AnimatedTextureRenderState(
        TextureSetup textureSetup,
        Matrix3x2fc pose,
        int x0,
        int y0,
        int x1,
        int y1,
        float u0,
        float u1,
        float v0,
        float v1,
        int nextU0,
        int nextU1,
        int nextV0,
        int nextV1,
        float progress,
        int color,
        @Nullable ScreenRectangle scissorArea
    ) {
        this(
            ModRenderPipelines.ANIMATED_TEXTURE,
            textureSetup,
            pose,
            x0,
            y0,
            x1,
            y1,
            u0,
            u1,
            v0,
            v1,
            nextU0,
            nextU1,
            nextV0,
            nextV1,
            progress,
            color,
            scissorArea,
            bounds(x0, y0, x1, y1, pose, scissorArea)
        );
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        this.vertex(consumer, this.x0, this.y0, this.u0, this.v0, this.nextU0, this.nextV0);
        this.vertex(consumer, this.x0, this.y1, this.u0, this.v1, this.nextU0, this.nextV1);
        this.vertex(consumer, this.x1, this.y1, this.u1, this.v1, this.nextU1, this.nextV1);
        this.vertex(consumer, this.x1, this.y0, this.u1, this.v0, this.nextU1, this.nextV0);
    }

    private void vertex(VertexConsumer consumer, int x, int y, float u, float v, int nextU, int nextV) {
        consumer.addVertexWith2DPose(this.pose, x, y)
            .setUv(u, v)
            .setColor(this.color)
            .setUv1(nextU, nextV)
            .setLineWidth(this.progress);
    }

    @Nullable
    private static ScreenRectangle bounds(
        int x0,
        int y0,
        int x1,
        int y1,
        Matrix3x2fc pose,
        @Nullable ScreenRectangle scissor
    ) {
        ScreenRectangle transformed = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0)
            .transformMaxBounds(pose);
        return scissor == null ? transformed : scissor.intersection(transformed);
    }
}
