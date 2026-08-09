package net.jr.mixin.controls;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyMappingClickAccessor {
    @Accessor("clickCount")
    int javareforged$getClickCount();

    @Accessor("clickCount")
    void javareforged$setClickCount(int value);
}

