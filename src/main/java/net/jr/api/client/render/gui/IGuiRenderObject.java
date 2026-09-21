package net.jr.api.client.render.gui;

@FunctionalInterface
public interface IGuiRenderObject<C extends GuiRenderContext> {
    void render(C context);
}
