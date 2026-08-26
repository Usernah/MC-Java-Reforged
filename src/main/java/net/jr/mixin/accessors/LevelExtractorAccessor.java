package net.jr.mixin.accessors;

import javax.annotation.Nullable;
import net.minecraft.client.SectionUpdateTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelExtractor.class)
public interface LevelExtractorAccessor {
    @Accessor("level")
    @Nullable ClientLevel javaReforged$getLevel();

    @Accessor("level")
    void javaReforged$setLevel(@Nullable ClientLevel level);

    @Accessor("sectionUpdateTracker")
    @Nullable SectionUpdateTracker javaReforged$getSectionUpdateTracker();

    @Accessor("sectionUpdateTracker")
    void javaReforged$setSectionUpdateTracker(@Nullable SectionUpdateTracker tracker);

    @Accessor("levelRenderState")
    LevelRenderState javaReforged$getLevelRenderState();

    @Mutable
    @Accessor("levelRenderState")
    void javaReforged$setLevelRenderState(LevelRenderState state);

    @Accessor("prevCamRotX")
    double javaReforged$getPrevCamRotX();

    @Accessor("prevCamRotX")
    void javaReforged$setPrevCamRotX(double value);

    @Accessor("prevCamRotY")
    double javaReforged$getPrevCamRotY();

    @Accessor("prevCamRotY")
    void javaReforged$setPrevCamRotY(double value);

    @Accessor("lastViewDistance")
    int javaReforged$getLastViewDistance();

    @Accessor("lastViewDistance")
    void javaReforged$setLastViewDistance(int value);

    @Accessor("shouldInvalidateCompiledGeometry")
    boolean javaReforged$getShouldInvalidateCompiledGeometry();

    @Accessor("shouldInvalidateCompiledGeometry")
    void javaReforged$setShouldInvalidateCompiledGeometry(boolean value);

    @Accessor("shouldResetLevelRenderData")
    boolean javaReforged$getShouldResetLevelRenderData();

    @Accessor("shouldResetLevelRenderData")
    void javaReforged$setShouldResetLevelRenderData(boolean value);

    @Accessor("shouldResetChunkLayerSampler")
    boolean javaReforged$getShouldResetChunkLayerSampler();

    @Accessor("shouldResetChunkLayerSampler")
    void javaReforged$setShouldResetChunkLayerSampler(boolean value);

    @Accessor("shouldResetSkyRenderer")
    boolean javaReforged$getShouldResetSkyRenderer();

    @Accessor("shouldResetSkyRenderer")
    void javaReforged$setShouldResetSkyRenderer(boolean value);
}
