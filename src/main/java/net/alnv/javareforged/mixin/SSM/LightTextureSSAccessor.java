package net.alnv.javareforged.mixin.SSM;

import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LightTexture.class)
public interface LightTextureSSAccessor {
    @Accessor("updateLightTexture")
    void splitTest$setUpdateLightTexture(boolean value);
}
