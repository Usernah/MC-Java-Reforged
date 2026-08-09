package net.jr.mixin.SSM;

import com.mojang.blaze3d.vulkan.VulkanRenderPass;
import org.lwjgl.vulkan.VkViewport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Keeps Vulkan's depth range intact when split screen changes a render-pass viewport. */
@Mixin(VulkanRenderPass.class)
public abstract class VulkanRenderPassSSMixin {
    @ModifyArg(
        method = "setViewport",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/vulkan/VK12;vkCmdSetViewport(Lorg/lwjgl/vulkan/VkCommandBuffer;ILorg/lwjgl/vulkan/VkViewport$Buffer;)V"
        ),
        index = 2
    )
    private VkViewport.Buffer splitTest$preserveDepthRange(VkViewport.Buffer viewport) {
        return viewport.minDepth(0.0F).maxDepth(1.0F);
    }
}
