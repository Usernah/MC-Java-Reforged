package net.alnv.javareforged.mixin.SSM;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ParticleEngine.class)
public interface ParticleEngineSSAccessor {
    @Accessor("level")
    ClientLevel splitTest$getLevel();

    @Accessor("level")
    void splitTest$setLevel(ClientLevel level);
}
