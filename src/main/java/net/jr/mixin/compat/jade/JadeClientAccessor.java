package net.jr.mixin.compat.jade;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import snownee.jade.JadeClient;

@Mixin(value = JadeClient.class, remap = false)
public interface JadeClientAccessor {
    @Accessor("savedProgress")
    static float javaReforged$getSavedProgress() {
        throw new AssertionError();
    }

    @Accessor("savedProgress")
    static void javaReforged$setSavedProgress(float value) {
        throw new AssertionError();
    }

    @Accessor("progressAlpha")
    static float javaReforged$getProgressAlpha() {
        throw new AssertionError();
    }

    @Accessor("progressAlpha")
    static void javaReforged$setProgressAlpha(float value) {
        throw new AssertionError();
    }

    @Accessor("canHarvest")
    static boolean javaReforged$getCanHarvest() {
        throw new AssertionError();
    }

    @Accessor("canHarvest")
    static void javaReforged$setCanHarvest(boolean value) {
        throw new AssertionError();
    }

    @Accessor("inPowderSnowTime")
    static long javaReforged$getInPowderSnowTime() {
        throw new AssertionError();
    }

    @Accessor("inPowderSnowTime")
    static void javaReforged$setInPowderSnowTime(long value) {
        throw new AssertionError();
    }
}
