package net.jr.mixin;

import net.jr.client.sound.bridge.SoundEngineBridge;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SoundManager.class)
public abstract class SoundManagerMixin implements SoundEngineBridge {
    @Shadow @Final private SoundEngine soundEngine;
    private SoundEngineBridge javaReforged$bridge() { return (SoundEngineBridge) this.soundEngine; }
    @Override public void javaReforged$stopMusic() { this.javaReforged$bridge().javaReforged$stopMusic(); }
    @Override public void javaReforged$stopSounds() { this.javaReforged$bridge().javaReforged$stopSounds(); }
    @Override public void javaReforged$fadeSounds(float ticks) { this.javaReforged$bridge().javaReforged$fadeSounds(ticks); }
    @Override public void javaReforged$clearQueued() { this.javaReforged$bridge().javaReforged$clearQueued(); }
    @Override public boolean javaReforged$hasFadingMusic() { return this.javaReforged$bridge().javaReforged$hasFadingMusic(); }
}
