package net.jr.client.runtime.state;

import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class MinimapState {
    private double x;
    private double y;
    private double z;

    @Nullable
    private ResourceKey<Level> dimension;

    private float rotation;
    private float zoom = 1.0F;
    private boolean visible = true;

    public void updateView(
            double x,
            double y,
            double z,
            ResourceKey<Level> dimension,
            float rotation
    ) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
        this.rotation = rotation;
    }

    public double x() {
        return this.x;
    }

    public double y() {
        return this.y;
    }

    public double z() {
        return this.z;
    }

    @Nullable
    public ResourceKey<Level> dimension() {
        return this.dimension;
    }

    public float rotation() {
        return this.rotation;
    }

    public float zoom() {
        return this.zoom;
    }

    public void setZoom(float zoom) {
        if (zoom <= 0.0F || Float.isNaN(zoom) || Float.isInfinite(zoom)) {
            throw new IllegalArgumentException("zoom must be a positive finite number");
        }

        this.zoom = zoom;
    }

    public boolean visible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void clearWorldBinding() {
        this.x = 0.0;
        this.y = 0.0;
        this.z = 0.0;
        this.dimension = null;
        this.rotation = 0.0F;
    }
}