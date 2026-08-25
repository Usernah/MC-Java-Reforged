package net.jr.client.runtime.viewport;

import javax.annotation.Nullable;

public interface PreciseScissorAccess {
    @Nullable
    PreciseGuiScissor javaReforged$preciseScissor();

    void javaReforged$setPreciseScissor(
            @Nullable PreciseGuiScissor scissor
    );
}