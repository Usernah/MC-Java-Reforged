package net.jr.api.client.render.world;


public interface RenderViewport {
    int id();

    int x();

    int y();

    int width();

    int height();

    int glX();

    int glY();

    int guiX();

    int guiY();

    int guiWidth();

    int guiHeight();

    float aspectRatio();
}
