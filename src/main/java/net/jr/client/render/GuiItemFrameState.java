package net.jr.client.render;

public final class GuiItemFrameState {
    private int itemColor = -1;
    private int largestAtlasItemSize = 16;

    public int itemColor() {
        return this.itemColor;
    }

    public void setItemColor(int itemColor) {
        this.itemColor = itemColor;
    }

    public int largestAtlasItemSize() {
        return this.largestAtlasItemSize;
    }

    public void setLargestAtlasItemSize(int size) {
        this.largestAtlasItemSize = size;
    }
}
