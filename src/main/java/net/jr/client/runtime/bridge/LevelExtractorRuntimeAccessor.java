package net.jr.client.runtime.bridge;

import javax.annotation.Nullable;
import net.jr.client.runtime.state.LevelExtractionState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.level.LevelRenderState;

public interface LevelExtractorRuntimeAccessor {
    @Nullable ClientLevel splitTest$getLevel();

    void splitTest$install(@Nullable ClientLevel level, LevelRenderState renderState, LevelExtractionState extractionState);

    void splitTest$capture(LevelExtractionState extractionState);
}
