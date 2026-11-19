package net.jr.client.render.pipeline;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.jr.api.client.resource.Asset;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

public final class ModRenderPipelines {
    public static final BindGroupLayout TERRAIN_BATCH_LAYOUT = BindGroupLayout.builder()
        .withUniform("TerrainBatch", UniformType.UNIFORM_BUFFER)
        .build();

    public static final BindGroupLayout LEGACY_GAMMA_LAYOUT = BindGroupLayout.builder()
        .withUniform("LegacyGammaConfig", UniformType.UNIFORM_BUFFER)
        .build();

    public static final VertexFormat TERRAIN_SECTION_INSTANCE_FORMAT = VertexFormat.builder(1)
        .addAttribute("ChunkPosition", GpuFormat.RGB32_SINT)
        .addAttribute("ChunkVisibility", GpuFormat.R32_FLOAT)
        .build();

    public static final VertexFormat ANIMATED_TEXTURE_FORMAT = VertexFormat.builder(0)
        .addAttribute("Position", GpuFormat.RGB32_FLOAT)
        .addAttribute("UV0", GpuFormat.RG32_FLOAT)
        .addAttribute("Color", GpuFormat.RGBA8_UNORM)
        .addAttribute("UV1", GpuFormat.RG16_SINT)
        .addAttribute("LineWidth", GpuFormat.R32_FLOAT)
        .build();

    public static final VertexFormat PIXEL_ART_COMPOSITE_FORMAT = VertexFormat.builder(0)
        .addAttribute("Position", GpuFormat.RGB32_FLOAT)
        .addAttribute("UV0", GpuFormat.RG32_FLOAT)
        .addAttribute("Color", GpuFormat.RGBA8_UNORM)
        .addAttribute("UV1", GpuFormat.RG16_SINT)
        .addAttribute("LineWidth", GpuFormat.R32_FLOAT)
        .build();

    public static final RenderPipeline ANIMATED_TEXTURE = RenderPipeline.builder()
        .withBindGroupLayout(BindGroupLayouts.GLOBALS)
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
        .withLocation(Asset.MOD("pipeline/animated_tex").res())
        .withVertexShader(Asset.MOD("core/animated_tex").res())
        .withFragmentShader(Asset.MOD("core/animated_tex").res())
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withVertexBinding(0, ANIMATED_TEXTURE_FORMAT)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .build();

    public static final RenderPipeline PIXEL_ART_COMPOSITE = RenderPipeline.builder()
        .withBindGroupLayout(BindGroupLayouts.GLOBALS)
        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
        .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
        .withLocation(Asset.MOD("pipeline/pixel_art_composite").res())
        .withVertexShader(Asset.MOD("core/pixel_art_composite").res())
        .withFragmentShader(Asset.MOD("core/pixel_art_composite").res())
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA))
        .withVertexBinding(0, PIXEL_ART_COMPOSITE_FORMAT)
        .withPrimitiveTopology(PrimitiveTopology.QUADS)
        .build();

    public static final RenderPipeline LEGACY_GAMMA = RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
        .withLocation(Asset.MOD("pipeline/legacy_gamma").res())
        .withVertexShader(Asset.MC("core/screenquad").res())
        .withFragmentShader(Asset.MOD("core/legacy_gamma").res())
        .withBindGroupLayout(BindGroupLayouts.IN_SAMPLER)
        .withBindGroupLayout(LEGACY_GAMMA_LAYOUT)
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .build();

    private static final RenderPipeline.Snippet TERRAIN_MULTIDRAW_SNIPPET = RenderPipeline.builder(RenderPipelines.GENERIC_BLOCKS_SNIPPET)
        .withBindGroupLayout(BindGroupLayouts.PROJECTION)
        .withBindGroupLayout(TERRAIN_BATCH_LAYOUT)
        .withVertexBinding(1, TERRAIN_SECTION_INSTANCE_FORMAT)
        .withVertexShader(Asset.MOD("core/terrain_multidraw").res())
        .withFragmentShader(Asset.MOD("core/terrain_multidraw").res())
        .buildSnippet();

    public static final RenderPipeline SOLID_TERRAIN_MULTIDRAW = RenderPipeline.builder(TERRAIN_MULTIDRAW_SNIPPET)
        .withLocation(Asset.MOD("pipeline/solid_terrain_multidraw").res())
        .build();

    public static final RenderPipeline CUTOUT_TERRAIN_MULTIDRAW = RenderPipeline.builder(TERRAIN_MULTIDRAW_SNIPPET)
        .withLocation(Asset.MOD("pipeline/cutout_terrain_multidraw").res())
        .withShaderDefine("ALPHA_CUTOUT", 0.5F)
        .build();

    private ModRenderPipelines() {
    }

    public static void register(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(ANIMATED_TEXTURE);
        event.registerPipeline(PIXEL_ART_COMPOSITE);
        event.registerPipeline(LEGACY_GAMMA);
        event.registerPipeline(SOLID_TERRAIN_MULTIDRAW);
        event.registerPipeline(CUTOUT_TERRAIN_MULTIDRAW);
    }
}
