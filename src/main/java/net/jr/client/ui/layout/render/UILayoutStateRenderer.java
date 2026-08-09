package net.jr.client.ui.layout.render;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.state.gui.GlyphRenderState;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

final class UILayoutStateRenderer {
    private static final Comparator<ScreenRectangle> SCISSOR_COMPARATOR = Comparator.nullsFirst(
        Comparator.comparing(ScreenRectangle::top)
            .thenComparing(ScreenRectangle::bottom)
            .thenComparing(ScreenRectangle::left)
            .thenComparing(ScreenRectangle::right)
    );
    private static final Comparator<TextureSetup> TEXTURE_COMPARATOR = Comparator.nullsFirst(
        Comparator.comparing(TextureSetup::getSortKey)
    );
    private static final Comparator<GuiElementRenderState> ELEMENT_COMPARATOR = Comparator
        .comparing(GuiElementRenderState::scissorArea, SCISSOR_COMPARATOR)
        .thenComparing(GuiElementRenderState::pipeline, Comparator.comparing(RenderPipeline::getSortKey))
        .thenComparing(GuiElementRenderState::textureSetup, TEXTURE_COMPARATOR);

    private final StagedVertexBuffer vertexBuffer = new StagedVertexBuffer(
        () -> "Java Reforged UI layout vertices",
        262144
    );
    private final Projection projection = new Projection();
    private final ProjectionMatrixBuffer projectionBuffer = new ProjectionMatrixBuffer("Java Reforged UI layout");
    private final List<Draw> draws = new ArrayList<>();

    private RenderPipeline previousPipeline;
    private TextureSetup previousTextureSetup;
    private ScreenRectangle previousScissor;
    private StagedVertexBuffer.Draw previousDraw;

    void render(UILayoutRenderQueue.Job job) {
        GuiRenderState state = job.state();
        try {
            this.prepareText(state);
            state.sortElements(ELEMENT_COMPARATOR);
            this.resetBatchState();
            state.forEachElement(this::appendElement, GuiRenderState.TraverseRange.ALL);
            this.vertexBuffer.upload();
            this.draw(job);
        } finally {
            this.draws.clear();
            this.vertexBuffer.endDraw();
            this.vertexBuffer.endFrame();
            state.reset();
            this.resetBatchState();
        }
    }

    private void prepareText(GuiRenderState state) {
        state.forEachText(text -> {
            final var pose = text.pose;
            final ScreenRectangle scissor = text.scissor;
            text.ensurePrepared().visit(new Font.GlyphVisitor() {
                @Override
                public void acceptRenderable(TextRenderable renderable) {
                    state.addGlyphToCurrentLayer(new GlyphRenderState(pose, renderable, scissor));
                }
            });
        });
    }

    private void appendElement(GuiElementRenderState element) {
        RenderPipeline pipeline = element.pipeline();
        TextureSetup textureSetup = element.textureSetup();
        ScreenRectangle scissor = element.scissorArea();
        if (this.previousDraw == null
            || pipeline != this.previousPipeline
            || !java.util.Objects.equals(scissor, this.previousScissor)
            || !textureSetup.equals(this.previousTextureSetup)) {
            this.previousPipeline = pipeline;
            this.previousTextureSetup = textureSetup;
            this.previousScissor = scissor;
            this.previousDraw = this.vertexBuffer.appendDraw(
                pipeline.getVertexFormatBinding(0),
                pipeline.getPrimitiveTopology()
            );
            this.draws.add(new Draw(this.previousDraw, pipeline, textureSetup, scissor));
        }
        element.buildVertices(this.vertexBuffer.getVertexBuilder(this.previousDraw));
    }

    private void draw(UILayoutRenderQueue.Job job) {
        UILayoutRenderTarget target = job.target();
        this.projection.setupOrtho(1000.0F, 11000.0F, job.guiWidth(), job.guiHeight(), true);
        RenderSystem.setProjectionMatrix(this.projectionBuffer.getBuffer(this.projection), ProjectionType.ORTHOGRAPHIC);
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
            .writeTransform(new Matrix4f().setTranslation(0.0F, 0.0F, -11000.0F));

        try (RenderPass pass = RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(
                () -> "Java Reforged live UI layout",
                target.colorView(),
                Optional.of(GuiRenderer.CLEAR_COLOR),
                target.depthView(),
                OptionalDouble.of(0.0D)
            )) {
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            for (Draw draw : this.draws) {
                this.execute(draw, pass, target.textureHeight(), job.guiScale());
            }
        }
    }

    private void execute(Draw draw, RenderPass pass, int targetHeight, int guiScale) {
        StagedVertexBuffer.ExecuteInfo info = this.vertexBuffer.getExecuteInfo(draw.draw());
        if (info == null) {
            return;
        }

        pass.setPipeline(draw.pipeline());
        pass.setVertexBuffer(0, info.vertexBuffer().slice());
        this.applyScissor(pass, draw.scissor(), targetHeight, guiScale);

        TextureSetup texture = draw.textureSetup();
        if (texture.texure0() != null) {
            pass.bindTexture("Sampler0", texture.texure0(), texture.sampler0());
        }
        if (texture.texure1() != null) {
            pass.bindTexture("Sampler1", texture.texure1(), texture.sampler1());
        }
        if (texture.texure2() != null) {
            pass.bindTexture("Sampler2", texture.texure2(), texture.sampler2());
        }

        pass.setIndexBuffer(info.indexBuffer(), info.indexType());
        pass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
    }

    private void applyScissor(RenderPass pass, ScreenRectangle scissor, int targetHeight, int guiScale) {
        if (scissor == null) {
            pass.disableScissor();
            return;
        }
        int left = scissor.left() * guiScale;
        int top = scissor.top() * guiScale;
        int right = scissor.right() * guiScale;
        int bottom = scissor.bottom() * guiScale;
        pass.enableScissor(
            left,
            targetHeight - bottom,
            Math.max(0, right - left),
            Math.max(0, bottom - top)
        );
    }

    private void resetBatchState() {
        this.previousPipeline = null;
        this.previousTextureSetup = null;
        this.previousScissor = null;
        this.previousDraw = null;
    }

    private record Draw(
        StagedVertexBuffer.Draw draw,
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        ScreenRectangle scissor
    ) {
    }
}
