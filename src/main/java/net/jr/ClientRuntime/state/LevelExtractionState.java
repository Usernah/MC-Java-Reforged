package net.jr.ClientRuntime.state;

import javax.annotation.Nullable;
import net.minecraft.client.SectionUpdateTracker;

/** Mutable data owned by one local player and consumed by the single LevelExtractor. */
public final class LevelExtractionState {
    @Nullable
    private SectionUpdateTracker sectionUpdateTracker;
    private double prevCamRotX = Double.MIN_VALUE;
    private double prevCamRotY = Double.MIN_VALUE;
    private int lastViewDistance = -1;
    private boolean shouldInvalidateCompiledGeometry = true;
    private boolean shouldResetLevelRenderData;
    private boolean shouldResetChunkLayerSampler;
    private boolean shouldResetSkyRenderer;

    @Nullable
    public SectionUpdateTracker sectionUpdateTracker() {
        return this.sectionUpdateTracker;
    }

    public void setSectionUpdateTracker(@Nullable SectionUpdateTracker sectionUpdateTracker) {
        this.sectionUpdateTracker = sectionUpdateTracker;
    }

    public double prevCamRotX() {
        return this.prevCamRotX;
    }

    public void setPrevCamRotX(double prevCamRotX) {
        this.prevCamRotX = prevCamRotX;
    }

    public double prevCamRotY() {
        return this.prevCamRotY;
    }

    public void setPrevCamRotY(double prevCamRotY) {
        this.prevCamRotY = prevCamRotY;
    }

    public int lastViewDistance() {
        return this.lastViewDistance;
    }

    public void setLastViewDistance(int lastViewDistance) {
        this.lastViewDistance = lastViewDistance;
    }

    public boolean shouldInvalidateCompiledGeometry() {
        return this.shouldInvalidateCompiledGeometry;
    }

    public void setShouldInvalidateCompiledGeometry(boolean value) {
        this.shouldInvalidateCompiledGeometry = value;
    }

    public boolean shouldResetLevelRenderData() {
        return this.shouldResetLevelRenderData;
    }

    public void setShouldResetLevelRenderData(boolean value) {
        this.shouldResetLevelRenderData = value;
    }

    public boolean shouldResetChunkLayerSampler() {
        return this.shouldResetChunkLayerSampler;
    }

    public void setShouldResetChunkLayerSampler(boolean value) {
        this.shouldResetChunkLayerSampler = value;
    }

    public boolean shouldResetSkyRenderer() {
        return this.shouldResetSkyRenderer;
    }

    public void setShouldResetSkyRenderer(boolean value) {
        this.shouldResetSkyRenderer = value;
    }

    public void clear() {
        this.sectionUpdateTracker = null;
        this.prevCamRotX = Double.MIN_VALUE;
        this.prevCamRotY = Double.MIN_VALUE;
        this.lastViewDistance = -1;
        this.shouldInvalidateCompiledGeometry = true;
        this.shouldResetLevelRenderData = false;
        this.shouldResetChunkLayerSampler = false;
        this.shouldResetSkyRenderer = false;
    }
}
