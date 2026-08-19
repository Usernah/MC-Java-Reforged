package net.jr.client.sound.bridge;

import net.jr.client.sound.config.SoundTransitionConfig;

public interface SoundEngineBridge {
    void moods$stopMusic();

    void moods$stopSounds();

    void moods$fadeSounds(float ticks);

    void moods$clearQueued();

    boolean moods$hasFadingMusic();

    default boolean moods$onInterceptStop() {
        if (!SoundTransitionConfig.seamlessTransitions && SoundTransitionConfig.seamlessSoundTransitions == 0) {
            return true;
        }

        this.moods$clearQueued();
        if (SoundTransitionConfig.seamlessSoundTransitions == 0) {
            this.moods$stopSounds();
        } else {
            this.moods$fadeSounds((float) SoundTransitionConfig.seamlessSoundTransitions);
        }

        if (!SoundTransitionConfig.seamlessTransitions) {
            this.moods$stopMusic();
        }

        return false;
    }
}
