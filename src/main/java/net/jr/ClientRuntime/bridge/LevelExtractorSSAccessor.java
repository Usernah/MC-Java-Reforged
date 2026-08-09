package net.jr.ClientRuntime.bridge;

import javax.annotation.Nullable;
import net.jr.ClientRuntime.state.LevelExtractionState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.level.LevelRenderState;

public interface LevelExtractorSSAccessor {
    @Nullable ClientLevel splitTest$getLevel();

    void splitTest$install(@Nullable ClientLevel level, LevelRenderState renderState, LevelExtractionState extractionState);

    void splitTest$capture(LevelExtractionState extractionState);
}
