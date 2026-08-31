package net.jr.client.ui.presentation;

import java.util.Arrays;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;

public final class RenderOptimizationOptionsHandler {
    private RenderOptimizationOptionsHandler() {
    }

    public static OptionInstance<?>[] extendQuality(Options options, OptionInstance<?>[] original) {
        OptionInstance<?> anchor = options.prioritizeChunkUpdates();
        int anchorIndex = -1;
        for (int index = 0; index < original.length; index++) {
            if (original[index] == anchor) {
                anchorIndex = index;
                break;
            }
        }

        if (anchorIndex < 0) {
            OptionInstance<?>[] extended = Arrays.copyOf(original, original.length + 1);
            extended[original.length] = ExperimentalRenderOption.INSTANCE;
            return extended;
        }

        OptionInstance<?>[] extended = new OptionInstance<?>[original.length + 1];
        int insertionIndex = anchorIndex + 1;
        System.arraycopy(original, 0, extended, 0, insertionIndex);
        extended[insertionIndex] = ExperimentalRenderOption.INSTANCE;
        System.arraycopy(original, insertionIndex, extended, insertionIndex + 1, original.length - insertionIndex);
        return extended;
    }
}
