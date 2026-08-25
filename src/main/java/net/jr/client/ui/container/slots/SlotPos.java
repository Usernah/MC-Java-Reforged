package net.jr.client.ui.container.slots;

public record SlotPos(int x, int y, int width, int height, int itemSize) {
    public SlotPos(int x, int y) {
        this(x, y, 16, 16, 16);
    }

    public SlotPos(int x, int y, int width, int height) {
        this(x, y, width, height, Math.max(1, Math.min(16, Math.min(width, height) - 2)));
    }
}

