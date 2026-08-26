package net.jr.mixin.accessors;

import javax.annotation.Nullable;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Gui.class)
public interface GuiAccessor {
    @Accessor("screen")
    @Nullable
    Screen javaReforged$getRawScreen();

    @Accessor("screen")
    void javaReforged$setRawScreen(@Nullable Screen screen);
}
