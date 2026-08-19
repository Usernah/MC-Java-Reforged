package net.jr.client.render;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.Nullable;

public interface GuiGraphicsExtractorBridge {
    @Nullable
    ScreenRectangle javaReforged$currentScissor();
}
