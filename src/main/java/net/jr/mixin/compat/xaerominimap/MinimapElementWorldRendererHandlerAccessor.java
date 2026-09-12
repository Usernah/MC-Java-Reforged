package net.jr.mixin.compat.xaerominimap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.hud.minimap.element.render.MinimapElementRenderer;
import xaero.hud.minimap.element.render.world.MinimapElementWorldRendererHandler;

@Mixin(value = MinimapElementWorldRendererHandler.class, remap = false)
public interface MinimapElementWorldRendererHandlerAccessor {
    @Accessor("previousClosestHoveredElement")
    Object javaReforged$getPreviousClosestHoveredElement();

    @Accessor("previousClosestHoveredElement")
    void javaReforged$setPreviousClosestHoveredElement(Object element);

    @Accessor("previousClosestHoveredElementRenderer")
    MinimapElementRenderer<?, ?> javaReforged$getPreviousClosestHoveredElementRenderer();

    @Accessor("previousClosestHoveredElementRenderer")
    void javaReforged$setPreviousClosestHoveredElementRenderer(MinimapElementRenderer<?, ?> renderer);

    @Accessor("previousClosestHoveredElementPresent")
    boolean javaReforged$getPreviousClosestHoveredElementPresent();

    @Accessor("previousClosestHoveredElementPresent")
    void javaReforged$setPreviousClosestHoveredElementPresent(boolean present);
}
