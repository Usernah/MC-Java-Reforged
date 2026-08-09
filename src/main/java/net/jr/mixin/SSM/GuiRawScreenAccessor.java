package net.jr.mixin.SSM;

import javax.annotation.Nullable;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Gui.class)
public interface GuiRawScreenAccessor {
    @Accessor("screen")
    @Nullable
    Screen splitTest$getRawScreen();

    @Accessor("screen")
    void splitTest$setRawScreen(@Nullable Screen screen);
}
