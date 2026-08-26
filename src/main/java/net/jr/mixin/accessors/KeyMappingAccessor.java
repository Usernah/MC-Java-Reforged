package net.jr.mixin.accessors;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("clickCount")
    int javaReforged$getClickCount();

    @Accessor("clickCount")
    void javaReforged$setClickCount(int value);
}

