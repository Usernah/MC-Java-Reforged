package net.jr.mixin.uifocus;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {
    @Accessor("xpos")
    double javareforged$getXpos();

    @Accessor("xpos")
    void javareforged$setXpos(double xpos);

    @Accessor("ypos")
    double javareforged$getYpos();

    @Accessor("ypos")
    void javareforged$setYpos(double ypos);
}

