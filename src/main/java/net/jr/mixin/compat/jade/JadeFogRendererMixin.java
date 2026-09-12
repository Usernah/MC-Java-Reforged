package net.jr.mixin.compat.jade;

import com.llamalad7.mixinextras.sugar.Local;
import net.jr.client.compat.jade.JadeRuntimeState;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FogRenderer.class, priority = 900)
abstract class JadeFogRendererMixin {
    @Inject(method = "setupFog", at = @At("TAIL"))
    private void javaReforged$captureCurrentFog(
        Camera camera,
        int renderDistanceInChunks,
        DeltaTracker deltaTracker,
        float darkenWorldAmount,
        ClientLevel level,
        CallbackInfoReturnable<Vector4f> cir,
        @Local(name = "fog") FogData fog
    ) {
        JadeRuntimeState.setFog(fog.renderDistanceStart, fog.renderDistanceEnd);
    }
}
