package net.jr;

import net.jr.api.client.split.SplitOrientation;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.EnumValue<SplitOrientation> SPLIT_ORIENTATION = BUILDER
        .comment("Orientation used by the two-player split-screen layout")
        .defineEnum("splitOrientation", SplitOrientation.VERTICAL);
    public static final ModConfigSpec.BooleanValue EXPERIMENTAL_RENDER_OPTIMIZATIONS = BUILDER
        .comment("Enable Java Reforged native terrain multi-draw indirect")
        .define("experimentalRenderOptimizations", false);
    public static final ModConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {
    }

    public static SplitOrientation splitOrientation() {
        return SPLIT_ORIENTATION.get();
    }

    public static void setSplitOrientation(SplitOrientation orientation) {
        SPLIT_ORIENTATION.set(orientation);
        SPLIT_ORIENTATION.save();
    }

    public static boolean experimentalRenderOptimizations() {
        return EXPERIMENTAL_RENDER_OPTIMIZATIONS.getAsBoolean();
    }

    public static void setExperimentalRenderOptimizations(boolean enabled) {
        EXPERIMENTAL_RENDER_OPTIMIZATIONS.set(enabled);
        EXPERIMENTAL_RENDER_OPTIMIZATIONS.save();
    }
}
