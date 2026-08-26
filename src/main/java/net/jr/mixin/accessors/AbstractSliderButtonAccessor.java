package net.jr.mixin.accessors;

import net.minecraft.client.gui.components.AbstractSliderButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractSliderButton.class)
public interface AbstractSliderButtonAccessor {
    @Accessor("value")
    double javaReforged$getValue();

    @Accessor("canChangeValue")
    boolean javaReforged$getCanChangeValue();

    @Accessor("canChangeValue")
    void javaReforged$setCanChangeValue(boolean canChangeValue);
}
