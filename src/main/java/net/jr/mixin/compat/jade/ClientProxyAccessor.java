package net.jr.mixin.compat.jade;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import snownee.jade.util.ClientProxy;

@Mixin(value = ClientProxy.class, remap = false)
public interface ClientProxyAccessor {
    @Accessor("bossbarShown")
    static boolean javaReforged$getBossbarShown() {
        throw new AssertionError();
    }

    @Accessor("bossbarShown")
    static void javaReforged$setBossbarShown(boolean value) {
        throw new AssertionError();
    }

    @Accessor("bossbarHeight")
    static int javaReforged$getBossbarHeight() {
        throw new AssertionError();
    }

    @Accessor("bossbarHeight")
    static void javaReforged$setBossbarHeight(int value) {
        throw new AssertionError();
    }
}
