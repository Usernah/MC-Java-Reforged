package net.jr.client.runtime.render.state;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;


public final class ViewportBlurState {
    private final Map<Integer, Region> regions = new LinkedHashMap<>();
    private boolean global;

    public void request(Region region) {
        this.regions.put(region.id(), region);
    }

    public void requestGlobal() {
        this.global = true;
    }

    public boolean isLocal() {
        return !this.global && !this.regions.isEmpty();
    }

    public Collection<Region> regions() {
        return this.regions.values();
    }

    public boolean contains(int id) {
        return this.isLocal() && this.regions.containsKey(id);
    }

    public void reset() {
        this.regions.clear();
        this.global = false;
    }


    public record Region(int id, int x, int y, int width, int height) {
        public Region clipTo(int targetWidth, int targetHeight) {
            int left = Math.max(0, this.x);
            int bottom = Math.max(0, this.y);
            int right = (int)Math.min((long)targetWidth, (long)this.x + this.width);
            int top = (int)Math.min((long)targetHeight, (long)this.y + this.height);
            return new Region(this.id, left, bottom, Math.max(0, right - left), Math.max(0, top - bottom));
        }

        public boolean isEmpty() {
            return this.width <= 0 || this.height <= 0;
        }
    }
}
