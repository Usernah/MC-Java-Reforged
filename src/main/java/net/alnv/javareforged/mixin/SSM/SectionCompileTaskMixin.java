package net.alnv.javareforged.mixin.SSM;

import java.util.concurrent.CompletableFuture;
import net.alnv.javareforged.ClientRuntime.runtime.TerrainSectionOwners;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {
    "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$RebuildTask",
    "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$ResortTransparencyTask"
})
public abstract class SectionCompileTaskMixin {
    @Inject(method = "doTask", at = @At("RETURN"))
    private void splitTest$trackTerrainTask(CallbackInfoReturnable<CompletableFuture<?>> callback) {
        TerrainSectionOwners.trackCompletion(this, callback.getReturnValue());
    }
}
