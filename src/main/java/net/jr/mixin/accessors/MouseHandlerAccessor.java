package net.jr.mixin.accessors;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {
    @Accessor("xpos")
    double javaReforged$getXpos();

    @Accessor("xpos")
    void javaReforged$setXpos(double xpos);

    @Accessor("ypos")
    double javaReforged$getYpos();

    @Accessor("ypos")
    void javaReforged$setYpos(double ypos);
}

