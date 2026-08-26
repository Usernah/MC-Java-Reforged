package net.jr.mixin.accessors;

import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiRenderer.class)
public interface GuiRendererAccessor {
    @Accessor("renderState")
    GuiRenderState javaReforged$getRenderState();

    @Invoker("invalidateItemAtlas")
    void javaReforged$invalidateItemAtlas();
}
