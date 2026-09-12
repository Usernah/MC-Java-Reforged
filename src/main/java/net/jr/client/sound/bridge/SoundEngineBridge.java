package net.jr.client.sound.bridge;

import net.jr.client.sound.config.SoundTransitionConfig;

public interface SoundEngineBridge {
    void javaReforged$stopMusic();

    void javaReforged$stopSounds();

    void javaReforged$fadeSounds(float ticks);

    void javaReforged$clearQueued();

    boolean javaReforged$hasFadingMusic();

    boolean javaReforged$hasManagedMusic();

    default boolean javaReforged$onInterceptStop() {
        if (!SoundTransitionConfig.seamlessTransitions && SoundTransitionConfig.seamlessSoundTransitions == 0) {
            return true;
        }

        this.javaReforged$clearQueued();
        if (SoundTransitionConfig.seamlessSoundTransitions == 0) {
            this.javaReforged$stopSounds();
        } else {
            this.javaReforged$fadeSounds((float) SoundTransitionConfig.seamlessSoundTransitions);
        }

        if (!SoundTransitionConfig.seamlessTransitions) {
            this.javaReforged$stopMusic();
        }

        return false;
    }
}
