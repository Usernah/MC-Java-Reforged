package net.jr.client.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.jr.api.client.resource.Asset;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

public final class ModRenderPipelines {
    public static final VertexFormat ANIMATED_TEXTURE_FORMAT = VertexFormat.builder(0)
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

    private ModRenderPipelines() {
    }

    public static void register(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(ANIMATED_TEXTURE);
    }
}
