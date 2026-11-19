package net.jr.mixin.accessors;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenAccesor {
    @Invoker("addRenderableWidget")
    <T extends GuiEventListener & Renderable & NarratableEntry>
    T javaReforged$addRenderableWidget(T widget);

    @Invoker("rebuildWidgets")
    void javaReforged$rebuildWidgets();
}
