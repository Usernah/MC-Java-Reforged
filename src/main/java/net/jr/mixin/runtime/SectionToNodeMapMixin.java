package net.jr.mixin.runtime;

import net.jr.client.runtime.terrain.TerrainGraphNodes;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.renderer.SectionOcclusionGraph$SectionToNodeMap")
public abstract class SectionToNodeMapMixin {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static int javaReforged$omitDuplicateIndexArray(int sectionCount) {
        return 0;
    }

    @Inject(method = "put", at = @At("HEAD"), cancellable = true)
    private void javaReforged$putGlobalSection(
        SectionRenderDispatcher.RenderSection section,
        @Coerce Object node,
        CallbackInfo ci
    ) {
        TerrainGraphNodes.put(this, section, node);
        ci.cancel();
    }

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void javaReforged$getGlobalSection(
        SectionRenderDispatcher.RenderSection section,
        CallbackInfoReturnable<Object> cir
    ) {
        cir.setReturnValue(TerrainGraphNodes.get(this, section));
    }
}
