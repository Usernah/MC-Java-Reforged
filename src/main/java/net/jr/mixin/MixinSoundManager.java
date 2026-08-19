package net.jr.mixin;

import net.jr.client.sound.bridge.SoundEngineBridge;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SoundManager.class)
public class MixinSoundManager implements SoundEngineBridge {
    @Shadow @Final private SoundEngine soundEngine;

    @Override
    public void moods$stopMusic() {
        ((SoundEngineBridge) this.soundEngine).moods$stopMusic();
    }

    @Override
    public void moods$stopSounds() {
        ((SoundEngineBridge) this.soundEngine).moods$stopSounds();
    }

    @Override
    public void moods$fadeSounds(float ticks) {
        ((SoundEngineBridge) this.soundEngine).moods$fadeSounds(ticks);
    }

    @Override
    public void moods$clearQueued() {
        ((SoundEngineBridge) this.soundEngine).moods$clearQueued();
    }

    @Override
    public boolean moods$hasFadingMusic() {
        return ((SoundEngineBridge) this.soundEngine).moods$hasFadingMusic();
    }
}
