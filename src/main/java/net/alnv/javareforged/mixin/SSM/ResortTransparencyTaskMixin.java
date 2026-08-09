package net.alnv.javareforged.mixin.SSM;

import net.alnv.javareforged.ClientRuntime.runtime.TerrainSectionOwners;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$ResortTransparencyTask")
public abstract class ResortTransparencyTaskMixin {
    @Shadow(remap = false)
    @Final
    private SectionRenderDispatcher.RenderSection this$1;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void splitTest$registerTerrainTask(CallbackInfo callback) {
        TerrainSectionOwners.taskCreated(this.this$1, this);
    }
}
