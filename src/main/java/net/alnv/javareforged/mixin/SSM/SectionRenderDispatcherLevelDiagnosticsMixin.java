package net.alnv.javareforged.mixin.SSM;

import net.alnv.javareforged.ClientRuntime.runtime.TerrainDebug;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SectionRenderDispatcher.class)
public abstract class SectionRenderDispatcherLevelDiagnosticsMixin {
    @Inject(method = "setLevel", at = @At("TAIL"))
    private void splitTest$trackDispatcherLevel(ClientLevel level, CallbackInfo callback) {
        TerrainDebug.recordSectionDispatcherLevel((SectionRenderDispatcher)(Object)this, level);
    }
}
