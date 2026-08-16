package net.jr.client.runtime.bridge;

/** Public color control exposed by the split-aware chat component mixin. */
public interface ChatComponentRuntimeBridge {
    int DEFAULT_BACKGROUND_COLOR = 0x7e7e7e;

    int javaReforged$getBackgroundColor();

    void javaReforged$setBackgroundColor(int rgb);
}
