package net.jr.mixin.compat.jade;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import snownee.jade.impl.ObjectDataCenter;

@Mixin(value = ObjectDataCenter.class, remap = false)
public interface ObjectDataCenterAccessor {
    @Accessor("lastObject")
    static Object javaReforged$getLastObject() {
        throw new AssertionError();
    }

    @Accessor("lastObject")
    static void javaReforged$setLastObject(Object value) {
        throw new AssertionError();
    }
}
