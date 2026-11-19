package net.jr.client.runtime.input.binding;

public final class KeyMappingState {
    private boolean down;
    private boolean physicallyDown;
    private int clickCount;

    public boolean isDown() {
        return this.down;
    }

    public void setDown(boolean down) {
        this.down = down;
    }

    public boolean isPhysicallyDown() {
        return this.physicallyDown;
    }

    public void setPhysicallyDown(boolean physicallyDown) {
        this.physicallyDown = physicallyDown;
    }

    public int clickCount() {
        return this.clickCount;
    }

    public void setClickCount(int clickCount) {
        this.clickCount = Math.max(0, clickCount);
    }

    public void incrementClickCount() {
        this.clickCount++;
    }

    public boolean consumeClick() {
        if (this.clickCount <= 0) {
            return false;
        }
        this.clickCount--;
        return true;
    }

    public void release() {
        this.down = false;
        this.physicallyDown = false;
        this.clickCount = 0;
    }
}
